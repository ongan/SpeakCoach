package com.example.audio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EdgeConsumerTtsEngine(
    private val context: Context,
    private val edgeManager: EdgeNeuralTtsManager = EdgeNeuralTtsManager(context)
) : TtsEngine {

    private val _status = MutableStateFlow<TtsStatus>(TtsStatus.Idle)
    override val status: StateFlow<TtsStatus> = _status.asStateFlow()

    override suspend fun initialize(): Result<Unit> {
        _status.value = TtsStatus.Idle
        return Result.success(Unit)
    }

    override suspend fun synthesize(
        text: String,
        voiceId: String,
        speed: Float
    ): Result<TtsAudio> {
        // Synthesis & playback managed by EdgeNeuralTtsManager
        return Result.failure(Exception("Edge Consumer synthesizes and plays audio dynamically."))
    }

    override fun stop() {
        edgeManager.stop()
        _status.value = TtsStatus.Idle
    }

    override fun release() {
        stop()
        edgeManager.shutdown()
    }
}
