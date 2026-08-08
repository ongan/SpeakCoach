package com.example.audio.kokoro

import android.content.Context
import android.util.Log
import com.example.audio.TtsAudio
import com.example.audio.TtsEngine
import com.example.audio.TtsProvider
import com.example.audio.TtsStatus
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class KokoroTtsEngine(
    private val context: Context,
    val modelManager: KokoroModelManager = KokoroModelManager(context),
    val cacheManager: KokoroAudioCacheManager = KokoroAudioCacheManager(context)
) : TtsEngine {

    private val initMutex = Mutex()
    private val synthMutex = Mutex()
    private val nativeLock = Any()

    private val _status = MutableStateFlow<TtsStatus>(
        if (modelManager.isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    )
    override val status: StateFlow<TtsStatus> = _status.asStateFlow()

    @Volatile
    private var isEngineInitialized = false
    @Volatile
    private var stopRequested = false
    @Volatile
    private var offlineTts: OfflineTts? = null

    // Player for playing audio
    val player = AudioTrackPlayer()

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (isEngineInitialized && offlineTts != null) {
                return@withLock Result.success(Unit)
            }

            val validation = modelManager.validateModelDirectory()
            if (!validation.isReady) {
                _status.value = TtsStatus.ModelNotDownloaded
                return@withLock Result.failure(Exception("Kokoro ses modeli indirilmemiş."))
            }

            _status.value = TtsStatus.Initializing
            try {
                val modelFile = validation.modelFile
                val voicesFile = validation.voicesFile
                val tokensFile = validation.tokensFile
                val dataDir = validation.espeakDataDir
                val lexiconUs = modelManager.findFileByName("lexicon-us-en.txt")
                val lexiconZh = modelManager.findFileByName("lexicon-zh.txt")

                if (modelFile == null || voicesFile == null || tokensFile == null ||
                    dataDir == null || lexiconUs == null
                ) {
                    val msg = "Kokoro model dosyaları eksik veya bozuk."
                    _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                    return@withLock Result.failure(Exception(msg))
                }

                val lexicons = listOfNotNull(lexiconUs, lexiconZh)
                    .joinToString(",") { it.absolutePath }
                val ruleFsts = listOf("phone-zh.fst", "date-zh.fst", "number-zh.fst")
                    .mapNotNull { modelManager.findFileByName(it) }
                    .joinToString(",") { it.absolutePath }
                val threadCount = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 2)

                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        kokoro = OfflineTtsKokoroModelConfig(
                            model = modelFile.absolutePath,
                            voices = voicesFile.absolutePath,
                            tokens = tokensFile.absolutePath,
                            dataDir = dataDir.absolutePath,
                            lexicon = lexicons,
                            lengthScale = 1.0f
                        ),
                        numThreads = threadCount,
                        debug = false,
                        provider = "cpu"
                    ),
                    ruleFsts = ruleFsts,
                    maxNumSentences = 1,
                    silenceScale = 0.2f
                )

                val createdEngine = OfflineTts(config = config)
                val sampleRate = createdEngine.sampleRate()
                val speakerCount = createdEngine.numSpeakers()
                val highestConfiguredSpeaker = KokoroModelManifest.ALL_VOICES.maxOf { it.speakerId }

                if (sampleRate <= 0 || speakerCount <= highestConfiguredSpeaker) {
                    createdEngine.release()
                    val msg = "Kokoro motoru modeli açamadı (örnekleme=$sampleRate, ses sayısı=$speakerCount)."
                    _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                    return@withLock Result.failure(Exception(msg))
                }

                synchronized(nativeLock) {
                    offlineTts?.release()
                    offlineTts = createdEngine
                    isEngineInitialized = true
                    stopRequested = false
                }

                _status.value = TtsStatus.Idle
                Log.i(
                    "KokoroTtsEngine",
                    "Kokoro initialized: ${modelFile.name}, sampleRate=$sampleRate, speakers=$speakerCount, threads=$threadCount"
                )
                Result.success(Unit)
            } catch (e: OutOfMemoryError) {
                releaseFailedEngine()
                val msg = "Kokoro modeli için cihaz belleği yetersiz kaldı. Modeli silip tekrar indirin veya yedek TTS motoru kullanın."
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Log.e("KokoroTtsEngine", msg, e)
                Result.failure(IllegalStateException(msg, e))
            } catch (e: UnsatisfiedLinkError) {
                releaseFailedEngine()
                isEngineInitialized = false
                val msg = "Kokoro yerel kütüphanesi yüklenemedi: ${e.message ?: "JNI bulunamadı"}"
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Log.e("KokoroTtsEngine", msg, e)
                Result.failure(IllegalStateException(msg, e))
            } catch (e: Exception) {
                releaseFailedEngine()
                isEngineInitialized = false
                val msg = e.message ?: "Kokoro modeli başlatılamadı."
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Log.e("KokoroTtsEngine", msg, e)
                Result.failure(Exception(msg, e))
            } catch (e: LinkageError) {
                releaseFailedEngine()
                val msg = "Kokoro native motoru yüklenemedi: ${e.message ?: "native bağlantı hatası"}"
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Log.e("KokoroTtsEngine", msg, e)
                Result.failure(IllegalStateException(msg, e))
            }
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
            stopRequested = false
            currentCoroutineContext().ensureActive()
            _status.value = TtsStatus.Synthesizing(TtsProvider.KOKORO_OFFLINE, effectiveVoiceId)

            try {
                val sentences = splitTextIntoSentences(cleanText)
                val sentenceAudio = ArrayList<FloatArray>(sentences.size)
                var totalSampleCount = 0
                var detectedSampleRate = 24000

                for (sentence in sentences) {
                    currentCoroutineContext().ensureActive()
                    if (sentence.isBlank()) continue
                    val (sentenceSamples, sr) = generateSpeechSamples(sentence, effectiveVoiceId, clampedSpeed)
                    if (sr > 0) detectedSampleRate = sr
                    sentenceAudio.add(sentenceSamples)
                    totalSampleCount += sentenceSamples.size
                }

                if (totalSampleCount == 0) {
                    _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, "Ses sentezi tamamlanamadı.")
                    return@withContext Result.failure(Exception("Ses sentezi boş sonuç döndürdü."))
                }

                val finalSamples = FloatArray(totalSampleCount)
                var writeOffset = 0
                for (samples in sentenceAudio) {
                    samples.copyInto(finalSamples, destinationOffset = writeOffset)
                    writeOffset += samples.size
                }
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

            } catch (e: CancellationException) {
                _status.value = TtsStatus.Idle
                throw e
            } catch (e: OutOfMemoryError) {
                releaseFailedEngine()
                val msg = "Kokoro ses üretirken cihaz belleği yetersiz kaldı."
                Log.e("KokoroTtsEngine", msg, e)
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Result.failure(IllegalStateException(msg, e))
            } catch (e: Exception) {
                Log.e("KokoroTtsEngine", "Synthesis error: ${e.message}", e)
                val msg = e.message ?: "Ses üretimi sırasında bir hata oluştu."
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Result.failure(Exception(msg))
            } catch (e: LinkageError) {
                releaseFailedEngine()
                val msg = "Kokoro native sentez hatası: ${e.message ?: "native hata"}"
                Log.e("KokoroTtsEngine", msg, e)
                _status.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, msg)
                Result.failure(IllegalStateException(msg, e))
            }
        }
    }

    override fun stop() {
        stopRequested = true
        player.stop()
        if (_status.value is TtsStatus.Synthesizing || _status.value is TtsStatus.Playing) {
            _status.value = TtsStatus.Idle
        }
    }

    override fun release() {
        stop()
        synchronized(nativeLock) {
            offlineTts?.release()
            offlineTts = null
            isEngineInitialized = false
        }
        _status.value = if (modelManager.isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    }

    private fun releaseFailedEngine() {
        synchronized(nativeLock) {
            try {
                offlineTts?.release()
            } catch (e: Throwable) {
                Log.e("KokoroTtsEngine", "Error releasing failed Kokoro engine: ${e.message}")
            } finally {
                offlineTts = null
                isEngineInitialized = false
            }
        }
    }

    private fun splitTextIntoSentences(text: String): List<String> {
        val delimiters = Regex("(?<=[.!?;\n])\\s+")
        val parts = text.split(delimiters)
        val sentenceParts = if (parts.isNotEmpty()) parts else listOf(text)
        return sentenceParts.flatMap { splitLongSentence(it.trim()) }.filter { it.isNotBlank() }
    }

    private fun splitLongSentence(sentence: String, maxChars: Int = 220): List<String> {
        if (sentence.length <= maxChars) return listOf(sentence)

        val chunks = mutableListOf<String>()
        var remaining = sentence
        while (remaining.length > maxChars) {
            val window = remaining.take(maxChars)
            val splitAt = listOf(
                window.lastIndexOf(", "),
                window.lastIndexOf("; "),
                window.lastIndexOf(": "),
                window.lastIndexOf(" ")
            ).filter { it > maxChars / 2 }.maxOrNull() ?: maxChars

            chunks += remaining.take(splitAt).trim()
            remaining = remaining.drop(splitAt).trim()
        }
        if (remaining.isNotBlank()) chunks += remaining
        return chunks
    }

    private suspend fun generateSpeechSamples(
        sentence: String,
        voiceId: String,
        speed: Float
    ): Pair<FloatArray, Int> {
        val coroutineContext = currentCoroutineContext()
        coroutineContext.ensureActive()
        val speakerId = KokoroModelManifest.getSpeakerId(voiceId)
        val config = GenerationConfig(
            silenceScale = 0.2f,
            speed = speed,
            sid = speakerId
        )

        val generated = synchronized(nativeLock) {
            val engine = offlineTts
                ?: throw IllegalStateException("Kokoro motoru başlatılmadı.")
            engine.generateWithConfigAndCallback(
                text = sentence,
                config = config
            ) {
                if (!stopRequested && coroutineContext.isActive) 1 else 0
            }
        }

        coroutineContext.ensureActive()
        if (generated.samples.isEmpty() || generated.sampleRate <= 0) {
            throw IllegalStateException("Kokoro boş ses verisi döndürdü.")
        }
        return generated.samples to generated.sampleRate
    }

}

