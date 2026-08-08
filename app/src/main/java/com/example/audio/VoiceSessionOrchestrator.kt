package com.example.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceSessionState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SYNTHESIZING,
    SPEAKING,
    ERROR
}

data class VoiceLatencyMetrics(
    val timeToFirstSoundMs: Long = 0,
    val totalTurnLatencyMs: Long = 0,
    val totalAudioChunks: Int = 0,
    val interruptedCount: Int = 0
)

class VoiceSessionOrchestrator(
    private val ttsManager: TextToSpeechManager,
    private val speechRecognizerManager: SpeechRecognizerManager
) {
    private val _sessionState = MutableStateFlow(VoiceSessionState.IDLE)
    val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _metrics = MutableStateFlow(VoiceLatencyMetrics())
    val metrics: StateFlow<VoiceLatencyMetrics> = _metrics.asStateFlow()

    private var turnStartTimeMs: Long = 0
    private var firstSoundTimeMs: Long = 0

    fun onUserStartedSpeaking() {
        // Immediate interruption handling per Master Prompt H
        if (_sessionState.value == VoiceSessionState.SPEAKING || _sessionState.value == VoiceSessionState.SYNTHESIZING) {
            ttsManager.stop()
            _metrics.value = _metrics.value.copy(interruptedCount = _metrics.value.interruptedCount + 1)
        }
        _sessionState.value = VoiceSessionState.LISTENING
        turnStartTimeMs = System.currentTimeMillis()
    }

    fun onUserFinishedSpeaking() {
        speechRecognizerManager.stopListening()
        _sessionState.value = VoiceSessionState.TRANSCRIBING
    }

    fun onStartThinking() {
        _sessionState.value = VoiceSessionState.THINKING
    }

    fun onStartSynthesizing() {
        _sessionState.value = VoiceSessionState.SYNTHESIZING
    }

    fun onStartSpeaking(messageId: Long, text: String, coachGender: com.example.data.model.CoachGender) {
        _sessionState.value = VoiceSessionState.SPEAKING
        firstSoundTimeMs = System.currentTimeMillis()

        val timeToFirstSound = if (turnStartTimeMs > 0) firstSoundTimeMs - turnStartTimeMs else 0
        _metrics.value = _metrics.value.copy(
            timeToFirstSoundMs = timeToFirstSound,
            totalTurnLatencyMs = timeToFirstSound
        )

        ttsManager.speak(text, messageId, coachGender)
    }

    fun onSpeechFinished() {
        _sessionState.value = VoiceSessionState.IDLE
    }

    fun onError(message: String) {
        _sessionState.value = VoiceSessionState.ERROR
    }

    fun reset() {
        ttsManager.stop()
        speechRecognizerManager.stopListening()
        _sessionState.value = VoiceSessionState.IDLE
    }
}
