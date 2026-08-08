package com.example

import com.example.data.api.CoachResponseSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CoachResponseSanitizerTest {
    @Test
    fun malformedJsonEnvelopeReturnsOnlyResponseText() {
        val raw = """
            {
              "response": "Hello Alex! Nice to meet you.",
              "
        """.trimIndent()

        val result = CoachResponseSanitizer.sanitize(raw)

        assertEquals("Hello Alex! Nice to meet you.", result)
        assertFalse(result.contains("response"))
    }

    @Test
    fun validJsonDecodesEscapedSpeechText() {
        val raw = """{"response":"Hello!\nShe said \"welcome\".","feedback":null}"""

        assertEquals(
            "Hello!\nShe said \"welcome\".",
            CoachResponseSanitizer.sanitize(raw)
        )
    }

    @Test
    fun markdownAndDoubleEncodedJsonReturnsOnlyResponseText() {
        val raw = """```json
            {\"response\":\"How was your weekend?\",\"feedback\":null}
            ```""".trimIndent()

        assertEquals("How was your weekend?", CoachResponseSanitizer.sanitize(raw))
    }

    @Test
    fun responsePrefixIsNotSpoken() {
        assertEquals(
            "What would you like to order?",
            CoachResponseSanitizer.sanitize("Response: What would you like to order?")
        )
    }

    @Test
    fun normalConversationTextRemainsUnchanged() {
        val text = "That sounds great. What did you do next?"
        assertEquals(text, CoachResponseSanitizer.sanitize(text))
    }
}
