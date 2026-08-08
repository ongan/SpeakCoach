package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.math.max

@JsonClass(generateAdapter = true)
data class GoalUpdateJson(
    val goalId: String = "",
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED
    val evidence: String? = null
)

@JsonClass(generateAdapter = true)
data class CorrectionJson(
    val original: String = "",
    val corrected: String = "",
    val explanationNative: String = "",
    val severity: String = "IMPORTANT", // MINOR, IMPORTANT
    val category: String = "GRAMMAR" // GRAMMAR, VOCABULARY, NATURALNESS
)

@JsonClass(generateAdapter = true)
data class NextTurnPlanJson(
    val unmetGoalId: String? = null,
    val branchId: String? = null,
    val mustNotAsk: List<String> = emptyList(),
    val purpose: String = ""
)

@JsonClass(generateAdapter = true)
data class StructuredCoachTurnResponse(
    val coachReply: String = "",
    val coachIntent: String = "REACT", // REACT, ASK, INFORM, COMPLICATE, HELP, CLOSE
    val stage: String = "DISCOVERY",
    val goalUpdates: List<GoalUpdateJson> = emptyList(),
    val corrections: List<CorrectionJson> = emptyList(),
    val targetVocabularyUsed: List<String> = emptyList(),
    val learnedUserFacts: List<String> = emptyList(),
    val nextTurnPlan: NextTurnPlanJson? = null,
    val shouldCompleteScenario: Boolean = false,
    val completionReason: String? = null
)

object StructuredResponseParser {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(StructuredCoachTurnResponse::class.java)

    fun parseOrRepair(rawInput: String): StructuredCoachTurnResponse {
        val cleaned = rawInput.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 1. Direct try
        try {
            val parsed = adapter.fromJson(cleaned)
            if (parsed != null && parsed.coachReply.isNotBlank()) {
                return parsed
            }
        } catch (_: Exception) {}

        // 2. Safe Repair: Extract JSON substring between { and }
        try {
            val startIndex = cleaned.indexOf('{')
            val endIndex = cleaned.lastIndexOf('}')
            if (startIndex != -1 && endIndex > startIndex) {
                val jsonSub = cleaned.substring(startIndex, endIndex + 1)
                val repaired = adapter.fromJson(jsonSub)
                if (repaired != null && repaired.coachReply.isNotBlank()) {
                    return repaired
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: If rawInput is plain text, treat it as coachReply
        val fallbackText = if (cleaned.startsWith("{")) {
            "That sounds good! Could you tell me a little more about that?"
        } else {
            cleaned
        }

        return StructuredCoachTurnResponse(
            coachReply = fallbackText,
            coachIntent = "REACT",
            stage = "DISCOVERY"
        )
    }

    /**
     * Anti-repetition: Calculates Jaccard similarity between two text strings based on normalized word tokens.
     */
    fun calculateLexicalSimilarity(text1: String, text2: String): Float {
        val tokens1 = text1.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val tokens2 = text2.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0f

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size

        if (union == 0) return 0f
        return intersection.toFloat() / union.toFloat()
    }
}
