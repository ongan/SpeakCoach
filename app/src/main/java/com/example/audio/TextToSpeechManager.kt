package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.audio.kokoro.KokoroAudioCacheManager
import com.example.audio.kokoro.KokoroModelManager
import com.example.audio.kokoro.KokoroModelManifest
import com.example.audio.kokoro.KokoroTtsEngine
import com.example.data.model.CoachGender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val edgeTtsManager = EdgeNeuralTtsManager(context.applicationContext)
    val kokoroModelManager = KokoroModelManager(context.applicationContext)
    val kokoroCacheManager = KokoroAudioCacheManager(context.applicationContext)
    val kokoroTtsEngine = KokoroTtsEngine(context.applicationContext, kokoroModelManager, kokoroCacheManager)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeSpeakJob: Job? = null

    private val _engineMode = MutableStateFlow(TtsEngineMode.KOKORO_OFFLINE)
    val engineMode: StateFlow<TtsEngineMode> = _engineMode.asStateFlow()

    private val _fallbackEngineOption = MutableStateFlow(FallbackEngineOption.OFF)
    val fallbackEngineOption: StateFlow<FallbackEngineOption> = _fallbackEngineOption.asStateFlow()

    // Legacy flag compatibility
    private val _allowAutoAndroidFallback = MutableStateFlow(false)
    val allowAutoAndroidFallback: StateFlow<Boolean> = _allowAutoAndroidFallback.asStateFlow()

    private val _ttsStatus = MutableStateFlow<TtsStatus>(
        if (kokoroModelManager.isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    )
    val ttsStatus: StateFlow<TtsStatus> = _ttsStatus.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _userPitch = MutableStateFlow(1.0f)
    val userPitch: StateFlow<Float> = _userPitch.asStateFlow()

    private val _currentPlayingMessageId = MutableStateFlow<Long?>(null)
    val currentPlayingMessageId: StateFlow<Long?> = _currentPlayingMessageId.asStateFlow()

    // Voices configuration
    private val _kokoroFemaleVoice = MutableStateFlow("af_heart")
    val kokoroFemaleVoice: StateFlow<String> = _kokoroFemaleVoice.asStateFlow()

    private val _kokoroMaleVoice = MutableStateFlow("am_michael")
    val kokoroMaleVoice: StateFlow<String> = _kokoroMaleVoice.asStateFlow()

    private var currentCoachGender: CoachGender = CoachGender.MAYA

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeechManager", "US English Language not supported in system TTS")
            } else {
                isInitialized = true
                tts?.setSpeechRate(_speechRate.value)
                tts?.setPitch(_userPitch.value)
                applyCoachVoiceAndPitch(currentCoachGender)
                setupProgressListener()
            }
        } else {
            Log.e("TextToSpeechManager", "System TTS Initialization failed")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                val voiceName = tts?.voice?.name ?: "Android System TTS"
                _ttsStatus.value = TtsStatus.Playing(TtsProvider.ANDROID_SYSTEM, voiceName)
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Error(TtsProvider.ANDROID_SYSTEM, "Android Sistem TTS çalma hatası")
            }
        })
    }

    fun setEngineMode(mode: TtsEngineMode) {
        stop()
        _engineMode.value = mode
        edgeTtsManager.engineMode = mode
        _ttsStatus.value = if (mode == TtsEngineMode.KOKORO_OFFLINE && !kokoroModelManager.isModelReady()) {
            TtsStatus.ModelNotDownloaded
        } else {
            TtsStatus.Idle
        }
    }

    fun setFallbackEngineOption(option: FallbackEngineOption) {
        _fallbackEngineOption.value = option
        _allowAutoAndroidFallback.value = (option != FallbackEngineOption.OFF)
    }

    fun setAllowAutoAndroidFallback(allow: Boolean) {
        _allowAutoAndroidFallback.value = allow
        if (!allow) {
            _fallbackEngineOption.value = FallbackEngineOption.OFF
        }
    }

    fun setEdgeVoices(femaleVoice: String, maleVoice: String) {
        edgeTtsManager.femaleVoice = femaleVoice
        edgeTtsManager.maleVoice = maleVoice
    }

    fun setKokoroVoices(femaleVoice: String, maleVoice: String) {
        _kokoroFemaleVoice.value = femaleVoice
        _kokoroMaleVoice.value = maleVoice
    }

    fun setCoachGender(gender: CoachGender) {
        currentCoachGender = gender
        if (isInitialized) {
            applyCoachVoiceAndPitch(gender)
        }
    }

    private fun applyCoachVoiceAndPitch(gender: CoachGender) {
        val ttsObj = tts ?: return
        ttsObj.setPitch(_userPitch.value)

        try {
            val voices = ttsObj.voices
            if (!voices.isNullOrEmpty()) {
                val englishVoices = voices.filter { it.locale.language == Locale.ENGLISH.language }
                val isMale = (gender == CoachGender.LEO)

                val selectedVoice = if (isMale) {
                    englishVoices.firstOrNull { v ->
                        val name = v.name.lowercase()
                        name.contains("male") || name.contains("-m-") || name.contains("_m_") ||
                        name.contains("iom") || name.contains("sfg") || name.contains("iol") || name.contains("rdb") || name.contains("ngu") ||
                        v.features.contains("male")
                    } ?: englishVoices.firstOrNull { v ->
                        val name = v.name.lowercase()
                        !name.contains("female") && !name.contains("-f-") && !name.contains("_f_") && !name.contains("tpf") && !name.contains("lbc")
                    }
                } else {
                    englishVoices.firstOrNull { v ->
                        val name = v.name.lowercase()
                        name.contains("female") || name.contains("-f-") || name.contains("_f_") ||
                        name.contains("tpf") || name.contains("lbc") ||
                        v.features.contains("female")
                    } ?: englishVoices.firstOrNull()
                }

                if (selectedVoice != null) {
                    ttsObj.voice = selectedVoice
                }
            }
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error setting system voice: ${e.message}")
        }
    }

    fun speak(text: String, messageId: Long? = null, gender: CoachGender? = null) {
        stop()
        _currentPlayingMessageId.value = messageId
        val targetGender = gender ?: currentCoachGender

        when (_engineMode.value) {
            TtsEngineMode.KOKORO_OFFLINE -> {
                speakWithKokoro(text, messageId, targetGender)
            }
            TtsEngineMode.EDGE_EXPERIMENTAL -> {
                speakWithEdge(text, messageId, targetGender)
            }
            TtsEngineMode.ANDROID_SYSTEM -> {
                speakWithSystemTts(text, messageId, targetGender)
            }
        }
    }

    private fun speakWithKokoro(text: String, messageId: Long?, targetGender: CoachGender) {
        if (!kokoroModelManager.isModelReady()) {
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            _ttsStatus.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, "Kokoro ses modeli indirilmemiş.")
            
            // Check fallback
            handleFallback(
                failedProvider = TtsProvider.KOKORO_OFFLINE,
                errorMsg = "Kokoro ses modeli indirilmemiş.",
                text = text,
                messageId = messageId,
                targetGender = targetGender
            )
            return
        }

        val targetVoice = if (targetGender == CoachGender.LEO) _kokoroMaleVoice.value else _kokoroFemaleVoice.value
        _ttsStatus.value = TtsStatus.Synthesizing(TtsProvider.KOKORO_OFFLINE, targetVoice)

        activeSpeakJob = scope.launch(Dispatchers.IO) {
            try {
                val result = kokoroTtsEngine.synthesize(text, targetVoice, _speechRate.value)
                if (result.isSuccess) {
                    val audio = result.getOrThrow()
                    kokoroTtsEngine.player.playAudio(
                        samples = audio.samples,
                        sampleRate = audio.sampleRate,
                        onStart = {
                            _isPlaying.value = true
                            _ttsStatus.value = TtsStatus.Playing(TtsProvider.KOKORO_OFFLINE, targetVoice)
                        },
                        onDone = {
                            _isPlaying.value = false
                            _currentPlayingMessageId.value = null
                            _ttsStatus.value = TtsStatus.Idle
                        },
                        onError = { err ->
                            _isPlaying.value = false
                            _currentPlayingMessageId.value = null
                            val errMsg = err.message ?: "Kokoro çalma hatası"
                            _ttsStatus.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, errMsg)
                            handleFallback(TtsProvider.KOKORO_OFFLINE, errMsg, text, messageId, targetGender)
                        }
                    )
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Kokoro sentez hatası"
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    _ttsStatus.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, errMsg)
                    handleFallback(TtsProvider.KOKORO_OFFLINE, errMsg, text, messageId, targetGender)
                }
            } catch (e: CancellationException) {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Idle
                throw e
            } catch (e: Exception) {
                val errMsg = e.message ?: "Kokoro hatası"
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Error(TtsProvider.KOKORO_OFFLINE, errMsg)
                handleFallback(TtsProvider.KOKORO_OFFLINE, errMsg, text, messageId, targetGender)
            }
        }
    }

    private fun speakWithEdge(text: String, messageId: Long?, targetGender: CoachGender) {
        _isPlaying.value = true
        _ttsStatus.value = TtsStatus.Synthesizing(TtsProvider.EDGE_CONSUMER, "Connecting")

        val activeVoice = if (targetGender == CoachGender.LEO) edgeTtsManager.maleVoice else edgeTtsManager.femaleVoice

        edgeTtsManager.speak(
            text = text,
            speechRate = _speechRate.value,
            gender = targetGender,
            messageId = messageId,
            onStart = { engineName ->
                _isPlaying.value = true
                _ttsStatus.value = TtsStatus.Playing(TtsProvider.EDGE_CONSUMER, activeVoice)
            },
            onDone = {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Idle
            },
            onError = { error ->
                val errMsg = error.message ?: "Edge Online TTS hatası"
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                _ttsStatus.value = TtsStatus.Error(TtsProvider.EDGE_CONSUMER, errMsg)
                handleFallback(TtsProvider.EDGE_CONSUMER, errMsg, text, messageId, targetGender)
            }
        )
    }

    private fun handleFallback(
        failedProvider: TtsProvider,
        errorMsg: String,
        text: String,
        messageId: Long?,
        targetGender: CoachGender
    ) {
        val option = _fallbackEngineOption.value
        Log.w("TextToSpeechManager", "Provider $failedProvider failed ($errorMsg). Fallback option = $option")

        when (option) {
            FallbackEngineOption.OFF -> {
                Log.i("TextToSpeechManager", "Fallback is OFF. No fallback engine executed.")
            }
            FallbackEngineOption.KOKORO -> {
                if (failedProvider != TtsProvider.KOKORO_OFFLINE && kokoroModelManager.isModelReady()) {
                    Log.i("TextToSpeechManager", "Falling back to Kokoro Offline Neural...")
                    speakWithKokoro(text, messageId, targetGender)
                } else if (!kokoroModelManager.isModelReady()) {
                    Log.w("TextToSpeechManager", "Kokoro model is not ready for fallback.")
                    _ttsStatus.value = TtsStatus.Error(failedProvider, "$errorMsg (Kokoro modeli indirilmemiş)")
                }
            }
            FallbackEngineOption.ANDROID_SYSTEM -> {
                if (failedProvider != TtsProvider.ANDROID_SYSTEM) {
                    Log.i("TextToSpeechManager", "Falling back to Android System TTS after $failedProvider failed.")
                    _ttsStatus.value = TtsStatus.Error(
                        failedProvider,
                        "$errorMsg Android System TTS fallback calistiriliyor."
                    )
                    speakWithSystemTts(text, messageId, targetGender)
                }
            }
        }
    }

    private fun speakWithSystemTts(text: String, messageId: Long?, targetGender: CoachGender) {
        if (!isInitialized) {
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            _ttsStatus.value = TtsStatus.Error(TtsProvider.ANDROID_SYSTEM, "Sistem TTS henüz başlatılmadı")
            return
        }
        _currentPlayingMessageId.value = messageId
        _isPlaying.value = true
        applyCoachVoiceAndPitch(targetGender)
        tts?.setSpeechRate(_speechRate.value)
        tts?.setPitch(_userPitch.value)

        val voiceName = tts?.voice?.name ?: "Android System Default"
        _ttsStatus.value = TtsStatus.Playing(TtsProvider.ANDROID_SYSTEM, voiceName)

        val utteranceId = messageId?.toString() ?: System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    suspend fun testConnection(
        mode: TtsEngineMode,
        voice: String
    ): TtsTestResult {
        return when (mode) {
            TtsEngineMode.KOKORO_OFFLINE -> {
                if (!kokoroModelManager.isModelReady()) {
                    TtsTestResult(
                        success = false,
                        provider = TtsProvider.KOKORO_OFFLINE,
                        engineName = "Kokoro Offline Neural",
                        voiceId = voice,
                        httpStatusCode = null,
                        message = "Kokoro ses modeli indirilmemiş."
                    )
                } else {
                    val res = kokoroTtsEngine.synthesize("Kokoro ses testi başarılı.", voice, 1.0f)
                    if (res.isSuccess) {
                        TtsTestResult(
                            success = true,
                            provider = TtsProvider.KOKORO_OFFLINE,
                            engineName = "Kokoro Offline Neural",
                            voiceId = voice,
                            httpStatusCode = null,
                            message = "Kokoro yerel ses üretimi başarılı!"
                        )
                    } else {
                        TtsTestResult(
                            success = false,
                            provider = TtsProvider.KOKORO_OFFLINE,
                            engineName = "Kokoro Offline Neural",
                            voiceId = voice,
                            httpStatusCode = null,
                            message = res.exceptionOrNull()?.message ?: "Kokoro ses sentez hatası."
                        )
                    }
                }
            }
            TtsEngineMode.EDGE_EXPERIMENTAL -> {
                edgeTtsManager.testConnection(mode, voice)
            }
            TtsEngineMode.ANDROID_SYSTEM -> {
                TtsTestResult(
                    success = true,
                    provider = TtsProvider.ANDROID_SYSTEM,
                    engineName = "Android System TTS",
                    voiceId = "System Default",
                    httpStatusCode = 200,
                    message = "Android sistem TTS motoru cihazınızda hazır."
                )
            }
        }
    }

    fun clearAudioCache() {
        kokoroCacheManager.clearCache()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.8f, 1.2f)
        _userPitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun stop() {
        activeSpeakJob?.cancel()
        activeSpeakJob = null
        kokoroTtsEngine.stop()
        edgeTtsManager.stop()
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
        _ttsStatus.value = if (_engineMode.value == TtsEngineMode.KOKORO_OFFLINE && !kokoroModelManager.isModelReady()) {
            TtsStatus.ModelNotDownloaded
        } else {
            TtsStatus.Idle
        }
    }

    fun shutdown() {
        stop()
        kokoroTtsEngine.release()
        edgeTtsManager.shutdown()
        tts?.stop()
        tts?.shutdown()
        _ttsStatus.value = TtsStatus.Idle
    }
}
