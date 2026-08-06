package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String = "deepseek-ai/deepseek-v4-flash",
    @Json(name = "messages") val messages: List<ChatMessageItem>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int? = 600,
    @Json(name = "stream") val stream: Boolean? = false,
    @Json(name = "response_format") val responseFormat: ResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type") val type: String = "json_object"
)

@JsonClass(generateAdapter = true)
data class ChatMessageItem(
    @Json(name = "role") val role: String, // "system", "user", "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<ChatChoice>?
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    @Json(name = "index") val index: Int?,
    @Json(name = "message") val message: ChatMessageItem?
)

// The required DeepSeek System Prompt JSON Output Schema:
// {
//   "feedback": "string or null",
//   "response": "string"
// }
@JsonClass(generateAdapter = true)
data class CoachJsonResponse(
    @Json(name = "feedback") val feedback: String? = null,
    @Json(name = "response") val response: String = "",
    @Json(name = "stage") val stage: String? = null,
    @Json(name = "completedGoalIds") val completedGoalIds: List<String>? = null,
    @Json(name = "learnedUserFacts") val learnedUserFacts: List<String>? = null,
    @Json(name = "shouldCompleteScenario") val shouldCompleteScenario: Boolean? = false
)
