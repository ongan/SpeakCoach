package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.model.CoachGender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class EdgeNeuralTtsManager(private val context: Context) {

    companion object {
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val WINDOWS_EPOCH_OFFSET_SECONDS = 11644473600L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var currentWebSocket: WebSocket? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingMessageId = MutableStateFlow<Long?>(null)
    val currentPlayingMessageId: StateFlow<Long?> = _currentPlayingMessageId

    // Selected Voice Names
    var femaleVoice: String = "en-US-AvaNeural"
    var maleVoice: String = "en-US-AndrewNeural"

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        gender: CoachGender = CoachGender.MAYA,
        messageId: Long? = null,
        onStart: (String) -> Unit = {},
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        stop()

        currentJob = scope.launch {
            try {
                _currentPlayingMessageId.value = messageId
                _isPlaying.value = true

                val voiceName = if (gender == CoachGender.LEO) maleVoice else femaleVoice
                var usedEngine = "Microsoft Edge Neural TTS"
                
                // Try Edge WebSocket first
                var audioBytes = fetchEdgeNeuralAudio(text, voiceName, speechRate)

                // Fallback to Google Online TTS if Edge returns null
                if (audioBytes == null || audioBytes.isEmpty()) {
                    Log.w("EdgeNeuralTtsManager", "Edge TTS unavailable, falling back to Google Online TTS...")
                    usedEngine = "Google Online TTS"
                    audioBytes = fetchGoogleOnlineAudio(text)
                }

                if (audioBytes == null || audioBytes.isEmpty()) {
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    onError(Exception("Canlı ses sunucusu yanıt vermedi"))
                    return@launch
                }

                playAudioBytes(audioBytes, { onStart(usedEngine) }, {
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    onDone()
                }, { error ->
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    onError(error)
                })

            } catch (e: Exception) {
                Log.e("EdgeNeuralTtsManager", "Synthesis error", e)
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                onError(e)
            }
        }
    }

    private fun generateSecMsGec(): String {
        return try {
            val unixSeconds = System.currentTimeMillis() / 1000L
            val windowsSeconds = unixSeconds + WINDOWS_EPOCH_OFFSET_SECONDS
            val roundedSeconds = (windowsSeconds / 300L) * 300L
            val ticks = roundedSeconds * 10000000L
            val strToHash = "${ticks}${TRUSTED_CLIENT_TOKEN}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(strToHash.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.e("EdgeNeuralTtsManager", "Sec-MS-GEC calculation error", e)
            ""
        }
    }

    private suspend fun fetchEdgeNeuralAudio(
        text: String,
        voiceName: String,
        speechRate: Float
    ): ByteArray? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->

        val secMsGec = generateSecMsGec()
        val wssUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=$TRUSTED_CLIENT_TOKEN&Sec-MS-GEC=$secMsGec"

        val request = Request.Builder()
            .url(wssUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
            .header("Origin", "chrome-extension://jdiccldimpdaibocbdbgmlgfldbpldlc")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Sec-MS-GEC-Version", "1-130.0.0.0")
            .build()

        val audioStream = ByteArrayOutputStream()
        var hasResponded = false

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    val configMessage = "X-Timestamp:${getIsoTimestamp()}\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                    webSocket.send(configMessage)

                    val ssml = buildSsml(text, voiceName, speechRate)
                    val requestId = UUID.randomUUID().toString().replace("-", "")
                    val ssmlMessage = "X-RequestId:$requestId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "Path:ssml\r\n\r\n" +
                            ssml
                    webSocket.send(ssmlMessage)
                } catch (e: Exception) {
                    if (!hasResponded) {
                        hasResponded = true
                        continuation.resumeWith(Result.success(null))
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    webSocket.close(1000, "Done")
                    if (!hasResponded) {
                        hasResponded = true
                        continuation.resumeWith(Result.success(audioStream.toByteArray()))
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val buffer = bytes.toByteArray()
                if (buffer.size >= 2) {
                    val headerLen = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
                    val audioOffset = 2 + headerLen
                    if (buffer.size > audioOffset) {
                        audioStream.write(buffer, audioOffset, buffer.size - audioOffset)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("EdgeNeuralTtsManager", "WebSocket failure: ${t.message}", t)
                if (!hasResponded) {
                    hasResponded = true
                    continuation.resumeWith(Result.success(null))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!hasResponded) {
                    hasResponded = true
                    continuation.resumeWith(Result.success(audioStream.toByteArray()))
                }
            }
        }

        currentWebSocket = client.newWebSocket(request, listener)

        continuation.invokeOnCancellation {
            try {
                currentWebSocket?.close(1000, "Cancelled")
            } catch (e: Exception) { }
        }
    }

    private fun fetchGoogleOnlineAudio(text: String): ByteArray? {
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=en&client=tw-ob"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EdgeNeuralTtsManager", "Google Online TTS error: ${e.message}")
            null
        }
    }

    private fun playAudioBytes(
        audioBytes: ByteArray,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val tempFile = File(context.cacheDir, "edge_neural_tts_temp.mp3")
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    onStart()
                    mp.start()
                }
                setOnCompletionListener {
                    tempFile.delete()
                    onDone()
                }
                setOnErrorListener { _, what, extra ->
                    tempFile.delete()
                    onError(Exception("MediaPlayer error: what=$what extra=$extra"))
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e("EdgeNeuralTtsManager", "Error playing audio file", e)
            onError(e)
        }
    }

    private fun buildSsml(text: String, voiceName: String, rate: Float): String {
        val escapedText = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val ratePercentage = ((rate - 1.0f) * 100).toInt()
        val rateStr = if (ratePercentage >= 0) "+$ratePercentage%" else "$ratePercentage%"

        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                "<voice name='$voiceName'>" +
                "<prosody pitch='+0Hz' rate='$rateStr'>" +
                escapedText +
                "</prosody>" +
                "</voice>" +
                "</speak>"
    }

    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null

        try {
            currentWebSocket?.close(1000, "User stopped")
        } catch (e: Exception) { }
        currentWebSocket = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) { }
        mediaPlayer = null

        _isPlaying.value = false
        _currentPlayingMessageId.value = null
    }

    fun shutdown() {
        stop()
    }
}
