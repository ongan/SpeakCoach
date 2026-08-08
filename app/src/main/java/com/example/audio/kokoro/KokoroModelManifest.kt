package com.example.audio.kokoro

data class KokoroVoiceInfo(
    val id: String,
    val displayName: String,
    val isFemale: Boolean,
    val speakerId: Int
)

object KokoroModelManifest {
    const val MODEL_VERSION = "v1.0"
    const val DOWNLOAD_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
    
    // Official release archive hash or null if verified dynamically
    val EXPECTED_SHA256: String? = null 

    const val ESTIMATED_DOWNLOAD_BYTES = 360_000_000L // ~360MB

    val FEMALE_VOICES = listOf(
        KokoroVoiceInfo("af_heart", "Heart (Amerikan Kadın - En Doğal)", isFemale = true, speakerId = 3),
        KokoroVoiceInfo("af_bella", "Bella (Amerikan Kadın - Berrak)", isFemale = true, speakerId = 2),
        KokoroVoiceInfo("af_sarah", "Sarah (Amerikan Kadın - Sıcak)", isFemale = true, speakerId = 9)
    )

    val MALE_VOICES = listOf(
        KokoroVoiceInfo("am_michael", "Michael (Amerikan Erkek - Güçlü)", isFemale = false, speakerId = 16),
        KokoroVoiceInfo("am_adam", "Adam (Amerikan Erkek - Sakin)", isFemale = false, speakerId = 11),
        KokoroVoiceInfo("am_liam", "Liam (Amerikan Erkek - Dinamik)", isFemale = false, speakerId = 15)
    )

    val ALL_VOICES = FEMALE_VOICES + MALE_VOICES

    val REQUIRED_FILES = listOf(
        "model.onnx",
        "voices.bin",
        "tokens.txt",
        "lexicon-us-en.txt",
        "espeak-ng-data"
    )

    fun getDefaultVoice(isFemale: Boolean): String {
        return if (isFemale) "af_heart" else "am_michael"
    }

    fun isValidVoice(voiceId: String): Boolean {
        return ALL_VOICES.any { it.id == voiceId }
    }

    fun getSpeakerId(voiceId: String): Int {
        return ALL_VOICES.firstOrNull { it.id == voiceId }?.speakerId
            ?: ALL_VOICES.first { it.id == getDefaultVoice(isFemale = true) }.speakerId
    }
}
