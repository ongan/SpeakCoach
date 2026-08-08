package com.example.audio.kokoro

data class KokoroVoiceInfo(
    val id: String,
    val displayName: String,
    val isFemale: Boolean
)

object KokoroModelManifest {
    const val MODEL_VERSION = "v1.1"
    const val DOWNLOAD_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_1.tar.bz2"
    
    // Official release archive hash or null if verified dynamically
    val EXPECTED_SHA256: String? = null 

    const val ESTIMATED_DOWNLOAD_BYTES = 90_000_000L // ~90MB

    val FEMALE_VOICES = listOf(
        KokoroVoiceInfo("af_heart", "Heart (Amerikan Kadın - En Doğal)", isFemale = true),
        KokoroVoiceInfo("af_bella", "Bella (Amerikan Kadın - Berrak)", isFemale = true),
        KokoroVoiceInfo("af_sarah", "Sarah (Amerikan Kadın - Sıcak)", isFemale = true)
    )

    val MALE_VOICES = listOf(
        KokoroVoiceInfo("am_michael", "Michael (Amerikan Erkek - Güçlü)", isFemale = false),
        KokoroVoiceInfo("am_adam", "Adam (Amerikan Erkek - Sakin)", isFemale = false),
        KokoroVoiceInfo("am_liam", "Liam (Amerikan Erkek - Dinamik)", isFemale = false)
    )

    val ALL_VOICES = FEMALE_VOICES + MALE_VOICES

    val REQUIRED_FILES = listOf(
        "model.onnx",
        "voices.bin",
        "tokens.txt"
    )

    fun getDefaultVoice(isFemale: Boolean): String {
        return if (isFemale) "af_heart" else "am_michael"
    }

    fun isValidVoice(voiceId: String): Boolean {
        return ALL_VOICES.any { it.id == voiceId }
    }
}
