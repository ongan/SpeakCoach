package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object Processing : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechRecognizerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _recognizedTextEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val recognizedTextEvent: SharedFlow<String> = _recognizedTextEvent.asSharedFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var currentLanguageLocale: String = Locale.US.toLanguageTag()

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupListener()
            } catch (e: Exception) {
                Log.e("SpeechRecognizerManager", "Error creating SpeechRecognizer", e)
                _errorState.value = "Failed to initialize Speech Recognizer: ${e.localizedMessage}"
            }
        } else {
            _errorState.value = "Speech recognition is not available on this device."
        }
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _speechState.value = SpeechState.Listening
                _errorState.value = null
                _partialText.value = ""
            }

            override fun onBeginningOfSpeech() {
                _speechState.value = SpeechState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize audio dB level (-2dB to 10dB range) to 0.0..1.0 for real-time visualizer
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsDb.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _speechState.value = SpeechState.Processing
            }

            override fun onError(error: Int) {
                _isListening.value = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error during speech recognition."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap microphone to try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                    else -> "Speech recognition error ($error)"
                }
                _errorState.value = errorMsg
                _speechState.value = SpeechState.Error(errorMsg)
                Log.e("SpeechRecognizerManager", "Speech error ($error): $errorMsg")
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _speechState.value = SpeechState.Idle
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _partialText.value = ""
                    _recognizedTextEvent.tryEmit(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _partialText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening(languageTag: String = currentLanguageLocale) {
        currentLanguageLocale = languageTag
        if (speechRecognizer == null) {
            initializeRecognizer()
            if (speechRecognizer == null) {
                val msg = "Speech recognition is not available on this device."
                _errorState.value = msg
                _speechState.value = SpeechState.Error(msg)
                return
            }
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }
            _partialText.value = ""
            _errorState.value = null
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognizerManager", "Failed to start listening", e)
            val err = "Error starting microphone: ${e.localizedMessage}"
            _errorState.value = err
            _speechState.value = SpeechState.Error(err)
        }
    }

    fun stopListening() {
        try {
            if (_isListening.value) {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _speechState.value = SpeechState.Processing
            }
        } catch (e: Exception) {
            Log.e("SpeechRecognizerManager", "Failed to stop listening", e)
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _speechState.value = SpeechState.Idle
            _partialText.value = ""
        } catch (e: Exception) {
            Log.e("SpeechRecognizerManager", "Failed to cancel listening", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechRecognizerManager", "Error destroying SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _speechState.value = SpeechState.Idle
        }
    }
}

