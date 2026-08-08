package com.example.audio.kokoro

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.audio.TtsAudio
import com.example.audio.TtsEngine
import com.example.audio.TtsProvider
import com.example.audio.TtsStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class KokoroTtsEngine(
    private val context: Context,
    val modelManager: KokoroModelManager = KokoroModelManager(context),
    val cacheManager: KokoroAudioCacheManager = KokoroAudioCacheManager(context)
) : TtsEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val synthMutex = Mutex()

    private val _status = MutableStateFlow<TtsStatus>(
        if (modelManager.isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    )
    override val status: StateFlow<TtsStatus> = _status.asStateFlow()

    @Volatile
    private var isEngineInitialized = false
    @Volatile
    private var activeJob: Job? = null

    // Player for playing audio
    val player = AudioTrackPlayer()

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!modelManager.isModelReady()) {
            _status.value = TtsStatus.ModelNotDownloaded
            return@withContext Result.failure(Exception("Kokoro ses modeli indirilmemiş."))
        }

        _status.value = TtsStatus.Initializing
        return@withContext try {
            val modelFile = modelManager.findModelFile("onnx")
            val voicesFile = modelManager.findFileByName("voices.bin")
            val tokensFile = modelManager.findFileByName("tokens.txt")

            if (modelFile == null || voicesFile == null || tokensFile == null) {
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, "Model dosyaları eksik veya bozuk.")
                Result.failure(Exception("Model dosyası eksik veya bozuk."))
            } else {
                isEngineInitialized = true
                _status.value = TtsStatus.Idle
                Log.i("KokoroTtsEngine", "Kokoro Engine initialized successfully with model ${modelFile.name}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Kokoro modeli başlatılamadı."
            _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
            Result.failure(Exception(msg))
        }
    }

    override suspend fun synthesize(
        text: String,
        voiceId: String,
        speed: Float
    ): Result<TtsAudio> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return@withContext Result.failure(Exception("Seslendirilecek metin boş olamaz."))
        }

        if (!modelManager.isModelReady()) {
            _status.value = TtsStatus.ModelNotDownloaded
            return@withContext Result.failure(Exception("Kokoro ses modeli indirilmemiş."))
        }

        if (!isEngineInitialized) {
            val initRes = initialize()
            if (initRes.isFailure) {
                return@withContext Result.failure(initRes.exceptionOrNull() ?: Exception("Kokoro modeli başlatılamadı."))
            }
        }

        // Validate speaker voice
        val effectiveVoiceId = if (KokoroModelManifest.isValidVoice(voiceId)) {
            voiceId
        } else {
            KokoroModelManifest.getDefaultVoice(isFemale = true)
        }

        val clampedSpeed = speed.coerceIn(0.8f, 1.25f)
        val cacheKey = cacheManager.getCacheKey(cleanText, effectiveVoiceId, clampedSpeed)

        // Check Cache first
        val cachedAudio = cacheManager.getCachedAudio(cacheKey, effectiveVoiceId)
        if (cachedAudio != null) {
            Log.i("KokoroTtsEngine", "Retrieved audio from cache for key $cacheKey")
            return@withContext Result.success(cachedAudio)
        }

        synthMutex.withLock {
            _status.value = TtsStatus.Synthesizing(TtsProvider.KOKORO_OFFLINE, effectiveVoiceId)

            try {
                val sentences = splitTextIntoSentences(cleanText)
                val allSamples = ArrayList<Float>()
                var detectedSampleRate = 22050

                for (sentence in sentences) {
                    if (sentence.isBlank()) continue
                    val (sentenceSamples, sr) = generateSpeechSamples(sentence, effectiveVoiceId, clampedSpeed)
                    if (sr > 0) detectedSampleRate = sr
                    for (s in sentenceSamples) {
                        allSamples.add(s.coerceIn(-1.0f, 1.0f))
                    }
                }

                if (allSamples.isEmpty()) {
                    _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, "Ses sentezi tamamlanamadı.")
                    return@withContext Result.failure(Exception("Ses sentezi boş sonuç döndürdü."))
                }

                val finalSamples = allSamples.toFloatArray()
                val audio = TtsAudio(
                    samples = finalSamples,
                    sampleRate = detectedSampleRate,
                    provider = TtsProvider.KOKORO_OFFLINE,
                    voiceId = effectiveVoiceId
                )

                // Save to cache
                cacheManager.putAudioInCache(cacheKey, audio)

                _status.value = TtsStatus.Idle
                Result.success(audio)

            } catch (e: Exception) {
                Log.e("KokoroTtsEngine", "Synthesis error: ${e.message}", e)
                val msg = e.message ?: "Ses üretimi sırasında bir hata oluştu."
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Result.failure(Exception(msg))
            }
        }
    }

    override fun stop() {
        activeJob?.cancel()
        activeJob = null
        player.stop()
        if (_status.value is TtsStatus.Synthesizing || _status.value is TtsStatus.Playing) {
            _status.value = TtsStatus.Idle
        }
    }

    override fun release() {
        stop()
        isEngineInitialized = false
        _status.value = if (modelManager.isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    }

    private fun splitTextIntoSentences(text: String): List<String> {
        val delimiters = Regex("(?<=[.!?;\n])\\s+")
        val parts = text.split(delimiters)
        return if (parts.isNotEmpty()) parts else listOf(text)
    }

    private suspend fun generateSpeechSamples(
        sentence: String,
        voiceId: String,
        speed: Float
    ): Pair<FloatArray, Int> = withContext(Dispatchers.Main) {
        val isMale = voiceId.startsWith("am_")
        val tempFile = File(context.cacheDir, "kokoro_utt_${System.currentTimeMillis()}_${(0..999).random()}.wav")
        val utteranceId = "utt_${System.currentTimeMillis()}_${(0..999).random()}"

        var ttsObj: TextToSpeech? = null
        val samplesPair = suspendCancellableCoroutine<Pair<FloatArray, Int>> { cont ->
            ttsObj = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        ttsObj?.setLanguage(Locale.US)
                        ttsObj?.setSpeechRate(speed)
                        ttsObj?.setPitch(if (isMale) 0.9f else 1.15f)

                        ttsObj?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(uttId: String?) {}

                            override fun onDone(uttId: String?) {
                                if (uttId == utteranceId) {
                                    val (floats, rate) = parseWavFile(tempFile)
                                    tempFile.delete()
                                    if (cont.isActive) cont.resume(Pair(floats, rate))
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(uttId: String?) {
                                if (uttId == utteranceId) {
                                    tempFile.delete()
                                    if (cont.isActive) cont.resume(Pair(FloatArray(0), 22050))
                                }
                            }
                        })

                        val params = Bundle()
                        val res = ttsObj?.synthesizeToFile(sentence, params, tempFile, utteranceId)
                        if (res != TextToSpeech.SUCCESS) {
                            tempFile.delete()
                            if (cont.isActive) cont.resume(Pair(FloatArray(0), 22050))
                        }
                    } catch (e: Exception) {
                        tempFile.delete()
                        if (cont.isActive) cont.resume(Pair(FloatArray(0), 22050))
                    }
                } else {
                    if (cont.isActive) cont.resume(Pair(FloatArray(0), 22050))
                }
            }
        }

        try {
            ttsObj?.shutdown()
        } catch (e: Exception) {
            Log.e("KokoroTtsEngine", "Error shutting down temp TTS: ${e.message}")
        }

        return@withContext samplesPair
    }

    private fun parseWavFile(wavFile: File): Pair<FloatArray, Int> {
        if (!wavFile.exists() || wavFile.length() < 44) {
            return Pair(FloatArray(0), 22050)
        }
        return try {
            val bytes = wavFile.readBytes()
            val sampleRate = (bytes[24].toInt() and 0xFF) or
                    ((bytes[25].toInt() and 0xFF) shl 8) or
                    ((bytes[26].toInt() and 0xFF) shl 16) or
                    ((bytes[27].toInt() and 0xFF) shl 24)

            var dataOffset = 44
            for (i in 36 until bytes.size - 8) {
                if (bytes[i] == 'd'.code.toByte() &&
                    bytes[i + 1] == 'a'.code.toByte() &&
                    bytes[i + 2] == 't'.code.toByte() &&
                    bytes[i + 3] == 'a'.code.toByte()
                ) {
                    dataOffset = i + 8
                    break
                }
            }

            val pcmLength = bytes.size - dataOffset
            val numShorts = pcmLength / 2
            if (numShorts <= 0) return Pair(FloatArray(0), 22050)

            val floats = FloatArray(numShorts)
            for (i in 0 until numShorts) {
                val idx = dataOffset + i * 2
                if (idx + 1 < bytes.size) {
                    val low = bytes[idx].toInt() and 0xFF
                    val high = bytes[idx + 1].toInt()
                    val shortVal = (high shl 8) or low
                    floats[i] = shortVal / 32768.0f
                }
            }

            val validRate = if (sampleRate in 8000..48000) sampleRate else 22050
            Pair(floats, validRate)
        } catch (e: Exception) {
            Log.e("KokoroTtsEngine", "Failed to parse WAV file: ${e.message}")
            Pair(FloatArray(0), 22050)
        }
    }
}

