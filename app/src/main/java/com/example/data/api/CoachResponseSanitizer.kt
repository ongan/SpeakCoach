package com.example.data.api

/**
 * Converts an LLM transport response into text that is safe to display and speak.
 *
 * Some OpenAI-compatible endpoints occasionally return an incomplete JSON envelope
 * even though its `response` string is complete. Keeping this in one shared utility
 * prevents JSON field names from leaking into Room, chat bubbles, or any TTS engine.
 */
object CoachResponseSanitizer {
    private val completedResponseField = Regex(
        "\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val incompleteResponseField = Regex(
        "\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val responsePrefix = Regex(
        "^\\s*(?:assistant\\s+)?response\\s*[:=-]\\s*",
        RegexOption.IGNORE_CASE
    )

    fun sanitize(text: String): String {
        val original = text.trim()
        if (original.isBlank()) return ""

        var candidate = original
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
            .replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        // Handle a JSON object that was itself serialized as a JSON string.
        repeat(2) {
            if (!candidate.contains("\"response\"", ignoreCase = true) &&
                candidate.contains("\\\"response\\\"", ignoreCase = true)
            ) {
                candidate = decodeJsonEscapes(candidate)
            }

            if (candidate.length >= 2 && candidate.first() == '"' && candidate.last() == '"') {
                val decoded = decodeJsonEscapes(candidate.substring(1, candidate.lastIndex))
                if (decoded.contains("response", ignoreCase = true)) candidate = decoded
            }
        }

        val encodedResponse = completedResponseField.find(candidate)?.groupValues?.getOrNull(1)
            ?: incompleteResponseField.find(candidate)?.groupValues?.getOrNull(1)

        if (!encodedResponse.isNullOrBlank()) {
            return decodeJsonEscapes(encodedResponse).trim().ifBlank { original }
        }

        return responsePrefix.replace(candidate, "").trim().ifBlank { original }
    }

    private fun decodeJsonEscapes(value: String): String {
        val output = StringBuilder(value.length)
        var index = 0

        while (index < value.length) {
            val current = value[index]
            if (current != '\\' || index + 1 >= value.length) {
                output.append(current)
                index++
                continue
            }

            when (val escaped = value[index + 1]) {
                '"' -> output.append('"')
                '\\' -> output.append('\\')
                '/' -> output.append('/')
                'b' -> output.append('\b')
                'f' -> output.append('\u000C')
                'n' -> output.append('\n')
                'r' -> output.append('\r')
                't' -> output.append('\t')
                'u' -> {
                    val end = index + 6
                    val codePoint = if (end <= value.length) {
                        value.substring(index + 2, end).toIntOrNull(16)
                    } else {
                        null
                    }
                    if (codePoint != null) {
                        output.append(codePoint.toChar())
                        index += 4
                    } else {
                        output.append('\\').append(escaped)
                    }
                }
                else -> output.append(escaped)
            }
            index += 2
        }

        return output.toString()
    }
}
