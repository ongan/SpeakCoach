package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechRecognizerManager
import com.example.audio.SpeechState
import com.example.audio.TextToSpeechManager
import com.example.audio.TtsEngineMode
import com.example.audio.TtsProvider
import com.example.audio.TtsStatus
import com.example.audio.TtsTestResult
import com.example.data.api.DeepSeekRepository
import com.example.data.api.LlmProvider
import com.example.data.api.WordDefinition
import com.example.data.local.ChatMessageEntity
import com.example.data.local.GrammarTipEntity
import com.example.data.local.SavedWordEntity
import com.example.data.repository.CoachRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    data class Success(val providerName: String, val modelName: String) : ConnectionTestState()
    data class Error(val providerName: String, val message: String) : ConnectionTestState()
}

data class CoachUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val grammarTips: List<GrammarTipEntity> = emptyList(),
    val isLoadingAi: Boolean = false,
    val activeScenario: String? = null,
    val userMessageCount: Int = 0,
    val grammarTipCount: Int = 0,
    val selectedProvider: LlmProvider = LlmProvider.CEREBRAS,
    val apiKey: String = "",
    val baseUrl: String = DeepSeekRepository.DEFAULT_CEREBRAS_BASE_URL,
    val modelName: String = DeepSeekRepository.DEFAULT_CEREBRAS_MODEL,
    val cefrLevel: String = "CEFR B1-B2",
    val userName: String = "",
    val nativeLanguage: String = "Türkçe",
    val isOnboardingCompleted: Boolean = false,
    val autoPlayTts: Boolean = true,
    val ttsEngineMode: TtsEngineMode = TtsEngineMode.KOKORO_OFFLINE,
    val fallbackEngineOption: com.example.audio.FallbackEngineOption = com.example.audio.FallbackEngineOption.ANDROID_SYSTEM,
    val allowAutoAndroidFallback: Boolean = true,
    val azureSpeechKey: String = "",
    val azureSpeechRegion: String = "eastus",
    val femaleEdgeVoice: String = "en-US-AvaNeural",
    val maleEdgeVoice: String = "en-US-AndrewNeural",
    val kokoroFemaleVoice: String = "af_heart",
    val kokoroMaleVoice: String = "am_michael",
    val kokoroModelDiskSizeMb: Float = 0f,
    val kokoroCacheSizeMb: Float = 0f,
    val speechRate: Float = 1.0f,
    val selectedCoachGender: com.example.data.model.CoachGender = com.example.data.model.CoachGender.MAYA,
    val showCoachAvatar: Boolean = true,
    val showChatBubbles: Boolean = true,
    val unlockAllStoryChapters: Boolean = false,
    val completedSceneIds: Set<String> = emptySet(),
    val activeScenarioSession: com.example.data.model.ScenarioSessionState? = null,
    val errorMessage: String? = null,
    val testState: ConnectionTestState = ConnectionTestState.Idle,
    val ttsStatus: TtsStatus = TtsStatus.Idle,
    val ttsTestResult: TtsTestResult? = null,
    val isTestingTts: Boolean = false,
    val useEdgeNeuralTts: Boolean = true,
    val userMemory: com.example.data.local.UserMemoryEntity? = null
)

class CoachViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CoachRepository(application)
    val ttsManager = TextToSpeechManager(application)
    val speechManager = SpeechRecognizerManager(application)

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _activeScenario = MutableStateFlow<String?>(null)
    val activeScenario: StateFlow<String?> = _activeScenario.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    private val _ttsTestResult = MutableStateFlow<TtsTestResult?>(null)
    val ttsTestResult: StateFlow<TtsTestResult?> = _ttsTestResult.asStateFlow()

    private val _isTestingTts = MutableStateFlow(false)
    val isTestingTts: StateFlow<Boolean> = _isTestingTts.asStateFlow()

    val messagesState: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grammarTipsState: StateFlow<List<GrammarTipEntity>> = repository.allGrammarTips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedWordsState: StateFlow<List<SavedWordEntity>> = repository.allSavedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWordDefinition = MutableStateFlow<WordDefinition?>(null)
    val selectedWordDefinition: StateFlow<WordDefinition?> = _selectedWordDefinition.asStateFlow()

    private val _isLookingUpWord = MutableStateFlow(false)
    val isLookingUpWord: StateFlow<Boolean> = _isLookingUpWord.asStateFlow()

    private val _wordSaveMessage = MutableStateFlow<String?>(null)
    val wordSaveMessage: StateFlow<String?> = _wordSaveMessage.asStateFlow()

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val speechState: StateFlow<SpeechState> = speechManager.speechState
    val partialSpeechText: StateFlow<String> = speechManager.partialText
    val rmsDb: StateFlow<Float> = speechManager.rmsDb
    val speechError: StateFlow<String?> = speechManager.errorState

    val isPlayingTts: StateFlow<Boolean> = ttsManager.isPlaying
    val currentPlayingMsgId: StateFlow<Long?> = ttsManager.currentPlayingMessageId

    private val _translations = MutableStateFlow<Map<Long, String>>(emptyMap())
    val translations: StateFlow<Map<Long, String>> = _translations.asStateFlow()

    private val _translatingIds = MutableStateFlow<Set<Long>>(emptySet())
    val translatingIds: StateFlow<Set<Long>> = _translatingIds.asStateFlow()

    val uiState: StateFlow<CoachUiState> = combine(
        combine(
            repository.allMessages,
            repository.allGrammarTips,
            _isLoadingAi,
            _activeScenario,
            repository.selectedProvider,
            repository.apiKey,
            repository.baseUrl,
            repository.modelName,
            repository.cefrLevel,
            repository.userName,
            repository.nativeLanguage,
            repository.isOnboardingCompleted,
            repository.autoPlayTts
        ) { args -> args },
        combine(
            repository.ttsEngineMode,
            repository.fallbackEngineOption,
            repository.allowAutoAndroidFallback,
            repository.kokoroFemaleVoice,
            repository.kokoroMaleVoice,
            repository.femaleEdgeVoice,
            repository.maleEdgeVoice,
            repository.speechRate,
            repository.selectedCoachGender,
            repository.showCoachAvatar,
            repository.showChatBubbles,
            repository.unlockAllStoryChapters,
            repository.completedSceneIds
        ) { args -> args },
        combine(
            repository.activeScenarioSession,
            _errorMessage,
            _testState,
            ttsManager.ttsStatus,
            _ttsTestResult,
            _isTestingTts,
            repository.useEdgeNeuralTts,
            repository.userMemory
        ) { args -> args }
    ) { group1, group2, group3 ->
        CoachUiState(
            messages = group1[0] as List<ChatMessageEntity>,
            grammarTips = group1[1] as List<GrammarTipEntity>,
            isLoadingAi = group1[2] as Boolean,
            activeScenario = group1[3] as String?,
            selectedProvider = group1[4] as LlmProvider,
            apiKey = group1[5] as String,
            baseUrl = group1[6] as String,
            modelName = group1[7] as String,
            cefrLevel = group1[8] as String,
            userName = group1[9] as String,
            nativeLanguage = group1[10] as String,
            isOnboardingCompleted = group1[11] as Boolean,
            autoPlayTts = group1[12] as Boolean,
            ttsEngineMode = group2[0] as TtsEngineMode,
            fallbackEngineOption = group2[1] as com.example.audio.FallbackEngineOption,
            allowAutoAndroidFallback = group2[2] as Boolean,
            kokoroFemaleVoice = group2[3] as String,
            kokoroMaleVoice = group2[4] as String,
            femaleEdgeVoice = group2[5] as String,
            maleEdgeVoice = group2[6] as String,
            kokoroModelDiskSizeMb = ttsManager.kokoroModelManager.getModelDiskSizeMb(),
            kokoroCacheSizeMb = ttsManager.kokoroCacheManager.getCacheSizeMb(),
            speechRate = group2[7] as Float,
            selectedCoachGender = group2[8] as com.example.data.model.CoachGender,
            showCoachAvatar = group2[9] as Boolean,
            showChatBubbles = group2[10] as Boolean,
            unlockAllStoryChapters = group2[11] as Boolean,
            completedSceneIds = group2[12] as Set<String>,
            activeScenarioSession = group3[0] as com.example.data.model.ScenarioSessionState?,
            errorMessage = group3[1] as String?,
            testState = group3[2] as ConnectionTestState,
            ttsStatus = group3[3] as TtsStatus,
            ttsTestResult = group3[4] as TtsTestResult?,
            isTestingTts = group3[5] as Boolean,
            useEdgeNeuralTts = group3[6] as Boolean,
            userMemory = group3[7] as com.example.data.local.UserMemoryEntity?
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CoachUiState()
    )

    init {
        viewModelScope.launch {
            speechManager.recognizedTextEvent.collect { text ->
                if (text.isNotBlank()) {
                    sendMessage(text, isVoice = true)
                }
            }
        }
        viewModelScope.launch {
            repository.selectedCoachGender.collect { gender ->
                ttsManager.setCoachGender(gender)
            }
        }
        viewModelScope.launch {
            repository.ttsEngineMode.collect { mode ->
                ttsManager.setEngineMode(mode)
            }
        }
        viewModelScope.launch {
            repository.fallbackEngineOption.collect { option ->
                ttsManager.setFallbackEngineOption(option)
            }
        }
        viewModelScope.launch {
            combine(repository.kokoroFemaleVoice, repository.kokoroMaleVoice) { female, male ->
                female to male
            }.collect { (female, male) ->
                ttsManager.setKokoroVoices(female, male)
            }
        }
        viewModelScope.launch {
            combine(repository.femaleEdgeVoice, repository.maleEdgeVoice) { female, male ->
                female to male
            }.collect { (female, male) ->
                ttsManager.setEdgeVoices(female, male)
            }
        }
        viewModelScope.launch {
            repository.speechRate.collect { rate ->
                ttsManager.setSpeechRate(rate)
            }
        }
    }

    fun sendMessage(userText: String, isVoice: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isLoadingAi.value) return

        viewModelScope.launch {
            _isLoadingAi.value = true
            _errorMessage.value = null
            ttsManager.stop()

            try {
                val currentHistory = messagesState.value
                val coachMsg = repository.sendUserMessageAndGetCoachReply(
                    userInput = trimmed,
                    isVoice = isVoice,
                    activeScenario = _activeScenario.value,
                    history = currentHistory
                )

                if (repository.autoPlayTts.value) {
                    ttsManager.speak(coachMsg.text, coachMsg.id, uiState.value.selectedCoachGender)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "${uiState.value.selectedProvider.displayName} isteği başarısız oldu."
            } finally {
                _isLoadingAi.value = false
            }
        }
    }

    fun testApiConnection() {
        val current = uiState.value
        val providerName = current.selectedProvider.displayName

        viewModelScope.launch {
            _testState.value = ConnectionTestState.Testing
            val result = repository.testApiConnection(
                testKey = current.apiKey,
                testUrl = current.baseUrl,
                testModel = current.modelName
            )
            result.onSuccess { model ->
                _testState.value = ConnectionTestState.Success(providerName, model)
            }.onFailure { err ->
                _testState.value = ConnectionTestState.Error(providerName, err.message ?: "$providerName bağlantı testi başarısız oldu.")
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun startVoiceRecording() {
        ttsManager.stop()
        speechManager.startListening()
    }

    fun stopVoiceRecording() {
        speechManager.stopListening()
    }

    fun selectScenario(scenarioName: String?) {
        _activeScenario.value = scenarioName
        if (scenarioName.isNullOrBlank()) {
            repository.endCurrentScenarioSession()
        } else {
            val starterPrompt = getStarterPromptForScenario(scenarioName)
            sendMessage("Let's practice $scenarioName! $starterPrompt", isVoice = false)
        }
    }

    fun startScenario(definition: com.example.data.model.ScenarioDefinition) {
        val session = repository.startScenarioSession(definition)
        _activeScenario.value = definition.title
        val varsDesc = session.sessionVariables.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        val prompt = "Let's start the scenario '${definition.title}'. Context details: Location: ${definition.location}, My Role: ${definition.userRole}, Your Role: ${definition.aiRole}. Dynamic variables: [$varsDesc]. Please greet me in character!"
        sendMessage(prompt, isVoice = false)
    }

    fun startStoryScene(scene: com.example.data.model.StoryScene) {
        val session = repository.startStorySceneSession(scene, uiState.value.userName)
        val title = scene.getTitle(uiState.value.userName)
        _activeScenario.value = title
        repository.markSceneCompleted(scene.id)
        val varsDesc = session.sessionVariables.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        val prompt = "Let me start Story Scene '${title}'. Context: ${scene.description}, Your Role: ${scene.aiRole}. Dynamic variables: [$varsDesc]. Please initiate our conversation!"
        sendMessage(prompt, isVoice = false)
    }

    fun startExtraModule(module: com.example.data.model.ExtraStudyModule) {
        _activeScenario.value = module.title
        repository.endCurrentScenarioSession()
        val prompt = "Let's work on the extra study module: '${module.title}' (${module.subtitle}). Focus area: ${module.description}. Please begin with a helpful introduction and a practical question!"
        sendMessage(prompt, isVoice = false)
    }

    fun clearActiveScenario() {
        _activeScenario.value = null
        repository.endCurrentScenarioSession()
    }

    fun updateCoachGender(gender: com.example.data.model.CoachGender) {
        repository.updateCoachGender(gender)
        ttsManager.setCoachGender(gender)
    }

    fun updateViewModes(showAvatar: Boolean, showBubbles: Boolean) {
        repository.updateViewModes(showAvatar, showBubbles)
    }

    fun updateUnlockAllStoryChapters(unlockAll: Boolean) {
        repository.updateUnlockAllStoryChapters(unlockAll)
    }

    private fun getStarterPromptForScenario(scenario: String): String {
        return when (scenario) {
            "Coffee Shop Order" -> "Hi! I'd like to order a drink please."
            "Job Interview" -> "Hello, I am ready for the interview."
            "Airport Customs" -> "Hello Officer, here is my passport."
            "Hotel Check-in" -> "Good evening, I have a reservation under my name."
            "Casual Weekend Chat" -> "Hey! What are your plans for the weekend?"
            else -> "Hello Coach, let's start!"
        }
    }

    fun speakCoachMessage(messageId: Long, text: String) {
        if (isPlayingTts.value && currentPlayingMsgId.value == messageId) {
            ttsManager.stop()
        } else {
            ttsManager.speak(text, messageId, uiState.value.selectedCoachGender)
        }
    }

    fun toggleTranslateMessage(messageId: Long, text: String) {
        if (_translations.value.containsKey(messageId)) {
            _translations.value = _translations.value - messageId
            return
        }

        viewModelScope.launch {
            _translatingIds.value = _translatingIds.value + messageId
            try {
                val targetLang = uiState.value.nativeLanguage.ifBlank { "Türkçe" }
                val translation = repository.translateText(text, targetLang)
                _translations.value = _translations.value + (messageId to translation)
            } catch (e: Exception) {
                _translations.value = _translations.value + (messageId to "Çeviri alınamadı.")
            } finally {
                _translatingIds.value = _translatingIds.value - messageId
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        repository.updateSpeechRate(rate)
        ttsManager.setSpeechRate(rate)
    }

    fun updateTtsEngineMode(mode: TtsEngineMode) {
        repository.updateTtsEngineMode(mode)
        ttsManager.setEngineMode(mode)
    }

    fun updateFallbackEngineOption(option: com.example.audio.FallbackEngineOption) {
        repository.updateFallbackEngineOption(option)
        ttsManager.setFallbackEngineOption(option)
    }

    fun updateKokoroFemaleVoice(voice: String) {
        repository.updateKokoroFemaleVoice(voice)
        ttsManager.setKokoroVoices(voice, uiState.value.kokoroMaleVoice)
    }

    fun updateKokoroMaleVoice(voice: String) {
        repository.updateKokoroMaleVoice(voice)
        ttsManager.setKokoroVoices(uiState.value.kokoroFemaleVoice, voice)
    }

    fun downloadKokoroModel() {
        ttsManager.kokoroModelManager.downloadModel(viewModelScope) { success, errorMsg ->
            if (!success && !errorMsg.isNullOrBlank()) {
                _errorMessage.value = errorMsg
            }
        }
    }

    fun cancelKokoroDownload() {
        ttsManager.kokoroModelManager.cancelDownload()
    }

    fun deleteKokoroModel() {
        ttsManager.kokoroModelManager.deleteModel()
        ttsManager.clearAudioCache()
    }

    fun clearAudioCache() {
        ttsManager.clearAudioCache()
    }

    fun updateAllowAutoAndroidFallback(allow: Boolean) {
        repository.updateAllowAutoAndroidFallback(allow)
        ttsManager.setAllowAutoAndroidFallback(allow)
    }

    fun updateAzureSpeechKey(key: String) {
        repository.updateAzureSpeechKey(key)
    }

    fun updateAzureSpeechRegion(region: String) {
        repository.updateAzureSpeechRegion(region)
    }

    fun testTtsConnection() {
        val current = uiState.value
        val voice = if (current.selectedCoachGender == com.example.data.model.CoachGender.LEO) current.maleEdgeVoice else current.femaleEdgeVoice

        viewModelScope.launch {
            _isTestingTts.value = true
            _ttsTestResult.value = null
            try {
                val result = ttsManager.testConnection(
                    mode = current.ttsEngineMode,
                    voice = voice
                )
                _ttsTestResult.value = result
            } catch (e: Exception) {
                val prov = if (current.ttsEngineMode == TtsEngineMode.KOKORO_OFFLINE) TtsProvider.KOKORO_OFFLINE else TtsProvider.EDGE_CONSUMER
                _ttsTestResult.value = TtsTestResult(
                    success = false,
                    provider = prov,
                    engineName = current.ttsEngineMode.displayName,
                    voiceId = voice,
                    httpStatusCode = null,
                    message = e.message ?: "Bağlantı testi sırasında hata oluştu."
                )
            } finally {
                _isTestingTts.value = false
            }
        }
    }

    fun testMicrosoftTtsConnection() {
        testTtsConnection()
    }

    fun selectProvider(provider: LlmProvider) {
        repository.selectProvider(provider)
        _errorMessage.value = null
        _testState.value = ConnectionTestState.Idle
    }

    fun updateApiKey(key: String) {
        repository.updateApiKey(key)
        _errorMessage.value = null
    }

    fun updateBaseUrl(url: String) {
        repository.updateBaseUrl(url)
        _errorMessage.value = null
    }

    fun updateModelName(model: String) {
        repository.updateModelName(model)
        _errorMessage.value = null
    }

    fun updateCefrLevel(level: String) = repository.updateCefrLevel(level)
    fun updateUserName(name: String) = repository.updateUserName(name)
    fun updateNativeLanguage(lang: String) = repository.updateNativeLanguage(lang)

    fun updateUserInterests(interests: String) {
        viewModelScope.launch {
            repository.saveUserInterests(interests)
        }
    }

    fun clearUserMemory() {
        viewModelScope.launch {
            repository.clearUserMemory()
        }
    }

    fun completeOnboarding(name: String, nativeLang: String, level: String, interests: String = "") {
        repository.completeOnboarding(name, nativeLang, level)
        viewModelScope.launch {
            if (interests.isNotBlank()) {
                repository.saveUserInterests(interests)
            }
            val userName = name.ifBlank { "there" }
            val greetingText = if (interests.isNotBlank()) {
                "Hello $userName! Welcome to SpeakCoach. I'm your English Language Coach. I noticed you are interested in $interests. Which of these topics would you like to talk about today, or shall we start with a casual chat?"
            } else {
                "Hello $userName! Welcome to SpeakCoach. I'm your English Language Coach. How are you feeling today? What topic would you like to chat about?"
            }
            val coachMsg = repository.saveCoachGreeting(greetingText)
            if (repository.autoPlayTts.value) {
                ttsManager.speak(coachMsg.text, coachMsg.id, uiState.value.selectedCoachGender)
            }
        }
    }

    fun updateAutoPlayTts(enabled: Boolean) = repository.updateAutoPlayTts(enabled)

    fun updateUseEdgeNeuralTts(useEdge: Boolean) = repository.updateUseEdgeNeuralTts(useEdge)

    fun updateFemaleEdgeVoice(voice: String) = repository.updateFemaleEdgeVoice(voice)

    fun updateMaleEdgeVoice(voice: String) = repository.updateMaleEdgeVoice(voice)

    fun speakWord(word: String) {
        ttsManager.speak(word)
    }

    fun testTtsPlayback() {
        ttsManager.speak("Hello! This is a test of the speech rate setting at ${String.format("%.2f", uiState.value.speechRate)} speed.")
    }

    fun lookupAndShowWordDetails(word: String, contextSentence: String? = null) {
        val clean = word.trim().lowercase().replace(Regex("[^a-zA-Z]"), "")
        if (clean.isBlank()) return

        viewModelScope.launch {
            _isLookingUpWord.value = true
            _selectedWordDefinition.value = null
            try {
                val definition = repository.lookupWordDetails(clean, contextSentence)
                _selectedWordDefinition.value = definition
            } catch (e: Exception) {
                _selectedWordDefinition.value = WordDefinition(
                    word = clean,
                    meaning = "Anlam yüklenemedi.",
                    exampleSentence = "Example sentence with $clean.",
                    contextSentence = contextSentence
                )
            } finally {
                _isLookingUpWord.value = false
            }
        }
    }

    fun dismissWordDetails() {
        _selectedWordDefinition.value = null
        _isLookingUpWord.value = false
    }

    fun saveWordFromDefinition(definition: WordDefinition) {
        viewModelScope.launch {
            repository.saveWord(
                word = definition.word,
                meaning = definition.meaning,
                exampleSentence = definition.exampleSentence,
                contextSentence = definition.contextSentence
            )
            _wordSaveMessage.value = "'${definition.word}' öğrenilecekler listenize kaydedildi! 🎯"
        }
    }

    fun clearWordSaveMessage() {
        _wordSaveMessage.value = null
    }

    fun toggleSavedWordMastered(word: SavedWordEntity) {
        viewModelScope.launch {
            repository.toggleSavedWordMastered(word)
        }
    }

    fun deleteSavedWord(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedWord(id)
        }
    }

    fun toggleGrammarTipMastered(tip: GrammarTipEntity) {
        viewModelScope.launch {
            repository.toggleGrammarTipMastered(tip)
        }
    }

    fun deleteGrammarTip(tipId: Long) {
        viewModelScope.launch {
            repository.deleteGrammarTip(tipId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
