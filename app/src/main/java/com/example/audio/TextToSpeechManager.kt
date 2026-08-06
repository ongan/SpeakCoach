package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.CoachGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val edgeTtsManager = EdgeNeuralTtsManager(context.applicationContext)

    private val _useEdgeNeural = MutableStateFlow(true)
    val useEdgeNeural: StateFlow<Boolean> = _useEdgeNeural

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate

    private val _currentPlayingMessageId = MutableStateFlow<Long?>(null)
    val currentPlayingMessageId: StateFlow<Long?> = _currentPlayingMessageId

    private var currentCoachGender: CoachGender = CoachGender.MAYA

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeechManager", "US English Language not supported in system TTS")
            } else {
                isInitialized = true
                tts?.setSpeechRate(_speechRate.value)
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
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
            }
        })
    }

    fun setUseEdgeNeural(useEdge: Boolean) {
        _useEdgeNeural.value = useEdge
    }

    fun setEdgeVoices(femaleVoice: String, maleVoice: String) {
        edgeTtsManager.femaleVoice = femaleVoice
        edgeTtsManager.maleVoice = maleVoice
    }

    fun setCoachGender(gender: CoachGender) {
        currentCoachGender = gender
        if (isInitialized) {
            applyCoachVoiceAndPitch(gender)
        }
    }

    private fun applyCoachVoiceAndPitch(gender: CoachGender) {
        val ttsObj = tts ?: return

        val pitch = if (gender == CoachGender.LEO) 0.65f else 1.18f
        ttsObj.setPitch(pitch)

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

        if (_useEdgeNeural.value) {
            _isPlaying.value = true
            edgeTtsManager.speak(
                text = text,
                speechRate = _speechRate.value,
                gender = targetGender,
                messageId = messageId,
                onStart = {
                    _isPlaying.value = true
                },
                onDone = {
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                },
                onError = { error ->
                    Log.w("TextToSpeechManager", "Edge Neural TTS failed, falling back to System TTS: ${error.message}")
                    speakWithSystemTts(text, messageId, targetGender)
                }
            )
        } else {
            speakWithSystemTts(text, messageId, targetGender)
        }
    }

    private fun speakWithSystemTts(text: String, messageId: Long?, targetGender: CoachGender) {
        if (!isInitialized) {
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            return
        }
        _currentPlayingMessageId.value = messageId
        applyCoachVoiceAndPitch(targetGender)
        tts?.setSpeechRate(_speechRate.value)
        val utteranceId = messageId?.toString() ?: System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun stop() {
        edgeTtsManager.stop()
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
    }

    fun shutdown() {
        edgeTtsManager.shutdown()
        tts?.stop()
        tts?.shutdown()
    }
}
