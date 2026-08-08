package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Amber500

enum class ScenarioStage(val displayName: String, val turkishName: String) {
    OPENING("Opening", "1. Açılış & Giriş"),
    DISCOVERY("Discovery", "2. Detaylandırma & Keşif"),
    COMPLICATION("Complication", "3. Sürpriz & Zorluk"),
    RESOLUTION("Resolution", "4. Çözüm & Anlaşma"),
    REVIEW("Review", "5. Kapanış & Değerlendirme")
}

enum class CoachGender(
    val id: String,
    val coachName: String,
    val turkishTitle: String,
    val avatarEmoji: String,
    val preferredVoicePitch: Float,
    val preferredVoiceRate: Float,
    val themeColor: Color
) {
    MAYA(
        id = "FEMALE",
        coachName = "Maya",
        turkishTitle = "Empatik & Neşeli İngilizce Koçu",
        avatarEmoji = "👩‍🏫",
        preferredVoicePitch = 1.1f,
        preferredVoiceRate = 0.95f,
        themeColor = Amber500
    ),
    LEO(
        id = "MALE",
        coachName = "Leo",
        turkishTitle = "Dinamik & Özgüvenli İngilizce Koçu",
        avatarEmoji = "👨‍🏫",
        preferredVoicePitch = 0.9f,
        preferredVoiceRate = 1.0f,
        themeColor = Cyan600
    )
}

data class ScenarioGoal(
    val id: String,
    val description: String,
    val successCriteria: String = "",
    val required: Boolean = true,
    val weight: Int = 1
)

data class ScenarioBranch(
    val id: String,
    val stage: ScenarioStage = ScenarioStage.COMPLICATION,
    val triggerCondition: String,
    val complicationDescription: String,
    val allowedOnce: Boolean = true
)

data class ScenarioDefinition(
    val id: String,
    val chapterId: String? = null,
    val sceneIndex: Int = 1,
    val title: String,
    val description: String,
    val cefrLevel: String,
    val location: String,
    val aiRole: String,
    val aiPersona: String = "Warm and encouraging English coach",
    val userRole: String,
    val mainGoal: String,
    val goals: List<ScenarioGoal> = emptyList(),
    val subGoals: List<String> = emptyList(), // Backwards compatibility helper
    val targetVocabulary: List<String>,
    val grammarFocus: String,
    val variableOptions: Map<String, List<String>> = emptyMap(),
    val openerPool: List<String> = emptyList(),
    val branchPool: List<ScenarioBranch> = emptyList(),
    val successConditions: List<String> = emptyList(),
    val minTurns: Int = 4,
    val maxTurns: Int = 10,
    val closingRules: String = "Wrap up gracefully when main goals are completed or max turns reached.",
    val starterPrompt: String
) {
    fun getResolvedGoals(): List<ScenarioGoal> {
        if (goals.isNotEmpty()) return goals
        return subGoals.mapIndexed { index, sub ->
            ScenarioGoal(
                id = "${id}_goal_${index + 1}",
                description = sub,
                successCriteria = sub,
                required = true
            )
        }
    }
}

data class ScenarioSessionState(
    val sessionId: String,
    val scenarioId: String,
    val currentStage: ScenarioStage = ScenarioStage.OPENING,
    val completedGoalIds: List<String> = emptyList(),
    val discoveredFacts: List<String> = emptyList(),
    val sessionVariables: Map<String, String> = emptyMap(),
    val turnCount: Int = 0,
    val usedCoachQuestions: List<String> = emptyList(),
    val recentAssistantReplies: List<String> = emptyList(),
    val userPerformanceScore: Int = 100,
    val isCompleted: Boolean = false,
    val finalFeedbackSummary: String? = null
)

data class StoryScene(
    val id: String,
    val chapterId: String,
    val sceneNumber: Int,
    val rawTitle: String,
    val description: String,
    val cefrLevel: String,
    val starterPromptTemplate: String,
    val aiRole: String,
    val targetVocabulary: List<String> = emptyList(),
    val variableOptions: Map<String, List<String>> = emptyMap()
) {
    fun getTitle(userName: String): String {
        val name = if (userName.isNotBlank()) userName else "Deniz"
        return rawTitle.replace("{name}", name)
    }

    fun getStarterPrompt(userName: String): String {
        val name = if (userName.isNotBlank()) userName else "Deniz"
        return starterPromptTemplate.replace("{name}", name)
    }
}

data class StoryChapter(
    val id: String,
    val chapterNumber: Int,
    val rawTitle: String,
    val description: String,
    val cefrLevel: String,
    val scenes: List<StoryScene>
) {
    fun getTitle(userName: String): String {
        val name = if (userName.isNotBlank()) userName else "Deniz"
        return rawTitle.replace("{name}", name)
    }
}

data class ExtraStudyModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val cefrLevel: String,
    val description: String,
    val targetVocabulary: List<String>,
    val starterPrompt: String
)
