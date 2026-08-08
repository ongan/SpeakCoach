package com.example

import com.example.audio.TtsEngineMode
import com.example.audio.TtsProvider
import com.example.audio.TtsStatus
import com.example.audio.TtsTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsInfrastructureTest {

    @Test
    fun testTtsEngineModeDefaults() {
        assertEquals("GOOGLE_ONLINE", TtsEngineMode.GOOGLE_ONLINE.name)
        assertTrue(TtsEngineMode.GOOGLE_ONLINE.displayName.contains("Google Online TTS"))
        assertTrue(TtsEngineMode.EDGE_EXPERIMENTAL.displayName.contains("Microsoft Edge"))
        assertTrue(TtsEngineMode.ANDROID_SYSTEM.displayName.contains("Android"))
    }

    @Test
    fun testTtsStatusSealedClass() {
        val idleStatus: TtsStatus = TtsStatus.Idle
        assertTrue(idleStatus is TtsStatus.Idle)

        val errorStatus: TtsStatus = TtsStatus.Error(TtsProvider.MICROSOFT_NEURAL, "Microsoft Neural ses üretilemedi: HTTP 401")
        if (errorStatus is TtsStatus.Error) {
            assertEquals(TtsProvider.MICROSOFT_NEURAL, errorStatus.provider)
            assertEquals("Microsoft Neural ses üretilemedi: HTTP 401", errorStatus.message)
        }
    }

    @Test
    fun testTtsTestResultParsing() {
        val successResult = TtsTestResult(
            success = true,
            provider = TtsProvider.MICROSOFT_NEURAL,
            engineName = "Azure REST API",
            voiceId = "en-US-AvaNeural",
            httpStatusCode = 200,
            message = "Test başarılı"
        )
        assertTrue(successResult.success)
        assertEquals(200, successResult.httpStatusCode)

        val unauthResult = TtsTestResult(
            success = false,
            provider = TtsProvider.MICROSOFT_NEURAL,
            engineName = "Azure REST API",
            voiceId = "en-US-AvaNeural",
            httpStatusCode = 401,
            message = "Microsoft Neural ses üretilemedi: HTTP 401 (Unauthorized - API Key Geçersiz)"
        )
        assertFalse(unauthResult.success)
        assertEquals(401, unauthResult.httpStatusCode)
        assertTrue(unauthResult.message.contains("HTTP 401"))
    }
}
