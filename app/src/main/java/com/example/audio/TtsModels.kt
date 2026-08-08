package com.example.audio

import kotlinx.coroutines.flow.StateFlow

enum class TtsProvider(val displayName: String) {
    KOKORO_OFFLINE("Kokoro Offline Neural"),
    EDGE_CONSUMER("Microsoft Edge Consumer"),
    ANDROID_SYSTEM("Android Sistem TTS")
}

enum class TtsEngineMode(val displayName: String, val provider: TtsProvider) {
    KOKORO_OFFLINE("Kokoro Offline Neural (Önerilen)", TtsProvider.KOKORO_OFFLINE),
    EDGE_EXPERIMENTAL("Microsoft Edge Consumer (Deneysel)", TtsProvider.EDGE_CONSUMER),
    ANDROID_SYSTEM("Android Sistem TTS (Cihaz Dahili)", TtsProvider.ANDROID_SYSTEM)
}

enum class FallbackEngineOption(val displayName: String) {
    OFF("Kapalı"),
    KOKORO("Kokoro hazırsa Kokoro"),
    ANDROID_SYSTEM("Android Sistem TTS")
}

interface TtsEngine {
    val status: StateFlow<TtsStatus>

    suspend fun initialize(): Result<Unit>

    suspend fun synthesize(
        text: String,
        voiceId: String,
        speed: Float
    ): Result<TtsAudio>

    fun stop()

    fun release()
}

data class TtsAudio(
    val samples: FloatArray,
    val sampleRate: Int,
    val provider: TtsProvider,
    val voiceId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TtsAudio
        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false
        if (provider != other.provider) return false
        if (voiceId != other.voiceId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + provider.hashCode()
        result = 31 * result + voiceId.hashCode()
        return result
    }
}

sealed class TtsStatus {
    object Idle : TtsStatus()
    object ModelNotDownloaded : TtsStatus()

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float
    ) : TtsStatus()

    object Verifying : TtsStatus()
    object Initializing : TtsStatus()

    data class Synthesizing(
        val provider: TtsProvider,
        val voiceId: String
    ) : TtsStatus()

    data class Playing(
        val provider: TtsProvider,
        val voiceId: String
    ) : TtsStatus()

    data class Error(
        val provider: TtsProvider,
        val message: String
    ) : TtsStatus()
}

data class TtsTestResult(
    val success: Boolean,
    val provider: TtsProvider,
    val engineName: String,
    val voiceId: String,
    val httpStatusCode: Int? = null,
    val message: String
)
