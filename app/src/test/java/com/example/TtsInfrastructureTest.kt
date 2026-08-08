package com.example

import com.example.audio.TtsEngineMode
import com.example.audio.FallbackEngineOption
import com.example.audio.TtsProvider
import com.example.audio.TtsStatus
import com.example.audio.TtsTestResult
import com.example.audio.kokoro.KokoroModelManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsInfrastructureTest {

    @Test
    fun testTtsEngineModeDefaults() {
        assertEquals("KOKORO_OFFLINE", TtsEngineMode.KOKORO_OFFLINE.name)
        assertTrue(TtsEngineMode.KOKORO_OFFLINE.displayName.contains("Kokoro"))
        assertTrue(TtsEngineMode.EDGE_EXPERIMENTAL.displayName.contains("Microsoft Edge"))
        assertTrue(TtsEngineMode.ANDROID_SYSTEM.displayName.contains("Android"))
    }

    @Test
    fun testTtsStatusSealedClass() {
        val idleStatus: TtsStatus = TtsStatus.Idle
        assertTrue(idleStatus is TtsStatus.Idle)

        val errorStatus: TtsStatus = TtsStatus.Error(TtsProvider.EDGE_CONSUMER, "Microsoft Edge Consumer TTS reddedildi: HTTP 403")
        if (errorStatus is TtsStatus.Error) {
            assertEquals(TtsProvider.EDGE_CONSUMER, errorStatus.provider)
            assertEquals("Microsoft Edge Consumer TTS reddedildi: HTTP 403", errorStatus.message)
        }
    }

    @Test
    fun testTtsTestResultParsing() {
        val successResult = TtsTestResult(
            success = true,
            provider = TtsProvider.EDGE_CONSUMER,
            engineName = "Edge Consumer",
            voiceId = "en-US-AvaNeural",
            httpStatusCode = 200,
            message = "Test başarılı"
        )
        assertTrue(successResult.success)
        assertEquals(200, successResult.httpStatusCode)

        val unauthResult = TtsTestResult(
            success = false,
            provider = TtsProvider.EDGE_CONSUMER,
            engineName = "Edge Consumer",
            voiceId = "en-US-AvaNeural",
            httpStatusCode = 403,
            message = "Microsoft Edge Consumer TTS reddedildi: HTTP 403"
        )
        assertFalse(unauthResult.success)
        assertEquals(403, unauthResult.httpStatusCode)
        assertTrue(unauthResult.message.contains("HTTP 403"))
    }

    @Test
    fun testAndroidFallbackIsExplicitChoice() {
        assertEquals("OFF", FallbackEngineOption.OFF.name)
        assertTrue(FallbackEngineOption.ANDROID_SYSTEM.displayName.contains("Android"))
    }

    @Test
    fun testKokoroVoiceIdsMapToOfficialSpeakerIds() {
        assertEquals(3, KokoroModelManifest.getSpeakerId("af_heart"))
        assertEquals(2, KokoroModelManifest.getSpeakerId("af_bella"))
        assertEquals(9, KokoroModelManifest.getSpeakerId("af_sarah"))
        assertEquals(16, KokoroModelManifest.getSpeakerId("am_michael"))
        assertEquals(11, KokoroModelManifest.getSpeakerId("am_adam"))
        assertEquals(15, KokoroModelManifest.getSpeakerId("am_liam"))
    }
}
