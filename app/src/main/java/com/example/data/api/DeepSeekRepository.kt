package com.example.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class WordDefinition(
    val word: String,
    val meaning: String,
    val exampleSentence: String,
    val contextSentence: String? = null
)

class ApiException(message: String) : Exception(message)

enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val availableModels: List<String>
) {
    CEREBRAS(
        displayName = "Cerebras AI",
        defaultBaseUrl = "https://api.cerebras.ai/v1",
        defaultModel = "llama-3.3-70b",
        availableModels = listOf(
            "llama-3.3-70b",
            "llama3.1-8b",
            "llama3.1-70b",
            "deepseek-r1-distill-llama-70b"
        )
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-1.5-flash",
        availableModels = listOf(
            "gemini-1.5-flash",
            "gemini-2.5-flash",
            "gemini-3.5-flash",
            "gemini-1.5-pro"
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek Direct",
        defaultBaseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        availableModels = listOf(
            "deepseek-chat",
            "deepseek-reasoner"
        )
    ),
    GROQ(
        displayName = "Groq AI",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        availableModels = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "deepseek-r1-distill-llama-70b"
        )
    ),
    CUSTOM(
        displayName = "Custom Endpoint",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        availableModels = emptyList()
    );

    companion object {
        fun fromBaseUrl(url: String): LlmProvider {
            val lower = url.lowercase()
            return when {
                lower.contains("cerebras") -> CEREBRAS
                lower.contains("generativelanguage") || lower.contains("gemini") || lower.contains("google") -> GEMINI
                lower.contains("deepseek") -> DEEPSEEK
                lower.contains("groq") -> GROQ
                else -> CUSTOM
            }
        }
    }
}

class DeepSeekRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val coachAdapter = moshi.adapter(CoachJsonResponse::class.java)
    private val stringAdapter = moshi.adapter(String::class.java)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader("Authorization")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(DeepSeekApiService::class.java)

    companion object {
        const val DEFAULT_CEREBRAS_MODEL = "llama-3.3-70b"
        const val DEFAULT_CEREBRAS_BASE_URL = "https://api.cerebras.ai/v1"

        const val DEFAULT_GEMINI_MODEL = "gemini-1.5-flash"
        const val DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai"

        const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
        const val DEFAULT_GROQ_BASE_URL = "https://api.groq.com/openai/v1"

        fun buildSystemPrompt(
            cefrLevel: String,
            userName: String,
            nativeLanguage: String,
            scenarioContext: String?,
            avoidReplies: List<String> = emptyList(),
            userInterests: String = "",
            conversationSummary: String = "",
            learnedFacts: String = ""
        ): String {
            val levelInstruction = when {
                cefrLevel.contains("A1", ignoreCase = true) ->
                    "Target CEFR Level: A1 (Elementary). Use ultra-simple English, short sentences (under 10 words), basic present tense, and high encouragement. Avoid complex idioms."
                cefrLevel.contains("A2", ignoreCase = true) ->
                    "Target CEFR Level: A2 (Pre-Intermediate). Use simple everyday English, clear vocabulary, basic past/future tenses, and simple questions."
                cefrLevel.contains("B1", ignoreCase = true) ->
                    "Target CEFR Level: B1 (Intermediate). Use natural everyday conversational English with standard vocabulary and varied sentence structures."
                cefrLevel.contains("B2", ignoreCase = true) ->
                    "Target CEFR Level: B2 (Upper-Intermediate). Use natural professional and social English, idiomatic expressions, and engaging discussions."
                cefrLevel.contains("C1", ignoreCase = true) || cefrLevel.contains("C2", ignoreCase = true) ->
                    "Target CEFR Level: C1 (Advanced). Use sophisticated vocabulary, nuanced expressions, complex sentence structures, and high-level conversational depth."
                else ->
                    "Target CEFR Level: $cefrLevel. Use natural, clear everyday English."
            }

            val userGreetingContext = if (userName.isNotBlank()) {
                "User's name: '$userName'. Address the user by name naturally in conversation."
            } else ""

            val languageContext = if (nativeLanguage.isNotBlank()) {
                "User's native language: '$nativeLanguage'."
            } else "User's native language: Turkish."

            val interestsPrompt = if (userInterests.isNotBlank()) {
                "\nUSER HOBBIES & INTERESTS:\n$userInterests\nIncorporate these interests naturally into casual conversations, icebreakers, or analogies."
            } else ""

            val memoryPrompt = if (conversationSummary.isNotBlank() || learnedFacts.isNotBlank()) {
                "\nBACKGROUND MEMORY & CONVERSATION CONTINUITY:\n" +
                        (if (conversationSummary.isNotBlank()) "Recent Conversation Context: $conversationSummary\n" else "") +
                        (if (learnedFacts.isNotBlank()) "Learned Facts About User: $learnedFacts\n" else "") +
                        "CRITICAL: Maintain continuous dialogue context! Refer back seamlessly to what was previously discussed."
            } else ""

            val scenarioPrompt = if (!scenarioContext.isNullOrBlank()) {
                "\nACTIVE ROLEPLAY SCENARIO & CONTEXT:\n$scenarioContext\nStay in character! Drive the conversation organically based on the active scenario stage and variables."
            } else ""

            val avoidPrompt = if (avoidReplies.isNotEmpty()) {
                "\nCRITICAL ANTI-REPETITION NOTICE: Do NOT repeat or use similar phrasing to these recent replies:\n" +
                        avoidReplies.joinToString("\n") { "- \"$it\"" } + "\nProvide a completely fresh, distinct response!"
            } else ""

            return """You are an expert, empathetic, and highly interactive English Language Coach for a mobile learning app.

PRIMARY OBJECTIVE:
Engage $userName in natural, dynamic English dialogue tailored to their level, maintaining high conversational depth while correcting mistakes in their native language ($nativeLanguage).

USER PROFILE:
- $userGreetingContext
- $languageContext
- $levelInstruction
$interestsPrompt
$memoryPrompt
$scenarioPrompt
$avoidPrompt

PEDAGOGICAL & CONVERSATIONAL RULES:
1. RESPONSE CHARACTER, MEMORY & DEPTH:
   - Treat the conversation as an ongoing personal friendship. Never feel like a cold stateless system.
   - React directly to the specifics of what the user said in their latest message.
   - Refer seamlessly to previously discussed topics or learned facts from the BACKGROUND MEMORY.
   - Share thoughts, opinions, and realistic persona details instead of just asking questions.
   - Ask at most ONE follow-up question per turn. In some turns, respond naturally WITHOUT asking any question.
   - NEVER repeat previous questions or rephrase a question the user already answered.
   - Avoid generic overused praise ("That's great!", "That's interesting!"). Be genuine, patient, and encouraging.
   - If user strays slightly off topic, build a natural bridge back to the topic without being harsh.

2. CORRECTION & FEEDBACK:
   - Provide corrections ONLY if the user made genuine grammar, vocabulary, or phrasing errors.
   - Write the "feedback" field in the user's native language ($nativeLanguage). Keep it concise (1 sentence correction + 1 short explanation).
   - If the user's sentence is grammatically correct and natural, set "feedback" to null! Do NOT fabricate minor nitpicks.

3. SCENARIO PROGRESSION:
   - Dynamically advance the stage (OPENING -> DISCOVERY -> COMPLICATION -> RESOLUTION -> REVIEW) as goals are met.
   - Track completed goals and learned facts in the output JSON.

4. STRICT JSON OUTPUT FORMAT:
Respond exclusively with valid JSON (no markdown formatting, no commentary):
{
  "response": "Dynamic coach response in natural English (1-3 sentences)",
  "feedback": null or "Düzeltme & Açıklama in $nativeLanguage",
  "stage": "OPENING | DISCOVERY | COMPLICATION | RESOLUTION | REVIEW",
  "completedGoalIds": ["goal_id_if_any"],
  "learnedUserFacts": ["fact_if_any"],
  "shouldCompleteScenario": false
}"""
        }

        fun isSimilarToPrevious(newText: String, previousTexts: List<String>): Boolean {
            val cleanNew = newText.lowercase().trim()
            if (cleanNew.isBlank()) return false
            for (prev in previousTexts) {
                val cleanPrev = prev.lowercase().trim()
                if (cleanPrev.isBlank()) continue
                if (cleanNew == cleanPrev) return true
                val wordsNew = cleanNew.split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
                val wordsPrev = cleanPrev.split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
                if (wordsNew.isNotEmpty() && wordsPrev.isNotEmpty()) {
                    val intersection = wordsNew.intersect(wordsPrev).size
                    val jaccard = intersection.toDouble() / wordsNew.union(wordsPrev).size
                    if (jaccard >= 0.70) return true
                }
            }
            return false
        }

        fun migrateModelName(baseUrl: String, modelName: String): String {
            if (baseUrl.contains("cerebras", ignoreCase = true)) {
                if (modelName.isBlank() || modelName.contains("nvidia", ignoreCase = true) || modelName.contains("v4-flash")) {
                    return DEFAULT_CEREBRAS_MODEL
                }
            } else if (baseUrl.contains("generativelanguage") || baseUrl.contains("gemini") || baseUrl.contains("google")) {
                if (modelName.isBlank() || modelName.contains("nvidia", ignoreCase = true)) {
                    return DEFAULT_GEMINI_MODEL
                }
            } else if (baseUrl.contains("groq", ignoreCase = true)) {
                if (modelName.isBlank()) {
                    return DEFAULT_GROQ_MODEL
                }
            }
            return modelName.ifBlank { DEFAULT_CEREBRAS_MODEL }
        }

        fun formatAndNormalizeMessages(
            systemPrompt: String,
            history: List<ChatMessageItem>,
            userInput: String,
            maxHistoryCount: Int = 10
        ): List<ChatMessageItem> {
            val recentHistory = if (history.size > maxHistoryCount) history.takeLast(maxHistoryCount) else history
            val rawItems = mutableListOf<ChatMessageItem>()
            rawItems.addAll(recentHistory)

            val trimmedUser = userInput.trim()
            if (trimmedUser.isNotEmpty()) {
                val lastItem = rawItems.lastOrNull()
                if (lastItem == null || !(lastItem.role == "user" && lastItem.content == trimmedUser)) {
                    rawItems.add(ChatMessageItem("user", trimmedUser))
                }
            }

            val mergedList = mutableListOf<ChatMessageItem>()
            for (item in rawItems) {
                if (mergedList.isEmpty()) {
                    mergedList.add(item)
                } else {
                    val prev = mergedList.last()
                    if (prev.role == item.role) {
                        mergedList[mergedList.lastIndex] = ChatMessageItem(
                            role = prev.role,
                            content = "${prev.content}\n${item.content}"
                        )
                    } else {
                        mergedList.add(item)
                    }
                }
            }

            val result = mutableListOf<ChatMessageItem>()
            result.add(ChatMessageItem("system", systemPrompt))
            result.addAll(mergedList)
            return result
        }

        fun cleanModelContent(rawContent: String): String {
            val withoutThink = rawContent.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
            val cleanJson = withoutThink.replace("```json", "").replace("```", "").trim()
            return cleanJson
        }
    }

    suspend fun getCoachResponse(
        userInput: String,
        apiKey: String,
        baseUrl: String = DEFAULT_CEREBRAS_BASE_URL,
        modelName: String = DEFAULT_CEREBRAS_MODEL,
        cefrLevel: String = "CEFR B1-B2",
        userName: String = "",
        nativeLanguage: String = "Turkish (Türkçe)",
        scenarioContext: String? = null,
        chatHistory: List<ChatMessageItem> = emptyList(),
        recentAssistantReplies: List<String> = emptyList(),
        userInterests: String = "",
        conversationSummary: String = "",
        learnedFacts: String = ""
    ): CoachJsonResponse {
        val trimmedKey = apiKey.trim()
        val provider = LlmProvider.fromBaseUrl(baseUrl)
        val providerName = provider.displayName

        if (trimmedKey.isEmpty()) {
            throw ApiException("$providerName API anahtarı gerekli")
        }

        val effectiveModel = migrateModelName(baseUrl, modelName)
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val endpointUrl = "$cleanBaseUrl/chat/completions"

        var effectiveSystemPrompt = buildSystemPrompt(
            cefrLevel = cefrLevel,
            userName = userName,
            nativeLanguage = nativeLanguage,
            scenarioContext = scenarioContext,
            avoidReplies = emptyList(),
            userInterests = userInterests,
            conversationSummary = conversationSummary,
            learnedFacts = learnedFacts
        )

        var normalizedMessages = formatAndNormalizeMessages(
            systemPrompt = effectiveSystemPrompt,
            history = chatHistory,
            userInput = userInput
        )

        val useJsonFormat = provider == LlmProvider.DEEPSEEK
        val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"

        try {
            var requestBody = ChatCompletionRequest(
                model = effectiveModel,
                messages = normalizedMessages,
                temperature = 0.7,
                maxTokens = 600,
                stream = false,
                responseFormat = if (useJsonFormat) ResponseFormat("json_object") else null
            )

            var response = apiService.createChatCompletion(
                url = endpointUrl,
                authorization = authHeader,
                request = requestBody
            )

            if (!response.isSuccessful) {
                val code = response.code()
                val errorMsg = parseHttpError(providerName, code)
                Log.e("DeepSeekRepository", errorMsg)
                throw ApiException(errorMsg)
            }

            var body = response.body()
            var rawContent = body?.choices?.firstOrNull()?.message?.content
            if (rawContent.isNullOrBlank()) {
                throw ApiException("$providerName cevabı boş döndü.")
            }

            var parsed = parseCoachResponse(rawContent, providerName)

            // Anti-repetition check: If response is too similar to recent assistant replies, retry once
            if (recentAssistantReplies.isNotEmpty() && isSimilarToPrevious(parsed.response, recentAssistantReplies.takeLast(3))) {
                Log.w("DeepSeekRepository", "Response too similar to recent replies. Retrying with anti-repetition prompt...")
                effectiveSystemPrompt = buildSystemPrompt(
                    cefrLevel = cefrLevel,
                    userName = userName,
                    nativeLanguage = nativeLanguage,
                    scenarioContext = scenarioContext,
                    avoidReplies = recentAssistantReplies.takeLast(3),
                    userInterests = userInterests,
                    conversationSummary = conversationSummary,
                    learnedFacts = learnedFacts
                )
                normalizedMessages = formatAndNormalizeMessages(
                    systemPrompt = effectiveSystemPrompt,
                    history = chatHistory,
                    userInput = userInput
                )
                requestBody = ChatCompletionRequest(
                    model = effectiveModel,
                    messages = normalizedMessages,
                    temperature = 0.85, // Slightly higher temperature for variation
                    maxTokens = 600,
                    stream = false,
                    responseFormat = if (useJsonFormat) ResponseFormat("json_object") else null
                )
                response = apiService.createChatCompletion(
                    url = endpointUrl,
                    authorization = authHeader,
                    request = requestBody
                )
                if (response.isSuccessful) {
                    val retryContent = response.body()?.choices?.firstOrNull()?.message?.content
                    if (!retryContent.isNullOrBlank()) {
                        parsed = parseCoachResponse(retryContent, providerName)
                    }
                }
            }

            return parsed
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            Log.e("DeepSeekRepository", "API Exception", e)
            throw ApiException(e.message ?: "$providerName servisine bağlanırken beklenmeyen bir hata oluştu.")
        }
    }

    suspend fun translateText(
        text: String,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        modelName: String
    ): String {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return "Çeviri için API Anahtarı tanımlanmamış. Ayarlar menüsünden API Key ekleyebilirsiniz."
        }
        val provider = LlmProvider.fromBaseUrl(baseUrl)
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val endpointUrl = "$cleanBaseUrl/chat/completions"
        val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"
        val effectiveModel = migrateModelName(baseUrl, modelName)

        val prompt = "Translate the following English text to $targetLanguage. Output ONLY the natural $targetLanguage translation with no quotes or extra text:\n$text"
        val requestBody = ChatCompletionRequest(
            model = effectiveModel,
            messages = listOf(
                ChatMessageItem("system", "You are a professional translator. Translate English into $targetLanguage accurately and naturally."),
                ChatMessageItem("user", prompt)
            ),
            temperature = 0.2
        )

        return try {
            val response = apiService.createChatCompletion(
                url = endpointUrl,
                authorization = authHeader,
                request = requestBody
            )
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    content.trim().trim('"')
                } else {
                    "Çeviri yanıtı alınamadı."
                }
            } else {
                "Çeviri hatası (Kod: ${response.code()})"
            }
        } catch (e: Exception) {
            Log.e("DeepSeekRepository", "Translation error", e)
            "Çeviri yapılırken bir hata oluştu: ${e.message}"
        }
    }

    suspend fun lookupWordDetails(
        word: String,
        contextSentence: String?,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        modelName: String
    ): WordDefinition {
        val trimmedKey = apiKey.trim()
        val cleanWord = word.trim().trim('.', ',', '!', '?', '"', '\'')
        if (trimmedKey.isEmpty()) {
            return WordDefinition(
                word = cleanWord,
                meaning = "Anlam yüklenemedi (API Anahtarı eksik)",
                exampleSentence = "Example: I am practicing the word '$cleanWord'.",
                contextSentence = contextSentence
            )
        }
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val endpointUrl = "$cleanBaseUrl/chat/completions"
        val authHeader = if (trimmedKey.startsWith("Bearer ", ignoreCase = true)) trimmedKey else "Bearer $trimmedKey"
        val effectiveModel = migrateModelName(baseUrl, modelName)

        val systemPrompt = "You are an English language dictionary assistant. For a given English word, reply ONLY with a valid JSON format:\n{\n  \"word\": \"<the clean word>\",\n  \"meaning\": \"<accurate short definition/translation in $targetLanguage>\",\n  \"example\": \"<one natural English example sentence using the word>\"\n}"
        val userPrompt = "Word: \"$cleanWord\"\nContext Sentence: \"${contextSentence.orEmpty()}\""

        val requestBody = ChatCompletionRequest(
            model = effectiveModel,
            messages = listOf(
                ChatMessageItem("system", systemPrompt),
                ChatMessageItem("user", userPrompt)
            ),
            temperature = 0.3
        )

        return try {
            val response = apiService.createChatCompletion(
                url = endpointUrl,
                authorization = authHeader,
                request = requestBody
            )
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    val cleanJson = content.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                    val meaningMatch = Regex("\"meaning\"\\s*:\\s*\"(.*?)\"").find(cleanJson)
                    val exampleMatch = Regex("\"example\"\\s*:\\s*\"(.*?)\"").find(cleanJson)
                    val meaning = meaningMatch?.groupValues?.get(1)?.ifBlank { null } ?: "Tümcük anlamı"
                    val example = exampleMatch?.groupValues?.get(1)?.ifBlank { null } ?: "This is an example sentence with $cleanWord."
                    WordDefinition(
                        word = cleanWord,
                        meaning = meaning,
                        exampleSentence = example,
                        contextSentence = contextSentence
                    )
                } else {
                    WordDefinition(cleanWord, "Anlam bulunamadı", "Example sentence with $cleanWord.", contextSentence)
                }
            } else {
                WordDefinition(cleanWord, "Çeviri servisi yanıt vermedi", "Example sentence with $cleanWord.", contextSentence)
            }
        } catch (e: Exception) {
            Log.e("DeepSeekRepository", "Word lookup error", e)
            WordDefinition(cleanWord, "Arama hatası", "Example sentence with $cleanWord.", contextSentence)
        }
    }

    suspend fun testApiConnection(
        apiKey: String,
        baseUrl: String,
        modelName: String
    ): Result<String> {
        val trimmedKey = apiKey.trim()
        val provider = LlmProvider.fromBaseUrl(baseUrl)
        val providerName = provider.displayName

        if (trimmedKey.isEmpty()) {
            return Result.failure(ApiException("$providerName API anahtarı gerekli"))
        }

        return try {
            val response = getCoachResponse(
                userInput = "Hello! Please test the connection.",
                apiKey = trimmedKey,
                baseUrl = baseUrl,
                modelName = modelName,
                cefrLevel = "CEFR B1-B2",
                scenarioContext = null,
                chatHistory = emptyList()
            )
            if (response.response.isNotBlank()) {
                val effectiveModel = migrateModelName(baseUrl, modelName)
                Result.success(effectiveModel)
            } else {
                Result.failure(ApiException("Bağlantı testi başarısız: Boş cevap alındı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCoachResponse(rawContent: String, providerName: String): CoachJsonResponse {
        val cleaned = cleanModelContent(rawContent)
        if (cleaned.isBlank()) {
            throw ApiException("$providerName cevabı boş döndü.")
        }

        // 1. Try Moshi adapter
        val parsedMoshi = try {
            coachAdapter.fromJson(cleaned)
        } catch (e: Exception) {
            null
        }

        if (parsedMoshi != null && parsedMoshi.response.isNotBlank()) {
            return parsedMoshi
        }

        // 2. Try Regex matching JSON object {...}
        val jsonMatch = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(cleaned)?.value
        if (jsonMatch != null) {
            val parsedRegex = try {
                coachAdapter.fromJson(jsonMatch)
            } catch (e: Exception) {
                null
            }
            if (parsedRegex != null && parsedRegex.response.isNotBlank()) {
                return parsedRegex
            }
        }

        // 3. Some OpenAI-compatible endpoints return a truncated or double-encoded
        // JSON envelope even though the response field itself is complete. Recover the
        // field so transport formatting never leaks into the chat bubble.
        val recoveredResponse = extractJsonStringField(cleaned, "response")
        if (!recoveredResponse.isNullOrBlank()) {
            return CoachJsonResponse(
                feedback = extractJsonStringField(cleaned, "feedback"),
                response = recoveredResponse
            )
        }

        // 4. Plain text response fallback
        if (cleaned.isNotBlank()) {
            return CoachJsonResponse(
                feedback = null,
                response = cleaned
            )
        }

        throw ApiException("$providerName cevabı ayrıştırılamadı veya 'response' alanı boş.")
    }

    private fun extractJsonStringField(content: String, fieldName: String): String? {
        val match = Regex(
            "\"${Regex.escape(fieldName)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            RegexOption.DOT_MATCHES_ALL
        ).find(content) ?: return null

        val encodedJsonString = "\"${match.groupValues[1]}\""
        return try {
            stringAdapter.fromJson(encodedJsonString)?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            match.groupValues[1].trim().takeIf { it.isNotBlank() }
        }
    }

    private fun parseHttpError(providerName: String, code: Int): String {
        return when (code) {
            401 -> "$providerName isteği başarısız oldu (HTTP 401): API anahtarını kontrol edin."
            404 -> "$providerName isteği başarısız oldu (HTTP 404): Endpoint veya model bulunamadı."
            429 -> "$providerName isteği başarısız oldu (HTTP 429): API kota sınırı aşıldı."
            422 -> "$providerName isteği başarısız oldu (HTTP 422): Model adı veya istek parametreleri geçersiz."
            500, 502, 503, 504 -> "$providerName isteği başarısız oldu (HTTP $code): Sunucu hatası."
            else -> "$providerName isteği başarısız oldu (HTTP $code)."
        }
    }
}
