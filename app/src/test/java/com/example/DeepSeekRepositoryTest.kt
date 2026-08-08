package com.example

import com.example.data.api.ChatMessageItem
import com.example.data.api.DeepSeekRepository
import com.example.data.api.LlmProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekRepositoryTest {

    @Test
    fun testLlmProviderFromBaseUrl() {
        assertEquals(LlmProvider.CUSTOM, LlmProvider.fromBaseUrl("https://integrate.api.nvidia.com/v1"))
        assertEquals(LlmProvider.DEEPSEEK, LlmProvider.fromBaseUrl("https://api.deepseek.com"))
        assertEquals(LlmProvider.GROQ, LlmProvider.fromBaseUrl("https://api.groq.com/openai/v1"))
        assertEquals(LlmProvider.CUSTOM, LlmProvider.fromBaseUrl("https://my-custom-endpoint.com/v1"))
    }

    @Test
    fun testLegacyModelAutoMigration() {
        // Removed providers are treated as custom endpoints and keep their explicit model.
        val nvidiaUrl = "https://integrate.api.nvidia.com/v1"
        assertEquals(
            "deepseek-ai/deepseek-r1",
            DeepSeekRepository.migrateModelName(nvidiaUrl, "deepseek-ai/deepseek-r1")
        )
        // Groq base URL migration
        val groqUrl = "https://api.groq.com/openai/v1"
        assertEquals(
            "llama-3.3-70b-versatile",
            DeepSeekRepository.migrateModelName(groqUrl, "")
        )
        // Official DeepSeek URL should preserve custom model
        val deepseekUrl = "https://api.deepseek.com"
        assertEquals(
            "deepseek-chat",
            DeepSeekRepository.migrateModelName(deepseekUrl, "deepseek-chat")
        )
    }

    @Test
    fun testFormatAndNormalizeMessages_SystemAtFrontAndUserAtEnd() {
        val history = listOf(
            ChatMessageItem("user", "Hello Coach!"),
            ChatMessageItem("assistant", "Hi there! How are you doing today?")
        )
        val result = DeepSeekRepository.formatAndNormalizeMessages(
            systemPrompt = "System Prompt",
            history = history,
            userInput = "I am fine, thanks!"
        )

        assertEquals("system", result[0].role)
        assertEquals("System Prompt", result[0].content)
        assertEquals("user", result.last().role)
        assertEquals("I am fine, thanks!", result.last().content)
    }

    @Test
    fun testFormatAndNormalizeMessages_MergeConsecutiveUserRoles() {
        val history = listOf(
            ChatMessageItem("user", "First question"),
            ChatMessageItem("user", "Second question")
        )
        val result = DeepSeekRepository.formatAndNormalizeMessages(
            systemPrompt = "System Prompt",
            history = history,
            userInput = "Third question"
        )

        assertEquals(2, result.size)
        assertEquals("system", result[0].role)
        assertEquals("user", result[1].role)
        assertTrue(result[1].content.contains("First question"))
        assertTrue(result[1].content.contains("Second question"))
        assertTrue(result[1].content.contains("Third question"))
    }

    @Test
    fun testFormatAndNormalizeMessages_NoDuplicateCurrentUserMessage() {
        val history = listOf(
            ChatMessageItem("user", "Hello Coach"),
            ChatMessageItem("assistant", "Hi there!"),
            ChatMessageItem("user", "I want to learn English")
        )
        val result = DeepSeekRepository.formatAndNormalizeMessages(
            systemPrompt = "System Prompt",
            history = history,
            userInput = "I want to learn English"
        )

        val userMessages = result.filter { it.role == "user" && it.content == "I want to learn English" }
        assertEquals(1, userMessages.size)
    }

    @Test
    fun testBuildSystemPrompt_IncludesNameNativeLanguageAndLevel() {
        val prompt = DeepSeekRepository.buildSystemPrompt(
            cefrLevel = "CEFR A1",
            userName = "Mehmet",
            nativeLanguage = "Türkçe",
            scenarioContext = "Ordering Fast Food"
        )

        assertTrue(prompt.contains("Mehmet"))
        assertTrue(prompt.contains("Türkçe"))
        assertTrue(prompt.contains("A1"))
        assertTrue(prompt.contains("Ordering Fast Food"))
    }

    @Test
    fun testCleanModelContent_StripsThinkingTagsAndMarkdown() {
        val rawResponse = """
            <think>
            Internal reasoning process thinking about grammar...
            </think>
            ```json
            {
              "feedback": "Use 'went' instead of 'go'.",
              "response": "Where did you go yesterday?"
            }
            ```
        """.trimIndent()

        val cleaned = DeepSeekRepository.cleanModelContent(rawResponse)
        assertFalse(cleaned.contains("<think>"))
        assertFalse(cleaned.contains("```"))
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
    }

    @Test
    fun testScenarioFiltering_SeparatesContexts() {
        val allMessages = listOf(
            com.example.data.local.ChatMessageEntity(1, "USER", "Can I have a latte?", scenario = "Coffee Shop Order"),
            com.example.data.local.ChatMessageEntity(2, "COACH", "Sure, what size?", scenario = "Coffee Shop Order"),
            com.example.data.local.ChatMessageEntity(3, "USER", "Here is my passport", scenario = "Airport Customs"),
            com.example.data.local.ChatMessageEntity(4, "COACH", "Welcome, what is your purpose?", scenario = "Airport Customs")
        )

        val coffeeMessages = allMessages.filter { it.scenario == "Coffee Shop Order" }
        assertEquals(2, coffeeMessages.size)
        assertTrue(coffeeMessages.all { it.scenario == "Coffee Shop Order" })

        val airportMessages = allMessages.filter { it.scenario == "Airport Customs" }
        assertEquals(2, airportMessages.size)
        assertTrue(airportMessages.all { it.scenario == "Airport Customs" })
    }
}
