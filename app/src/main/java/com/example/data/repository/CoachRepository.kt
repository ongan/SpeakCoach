package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ChatMessageItem
import com.example.data.api.CoachJsonResponse
import com.example.data.api.DeepSeekRepository
import com.example.data.api.LlmProvider
import com.example.data.api.WordDefinition
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.GrammarTipEntity
import com.example.data.local.SavedWordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class CoachRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.coachDao()
    private val deepSeekRepository = DeepSeekRepository()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("speakcoach_prefs", Context.MODE_PRIVATE)

    // Provider state
    private val savedProviderName = prefs.getString("selected_provider", LlmProvider.NVIDIA.name) ?: LlmProvider.NVIDIA.name
    private val initialProvider = try {
        LlmProvider.valueOf(savedProviderName)
    } catch (e: Exception) {
        LlmProvider.NVIDIA
    }

    private val _selectedProvider = MutableStateFlow(initialProvider)
    val selectedProvider: StateFlow<LlmProvider> = _selectedProvider

    // API Key State
    private val _apiKey = MutableStateFlow(getApiKeyForProvider(initialProvider))
    val apiKey: StateFlow<String> = _apiKey

    // Base URL State
    private val _baseUrl = MutableStateFlow(getBaseUrlForProvider(initialProvider))
    val baseUrl: StateFlow<String> = _baseUrl

    // Model Name State
    private val _modelName = MutableStateFlow(getModelForProvider(initialProvider))
    val modelName: StateFlow<String> = _modelName

    private val _cefrLevel = MutableStateFlow(prefs.getString("cefr_level", "CEFR B1-B2") ?: "CEFR B1-B2")
    val cefrLevel: StateFlow<String> = _cefrLevel

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName

    private val _nativeLanguage = MutableStateFlow(prefs.getString("native_language", "Türkçe") ?: "Türkçe")
    val nativeLanguage: StateFlow<String> = _nativeLanguage

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("is_onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _autoPlayTts = MutableStateFlow(prefs.getBoolean("auto_play_tts", true))
    val autoPlayTts: StateFlow<Boolean> = _autoPlayTts

    private val _useEdgeNeuralTts = MutableStateFlow(prefs.getBoolean("use_edge_neural_tts", true))
    val useEdgeNeuralTts: StateFlow<Boolean> = _useEdgeNeuralTts

    private val _femaleEdgeVoice = MutableStateFlow(prefs.getString("female_edge_voice", "en-US-AvaNeural") ?: "en-US-AvaNeural")
    val femaleEdgeVoice: StateFlow<String> = _femaleEdgeVoice

    private val _maleEdgeVoice = MutableStateFlow(prefs.getString("male_edge_voice", "en-US-AndrewNeural") ?: "en-US-AndrewNeural")
    val maleEdgeVoice: StateFlow<String> = _maleEdgeVoice

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.0f))
    val speechRate: StateFlow<Float> = _speechRate

    // Coach Selection & View Modes
    private val savedCoachGender = prefs.getString("coach_gender", com.example.data.model.CoachGender.MAYA.id) ?: com.example.data.model.CoachGender.MAYA.id
    private val initialCoachGender = if (savedCoachGender == com.example.data.model.CoachGender.LEO.id) com.example.data.model.CoachGender.LEO else com.example.data.model.CoachGender.MAYA

    private val _selectedCoachGender = MutableStateFlow(initialCoachGender)
    val selectedCoachGender: StateFlow<com.example.data.model.CoachGender> = _selectedCoachGender

    private val _showCoachAvatar = MutableStateFlow(prefs.getBoolean("show_coach_avatar", true))
    val showCoachAvatar: StateFlow<Boolean> = _showCoachAvatar

    private val _showChatBubbles = MutableStateFlow(prefs.getBoolean("show_chat_bubbles", true))
    val showChatBubbles: StateFlow<Boolean> = _showChatBubbles

    private val _unlockAllStoryChapters = MutableStateFlow(prefs.getBoolean("unlock_all_story_chapters", false))
    val unlockAllStoryChapters: StateFlow<Boolean> = _unlockAllStoryChapters

    private val _completedSceneIds = MutableStateFlow(prefs.getStringSet("completed_scene_ids", emptySet()) ?: emptySet())
    val completedSceneIds: StateFlow<Set<String>> = _completedSceneIds

    // Scenario Engine Session State
    private val _activeScenarioSession = MutableStateFlow<com.example.data.model.ScenarioSessionState?>(null)
    val activeScenarioSession: StateFlow<com.example.data.model.ScenarioSessionState?> = _activeScenarioSession

    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()
    val allGrammarTips: Flow<List<GrammarTipEntity>> = dao.getAllGrammarTips()
    val allSavedWords: Flow<List<SavedWordEntity>> = dao.getAllSavedWords()
    val userMessageCount: Flow<Int> = dao.getUserMessageCount()
    val grammarTipCount: Flow<Int> = dao.getGrammarTipCount()
    val savedWordCount: Flow<Int> = dao.getSavedWordCount()

    init {
        // Sync legacy api_key into nvidia if empty
        val legacyKey = prefs.getString("api_key", "") ?: ""
        if (legacyKey.isNotBlank() && prefs.getString(getPrefKeyForApiKey(LlmProvider.NVIDIA), "").isNullOrBlank()) {
            prefs.edit().putString(getPrefKeyForApiKey(LlmProvider.NVIDIA), legacyKey).apply()
        }
    }

    fun getApiKeyForProvider(provider: LlmProvider): String {
        val prefKey = getPrefKeyForApiKey(provider)
        val saved = prefs.getString(prefKey, "") ?: ""
        if (saved.isBlank() && provider == LlmProvider.NVIDIA) {
            return prefs.getString("api_key", "") ?: ""
        }
        return saved
    }

    fun getBaseUrlForProvider(provider: LlmProvider): String {
        return prefs.getString("base_url_${provider.name}", provider.defaultBaseUrl) ?: provider.defaultBaseUrl
    }

    fun getModelForProvider(provider: LlmProvider): String {
        val saved = prefs.getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel
        return DeepSeekRepository.migrateModelName(getBaseUrlForProvider(provider), saved)
    }

    private fun getPrefKeyForApiKey(provider: LlmProvider): String {
        return "api_key_${provider.name}"
    }

    fun selectProvider(provider: LlmProvider) {
        prefs.edit().putString("selected_provider", provider.name).apply()
        _selectedProvider.value = provider

        val providerKey = getApiKeyForProvider(provider)
        val providerBaseUrl = getBaseUrlForProvider(provider)
        val providerModel = getModelForProvider(provider)

        _apiKey.value = providerKey
        _baseUrl.value = providerBaseUrl
        _modelName.value = providerModel
    }

    fun updateApiKey(key: String) {
        val trimmed = key.trim()
        val currentProvider = _selectedProvider.value
        prefs.edit().putString(getPrefKeyForApiKey(currentProvider), trimmed).apply()
        prefs.edit().putString("api_key", trimmed).apply()
        _apiKey.value = trimmed
    }

    fun updateBaseUrl(url: String) {
        val trimmed = url.trim()
        val currentProvider = _selectedProvider.value
        prefs.edit().putString("base_url_${currentProvider.name}", trimmed).apply()
        prefs.edit().putString("base_url", trimmed).apply()
        _baseUrl.value = trimmed

        val migratedModel = DeepSeekRepository.migrateModelName(trimmed, _modelName.value)
        if (migratedModel != _modelName.value) {
            updateModelName(migratedModel)
        }
    }

    fun updateModelName(model: String) {
        val trimmed = model.trim()
        val currentProvider = _selectedProvider.value
        val effectiveModel = DeepSeekRepository.migrateModelName(_baseUrl.value, trimmed)
        prefs.edit().putString("model_${currentProvider.name}", effectiveModel).apply()
        prefs.edit().putString("model_name", effectiveModel).apply()
        _modelName.value = effectiveModel
    }

    fun updateCefrLevel(level: String) {
        prefs.edit().putString("cefr_level", level).apply()
        _cefrLevel.value = level
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString("user_name", trimmed).apply()
        _userName.value = trimmed
    }

    fun updateNativeLanguage(language: String) {
        val trimmed = language.trim()
        prefs.edit().putString("native_language", trimmed).apply()
        _nativeLanguage.value = trimmed
    }

    fun completeOnboarding(name: String, nativeLang: String, level: String) {
        updateUserName(name)
        updateNativeLanguage(nativeLang)
        updateCefrLevel(level)
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun updateAutoPlayTts(enabled: Boolean) {
        prefs.edit().putBoolean("auto_play_tts", enabled).apply()
        _autoPlayTts.value = enabled
    }

    fun updateUseEdgeNeuralTts(useEdge: Boolean) {
        prefs.edit().putBoolean("use_edge_neural_tts", useEdge).apply()
        _useEdgeNeuralTts.value = useEdge
    }

    fun updateFemaleEdgeVoice(voice: String) {
        prefs.edit().putString("female_edge_voice", voice).apply()
        _femaleEdgeVoice.value = voice
    }

    fun updateMaleEdgeVoice(voice: String) {
        prefs.edit().putString("male_edge_voice", voice).apply()
        _maleEdgeVoice.value = voice
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat("speech_rate", rate).apply()
        _speechRate.value = rate
    }

    fun updateCoachGender(gender: com.example.data.model.CoachGender) {
        prefs.edit().putString("coach_gender", gender.id).apply()
        _selectedCoachGender.value = gender
    }

    fun updateViewModes(showAvatar: Boolean, showBubbles: Boolean) {
        // Enforcement Rule: Avatar and Chat Bubbles CANNOT both be hidden simultaneously
        var finalAvatar = showAvatar
        var finalBubbles = showBubbles

        if (!finalAvatar && !finalBubbles) {
            // Keep bubbles on if user tries to turn off both
            finalBubbles = true
        }

        prefs.edit().putBoolean("show_coach_avatar", finalAvatar).apply()
        prefs.edit().putBoolean("show_chat_bubbles", finalBubbles).apply()

        _showCoachAvatar.value = finalAvatar
        _showChatBubbles.value = finalBubbles
    }

    fun updateUnlockAllStoryChapters(unlockAll: Boolean) {
        prefs.edit().putBoolean("unlock_all_story_chapters", unlockAll).apply()
        _unlockAllStoryChapters.value = unlockAll
    }

    fun markSceneCompleted(sceneId: String) {
        val updated = _completedSceneIds.value.toMutableSet()
        updated.add(sceneId)
        prefs.edit().putStringSet("completed_scene_ids", updated).apply()
        _completedSceneIds.value = updated
    }

    fun startScenarioSession(definition: com.example.data.model.ScenarioDefinition): com.example.data.model.ScenarioSessionState {
        val randomVariables = mutableMapOf<String, String>()
        definition.variableOptions.forEach { (key, options) ->
            if (options.isNotEmpty()) {
                randomVariables[key] = options.random()
            }
        }

        val session = com.example.data.model.ScenarioSessionState(
            sessionId = "session_${System.currentTimeMillis()}",
            scenarioId = definition.id,
            currentStage = com.example.data.model.ScenarioStage.OPENING,
            sessionVariables = randomVariables
        )
        _activeScenarioSession.value = session
        return session
    }

    fun startStorySceneSession(scene: com.example.data.model.StoryScene, userName: String): com.example.data.model.ScenarioSessionState {
        val randomVariables = mutableMapOf<String, String>()
        scene.variableOptions.forEach { (key, options) ->
            if (options.isNotEmpty()) {
                randomVariables[key] = options.random()
            }
        }

        val session = com.example.data.model.ScenarioSessionState(
            sessionId = "session_story_${scene.id}_${System.currentTimeMillis()}",
            scenarioId = scene.id,
            currentStage = com.example.data.model.ScenarioStage.OPENING,
            sessionVariables = randomVariables
        )
        _activeScenarioSession.value = session
        return session
    }

    fun updateActiveScenarioSession(newState: com.example.data.model.ScenarioSessionState) {
        _activeScenarioSession.value = newState
    }

    fun endCurrentScenarioSession() {
        _activeScenarioSession.value = null
    }

    suspend fun saveUserMessage(text: String, isVoice: Boolean, scenario: String?): ChatMessageEntity {
        val entity = ChatMessageEntity(
            sender = "USER",
            text = text,
            isVoice = isVoice,
            scenario = scenario
        )
        val id = dao.insertMessage(entity)
        return entity.copy(id = id)
    }

    suspend fun sendUserMessageAndGetCoachReply(
        userInput: String,
        isVoice: Boolean,
        activeScenario: String?,
        history: List<ChatMessageEntity>
    ): ChatMessageEntity {
        saveUserMessage(userInput, isVoice, activeScenario)

        val filteredHistory = history.filter { msg ->
            if (activeScenario.isNullOrBlank()) {
                msg.scenario.isNullOrBlank()
            } else {
                msg.scenario == activeScenario
            }
        }

        val apiHistory = filteredHistory.map { msg ->
            ChatMessageItem(
                role = if (msg.sender == "USER") "user" else "assistant",
                content = msg.text
            )
        }

        val session = _activeScenarioSession.value
        val recentReplies = session?.recentAssistantReplies ?: filteredHistory
            .filter { it.sender == "COACH" }
            .takeLast(3)
            .map { it.text }

        // Construct rich scenario context string if session active
        var effectiveScenarioContext = if (session != null) {
            val varsDesc = session.sessionVariables.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "Scenario ID: ${session.scenarioId}\n" +
                    "Current Stage: ${session.currentStage.displayName}\n" +
                    "Session Variables: [$varsDesc]\n" +
                    "Turn Count: ${session.turnCount + 1}\n" +
                    if (activeScenario != null) "Scenario Overview: $activeScenario" else ""
        } else {
            activeScenario
        }

        val isSavedWordsPractice = session?.scenarioId == "mod_saved_words_practice" || activeScenario?.contains("mod_saved_words_practice") == true
        if (isSavedWordsPractice) {
            val savedList = try { dao.getAllSavedWords().first() } catch (e: Exception) { emptyList() }
            if (savedList.isNotEmpty()) {
                val wordsStr = savedList.joinToString(", ") { "${it.word} (${it.meaning})" }
                effectiveScenarioContext = (effectiveScenarioContext.orEmpty()) +
                        "\nSPECIAL INSTRUCTION: This practice session focuses on the user's saved vocabulary: [$wordsStr]. Naturally incorporate these words into your questions and dialogue, and praise the user when they use them."
            }
        }

        val coachOutput: CoachJsonResponse = deepSeekRepository.getCoachResponse(
            userInput = userInput,
            apiKey = _apiKey.value,
            baseUrl = _baseUrl.value,
            modelName = _modelName.value,
            cefrLevel = _cefrLevel.value,
            userName = _userName.value,
            nativeLanguage = _nativeLanguage.value,
            scenarioContext = effectiveScenarioContext,
            chatHistory = apiHistory,
            recentAssistantReplies = recentReplies
        )

        // Update session state if present
        if (session != null) {
            val updatedStage = try {
                if (!coachOutput.stage.isNullOrBlank()) {
                    com.example.data.model.ScenarioStage.valueOf(coachOutput.stage.uppercase())
                } else session.currentStage
            } catch (e: Exception) {
                session.currentStage
            }

            val updatedGoals = (session.completedGoalIds + (coachOutput.completedGoalIds ?: emptyList())).distinct()
            val updatedFacts = (session.discoveredFacts + (coachOutput.learnedUserFacts ?: emptyList())).distinct()
            val updatedReplies = (session.recentAssistantReplies + coachOutput.response).takeLast(5)
            val isComplete = (coachOutput.shouldCompleteScenario == true) || (session.turnCount + 1 >= 10)

            _activeScenarioSession.value = session.copy(
                currentStage = updatedStage,
                completedGoalIds = updatedGoals,
                discoveredFacts = updatedFacts,
                turnCount = session.turnCount + 1,
                recentAssistantReplies = updatedReplies,
                isCompleted = isComplete
            )
        }

        val coachMessageEntity = ChatMessageEntity(
            sender = "COACH",
            text = coachOutput.response,
            feedback = coachOutput.feedback,
            scenario = activeScenario
        )
        val coachMsgId = dao.insertMessage(coachMessageEntity)

        if (!coachOutput.feedback.isNullOrBlank()) {
            val tip = parseFeedbackToGrammarTip(userInput, coachOutput.feedback)
            dao.insertGrammarTip(tip)
        }

        return coachMessageEntity.copy(id = coachMsgId)
    }

    suspend fun testApiConnection(
        testKey: String = _apiKey.value,
        testUrl: String = _baseUrl.value,
        testModel: String = _modelName.value
    ): Result<String> {
        return deepSeekRepository.testApiConnection(
            apiKey = testKey,
            baseUrl = testUrl,
            modelName = testModel
        )
    }

    private fun parseFeedbackToGrammarTip(original: String, feedback: String): GrammarTipEntity {
        var corrected = original
        var explanation = feedback

        if (feedback.contains("Correction:", ignoreCase = true)) {
            val parts = feedback.split("Explanation:", ignoreCase = true)
            val corrPart = parts[0].replace("Correction:", "", ignoreCase = true).trim()
            if (corrPart.isNotEmpty()) {
                corrected = corrPart
            }
            if (parts.size > 1) {
                explanation = parts[1].trim()
            }
        }

        return GrammarTipEntity(
            originalSentence = original,
            correctedSentence = corrected,
            explanation = explanation
        )
    }

    suspend fun bookmarkGrammarTip(tip: GrammarTipEntity) {
        dao.insertGrammarTip(tip)
    }

    suspend fun toggleGrammarTipMastered(tip: GrammarTipEntity) {
        dao.updateGrammarTip(tip.copy(isMastered = !tip.isMastered))
    }

    suspend fun deleteGrammarTip(tipId: Long) {
        dao.deleteGrammarTip(tipId)
    }

    suspend fun clearHistory() {
        dao.clearChatHistory()
    }

    suspend fun translateText(text: String, targetLanguage: String): String {
        return deepSeekRepository.translateText(
            text = text,
            targetLanguage = targetLanguage,
            apiKey = _apiKey.value,
            baseUrl = _baseUrl.value,
            modelName = _modelName.value
        )
    }

    suspend fun saveWord(word: String, meaning: String, exampleSentence: String, contextSentence: String?): Long {
        val entity = SavedWordEntity(
            word = word.trim().lowercase(),
            meaning = meaning,
            exampleSentence = exampleSentence,
            contextSentence = contextSentence
        )
        return dao.insertSavedWord(entity)
    }

    suspend fun toggleSavedWordMastered(word: SavedWordEntity) {
        dao.updateSavedWord(word.copy(isMastered = !word.isMastered))
    }

    suspend fun deleteSavedWord(id: Long) {
        dao.deleteSavedWord(id)
    }

    suspend fun lookupWordDetails(word: String, contextSentence: String?): WordDefinition {
        return deepSeekRepository.lookupWordDetails(
            word = word,
            contextSentence = contextSentence,
            targetLanguage = _nativeLanguage.value.ifBlank { "Türkçe" },
            apiKey = _apiKey.value,
            baseUrl = _baseUrl.value,
            modelName = _modelName.value
        )
    }
}
