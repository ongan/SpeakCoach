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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.ParseException
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
        private const val EDGE_CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val EDGE_CHROMIUM_MAJOR_VERSION = "143"
        private const val SEC_MS_GEC_VERSION = "1-$EDGE_CHROMIUM_FULL_VERSION"
        private const val EDGE_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"
        private const val EDGE_BASE_URL = "speech.platform.bing.com/consumer/speech/synthesize/readaloud"
    }

    private val secureRandom = SecureRandom()
    private var clockSkewSeconds: Double = 0.0

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var currentWebSocket: WebSocket? = null
    private var currentJob: Job? = null
    private var currentTempFile: File? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingMessageId = MutableStateFlow<Long?>(null)
    val currentPlayingMessageId: StateFlow<Long?> = _currentPlayingMessageId

    // Configuration
    var engineMode: TtsEngineMode = TtsEngineMode.KOKORO_OFFLINE
    var femaleVoice: String = "en-US-AvaNeural"
    var maleVoice: String = "en-US-AndrewNeural"

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        gender: CoachGender = CoachGender.MAYA,
        messageId: Long? = null,
        onStart: (engineName: String) -> Unit = {},
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        stop()

        currentJob = scope.launch {
            try {
                _currentPlayingMessageId.value = messageId
                _isPlaying.value = true

                val voiceName = if (gender == CoachGender.LEO) maleVoice else femaleVoice
                var usedEngineName = "Edge Consumer (Deneysel)"

                val audioBytes: ByteArray = when (engineMode) {
                    TtsEngineMode.EDGE_EXPERIMENTAL -> {
                        usedEngineName = "Edge Consumer (Deneysel)"
                        fetchEdgeNeuralAudio(text, voiceName, speechRate)
                    }
                    else -> {
                        throw Exception("Handled by primary TtsEngine")
                    }
                }

                if (audioBytes.isEmpty()) {
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    onError(Exception("Canlı ses sunucusundan ses verisi alınamadı"))
                    return@launch
                }

                playAudioBytes(audioBytes, { onStart(usedEngineName) }, {
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

    suspend fun testConnection(
        mode: TtsEngineMode = engineMode,
        voice: String = femaleVoice
    ): TtsTestResult {
        return try {
            when (mode) {
                TtsEngineMode.EDGE_EXPERIMENTAL -> {
                    val audioBytes = fetchEdgeNeuralAudio("Test", voice, 1.0f)
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        TtsTestResult(
                            success = true,
                            provider = TtsProvider.EDGE_CONSUMER,
                            engineName = "Edge Consumer (Deneysel)",
                            voiceId = voice,
                            httpStatusCode = 200,
                            message = "Edge WebSocket bağlantısı ve ses sentezi başarılı!"
                        )
                    } else {
                        TtsTestResult(
                            success = false,
                            provider = TtsProvider.EDGE_CONSUMER,
                            engineName = "Edge Consumer (Deneysel)",
                            voiceId = voice,
                            httpStatusCode = null,
                            message = "Microsoft Edge WebSocket sunucusuna erişilemiyor."
                        )
                    }
                }
                TtsEngineMode.KOKORO_OFFLINE -> {
                    TtsTestResult(
                        success = true,
                        provider = TtsProvider.KOKORO_OFFLINE,
                        engineName = "Kokoro Offline Neural",
                        voiceId = voice,
                        httpStatusCode = null,
                        message = "Kokoro çevrimdışı ses motoru hazır."
                    )
                }
                TtsEngineMode.ANDROID_SYSTEM -> {
                    TtsTestResult(
                        success = true,
                        provider = TtsProvider.ANDROID_SYSTEM,
                        engineName = "Android System TTS",
                        voiceId = "System Default",
                        httpStatusCode = 200,
                        message = "Android sistem TTS motoru cihazınızda hazır."
                    )
                }
            }
        } catch (e: Exception) {
            val code = parseHttpStatusCode(e.message)
            val provider = when (mode) {
                TtsEngineMode.KOKORO_OFFLINE -> TtsProvider.KOKORO_OFFLINE
                TtsEngineMode.EDGE_EXPERIMENTAL -> TtsProvider.EDGE_CONSUMER
                TtsEngineMode.ANDROID_SYSTEM -> TtsProvider.ANDROID_SYSTEM
            }
            TtsTestResult(
                success = false,
                provider = provider,
                engineName = mode.displayName,
                voiceId = voice,
                httpStatusCode = code,
                message = e.message ?: "Bilinmeyen bağlantı hatası"
            )
        }
    }

    private fun parseHttpStatusCode(message: String?): Int? {
        if (message == null) return null
        val regex = Regex("HTTP (\\d{3})")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun fetchGoogleOnlineAudio(text: String): ByteArray {
        val chunks = splitTextIntoChunks(text, 180)
        val baos = ByteArrayOutputStream()

        for (chunk in chunks) {
            val encodedText = java.net.URLEncoder.encode(chunk, "UTF-8")
            val endpoints = listOf(
                "https://translate.googleapis.com/translate_tts?client=gtx&ie=UTF-8&tl=en&q=$encodedText",
                "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=en&total=1&idx=0&textlen=${chunk.length}&client=tw-ob",
                "https://translate.google.com/translate_tts?client=at&ie=UTF-8&tl=en&q=$encodedText"
            )

            var chunkSuccess = false
            var lastError = ""

            for (endpoint in endpoints) {
                try {
                    val reqBuilder = Request.Builder().url(endpoint)
                    if (endpoint.contains("googleapis.com")) {
                        reqBuilder.header("User-Agent", "GoogleTranslate/6.29.0.04.336185664 (Linux; U; Android 10; Mobile)")
                    } else if (endpoint.contains("tw-ob")) {
                        reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    } else {
                        reqBuilder.header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    }

                    val response: Response = client.newCall(reqBuilder.build()).execute()
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body
                            if (body != null) {
                                val bytes = body.bytes()
                                if (bytes.isNotEmpty()) {
                                    baos.write(bytes)
                                    chunkSuccess = true
                                }
                            }
                        } else {
                            lastError = "HTTP ${resp.code} (${resp.message})"
                        }
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "Bağlantı hatası"
                }

                if (chunkSuccess) break
            }

            if (!chunkSuccess) {
                throw Exception("Google Online TTS sunucusuna erişilemedi ($lastError). Lütfen internet bağlantınızı kontrol edin.")
            }
        }

        return baos.toByteArray()
    }

    private fun splitTextIntoChunks(text: String, maxLen: Int = 180): List<String> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return emptyList()
        if (cleanText.length <= maxLen) return listOf(cleanText)

        val sentences = cleanText.split(Regex("(?<=[.!?])\\s+"))
        val result = mutableListOf<String>()
        var current = StringBuilder()

        for (s in sentences) {
            if (current.length + s.length + 1 > maxLen) {
                if (current.isNotEmpty()) {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                if (s.length > maxLen) {
                    val words = s.split(" ")
                    for (w in words) {
                        if (current.length + w.length + 1 > maxLen) {
                            if (current.isNotEmpty()) {
                                result.add(current.toString().trim())
                                current = StringBuilder()
                            }
                        }
                        if (current.isNotEmpty()) current.append(" ")
                        current.append(w)
                    }
                } else {
                    current.append(s)
                }
            } else {
                if (current.isNotEmpty()) current.append(" ")
                current.append(s)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

    private fun fetchAzureNeuralAudio(
        text: String,
        voiceName: String,
        speechRate: Float,
        apiKey: String,
        region: String
    ): ByteArray {
        val effectiveRegion = region.trim().ifBlank { "eastus" }
        val endpoint = "https://$effectiveRegion.tts.speech.microsoft.com/cognitiveservices/v1"
        val ssml = buildSsml(text, voiceName, speechRate)

        val request = Request.Builder()
            .url(endpoint)
            .header("Ocp-Apim-Subscription-Key", apiKey.trim())
            .header("Content-Type", "application/ssml+xml")
            .header("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
            .header("User-Agent", "SpeakCoach")
            .post(ssml.toRequestBody("application/ssml+xml; charset=utf-8".toMediaType()))
            .build()

        val response: Response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw Exception("Bağlantı kurulamadı: ${e.message}")
        }

        response.use { resp ->
            if (!resp.isSuccessful) {
                val errorMsg = when (resp.code) {
                    401 -> "Microsoft Neural ses üretilemedi: HTTP 401 (Unauthorized - API Key Geçersiz)"
                    403 -> "Microsoft Neural ses üretilemedi: HTTP 403 (Forbidden - Region veya Yetki Hatalı)"
                    429 -> "Microsoft Neural ses üretilemedi: HTTP 429 (Rate Limit Exceeded - Kota Doldu)"
                    500, 503 -> "Microsoft Neural ses sunucusu yanıt vermedi: HTTP ${resp.code}"
                    else -> "Microsoft Neural ses üretilemedi: HTTP ${resp.code} (${resp.message})"
                }
                throw Exception(errorMsg)
            }
            val body = resp.body ?: throw Exception("Microsoft Neural boş yanıt döndü")
            return body.bytes()
        }
    }

    private fun generateSecMsGec(): String {
        return try {
            var unixSeconds = (System.currentTimeMillis() / 1000.0) + clockSkewSeconds
            unixSeconds += WINDOWS_EPOCH_OFFSET_SECONDS
            unixSeconds -= unixSeconds % 300.0
            val ticks = (unixSeconds * 10000000.0).toLong()
            val strToHash = "${ticks}${TRUSTED_CLIENT_TOKEN}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(strToHash.toByteArray(Charsets.US_ASCII))
            digest.joinToString("") { "%02x".format(it) }.uppercase(Locale.US)
        } catch (e: Exception) {
            Log.e("EdgeNeuralTtsManager", "Sec-MS-GEC calculation error", e)
            ""
        }
    }

    private fun generateConnectionId(): String = UUID.randomUUID().toString().replace("-", "")

    private fun generateMuid(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }.uppercase(Locale.US)
    }

    private fun adjustClockSkewFromServerDate(response: Response?): Boolean {
        val serverDate = response?.header("Date") ?: return false
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            val parsed = formatter.parse(serverDate) ?: return false
            val serverSeconds = parsed.time / 1000.0
            val clientSeconds = System.currentTimeMillis() / 1000.0
            clockSkewSeconds += serverSeconds - clientSeconds
            Log.w("EdgeNeuralTtsManager", "Adjusted Edge clock skew by ${serverSeconds - clientSeconds}s")
            true
        } catch (e: ParseException) {
            Log.w("EdgeNeuralTtsManager", "Could not parse Edge server date: $serverDate")
            false
        }
    }

    private suspend fun fetchEdgeNeuralAudio(
        text: String,
        voiceName: String,
        speechRate: Float
    ): ByteArray = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->

        fun attemptConnection(attempt: Int, lastErr: String = "") {
            if (attempt > 1) {
                if (continuation.isActive) {
                    val message = if (lastErr.contains("HTTP 403")) {
                        "Microsoft Edge Consumer TTS bu ag/cihaz/bolge icin reddedildi ($lastErr). Bu ucretsiz consumer endpoint resmi API degildir; Kokoro Offline veya kullanici izin verirse Android TTS fallback kullanin."
                    } else {
                        "Microsoft Edge WebSocket sunucusuna baglanti kurulamadi ($lastErr)."
                    }
                    continuation.resumeWith(Result.failure(Exception(message)))
                    return
                }
                return
            }

            val secMsGec = generateSecMsGec()
            val connectionId = generateConnectionId()
            val wssUrl = "wss://$EDGE_BASE_URL/edge/v1" +
                    "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                    "&ConnectionId=$connectionId" +
                    "&Sec-MS-GEC=$secMsGec" +
                    "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION"
            Log.i("EdgeNeuralTtsManager", "Opening Edge TTS WebSocket attempt=$attempt connectionId=$connectionId secMsGecVersion=$SEC_MS_GEC_VERSION")
            val request = Request.Builder()
                .url(wssUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$EDGE_CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 Edg/$EDGE_CHROMIUM_MAJOR_VERSION.0.0.0")
                .header("Origin", EDGE_ORIGIN)
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Sec-WebSocket-Version", "13")
                .header("Cookie", "muid=${generateMuid()};")
                .build()

            val audioStream = ByteArrayOutputStream()
            var hasResponded = false

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    try {
                        val configMessage = "X-Timestamp:${getIsoTimestamp()}\r\n" +
                                "Content-Type:application/json; charset=utf-8\r\n" +
                                "Path:speech.config\r\n\r\n" +
                                "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n"
                        webSocket.send(configMessage)

                        val ssml = buildSsml(text, voiceName, speechRate)
                        val ssmlMessage = "X-RequestId:${generateConnectionId()}\r\n" +
                                "Content-Type:application/ssml+xml\r\n" +
                                "X-Timestamp:${getIsoTimestamp()}Z\r\n" +
                                "Path:ssml\r\n\r\n" +
                                ssml
                        webSocket.send(ssmlMessage)
                    } catch (e: Exception) {
                        if (!hasResponded) {
                            hasResponded = true
                            attemptConnection(attempt + 1, e.message ?: "Send error")
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, textMsg: String) {
                    if (textMsg.contains("Path:turn.end")) {
                        webSocket.close(1000, "Done")
                        if (!hasResponded) {
                            hasResponded = true
                            val bytes = audioStream.toByteArray()
                            if (bytes.isNotEmpty() && continuation.isActive) {
                                continuation.resumeWith(Result.success(bytes))
                            } else {
                                attemptConnection(attempt + 1, "Bos ses akisi")
                            }
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
                    val responseCode = response?.code
                    val errorText = if (responseCode != null) {
                        "HTTP $responseCode (${response.message})"
                    } else {
                        t.message ?: "WebSocket hatasi"
                    }
                    Log.e("EdgeNeuralTtsManager", "WebSocket failure (attempt $attempt): $errorText", t)
                    if (!hasResponded) {
                        hasResponded = true
                        if (responseCode == 403 && attempt == 0) {
                            adjustClockSkewFromServerDate(response)
                        }
                        attemptConnection(attempt + 1, errorText)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!hasResponded) {
                        hasResponded = true
                        val bytes = audioStream.toByteArray()
                        if (bytes.isNotEmpty() && continuation.isActive) {
                            continuation.resumeWith(Result.success(bytes))
                        } else {
                            attemptConnection(attempt + 1, "Baglanti kapandi: $reason")
                        }
                    }
                }
            }

            currentWebSocket = client.newWebSocket(request, listener)
        }

        attemptConnection(0)

        continuation.invokeOnCancellation {
            try {
                currentWebSocket?.close(1000, "Cancelled")
            } catch (e: Exception) { }
        }
    }

    private fun playAudioBytes(
        audioBytes: ByteArray,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            cleanupTempFile()
            val tempFile = File.createTempFile("tts_${UUID.randomUUID()}_", ".mp3", context.cacheDir)
            currentTempFile = tempFile

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
                    cleanupTempFile()
                    onDone()
                }
                setOnErrorListener { _, what, extra ->
                    cleanupTempFile()
                    onError(Exception("MediaPlayer hatası: what=$what extra=$extra"))
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e("EdgeNeuralTtsManager", "Error playing audio file", e)
            cleanupTempFile()
            onError(e)
        }
    }

    private fun cleanupTempFile() {
        try {
            currentTempFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        } catch (e: Exception) {
            Log.w("EdgeNeuralTtsManager", "Temp file deletion warning: ${e.message}")
        }
        currentTempFile = null
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

        cleanupTempFile()
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
    }

    fun shutdown() {
        stop()
    }
}

