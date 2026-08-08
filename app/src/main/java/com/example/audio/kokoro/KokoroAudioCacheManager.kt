package com.example.audio.kokoro

import android.content.Context
import android.util.Log
import com.example.audio.TtsAudio
import com.example.audio.TtsProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class KokoroAudioCacheManager(private val context: Context) {

    private val cacheDir: File
        get() = File(context.cacheDir, "tts_cache").also { if (!it.exists()) it.mkdirs() }

    private val MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB

    fun getCacheKey(text: String, voiceId: String, speed: Float, modelVersion: String = KokoroModelManifest.MODEL_VERSION): String {
        val normalizedText = text.trim().lowercase().replace("\\s+".toRegex(), " ")
        val rawKey = "${modelVersion}_${voiceId}_${"%.2f".format(speed)}_${normalizedText}"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(rawKey.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getCachedAudio(key: String, voiceId: String): TtsAudio? {
        val file = File(cacheDir, "$key.wav")
        if (!file.exists() || !file.isFile) return null

        return try {
            file.setLastModified(System.currentTimeMillis()) // Update LRU timestamp
            readWavFile(file, voiceId)
        } catch (e: Exception) {
            Log.e("KokoroAudioCache", "Error reading cached audio file: ${e.message}")
            file.delete()
            null
        }
    }

    fun putAudioInCache(key: String, audio: TtsAudio) {
        try {
            trimCacheIfNeeded()
            val file = File(cacheDir, "$key.wav")
            writeWavFile(file, audio)
        } catch (e: Exception) {
            Log.e("KokoroAudioCache", "Error caching audio: ${e.message}")
        }
    }

    fun clearCache() {
        try {
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e("KokoroAudioCache", "Error clearing cache: ${e.message}")
        }
    }

    fun getCacheSizeMb(): Float {
        if (!cacheDir.exists()) return 0f
        val bytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        return bytes / (1024f * 1024f)
    }

    private fun trimCacheIfNeeded() {
        val files = cacheDir.listFiles() ?: return
        var currentSize = files.sumOf { it.length() }
        if (currentSize <= MAX_CACHE_SIZE_BYTES) return

        val sortedFiles = files.sortedBy { it.lastModified() }
        for (file in sortedFiles) {
            if (currentSize <= MAX_CACHE_SIZE_BYTES) break
            val len = file.length()
            if (file.delete()) {
                currentSize -= len
            }
        }
    }

    private fun writeWavFile(file: File, audio: TtsAudio) {
        val pcmData = floatArrayToPcm16(audio.samples)
        val sampleRate = audio.sampleRate
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(chunkSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size for PCM
                putShort(1) // AudioFormat 1 = PCM
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }.array()

            out.write(header)
            out.write(pcmData)
        }
    }

    private fun readWavFile(file: File, voiceId: String): TtsAudio? {
        FileInputStream(file).use { input ->
            val header = ByteArray(44)
            if (input.read(header) < 44) return null

            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val riff = String(header, 0, 4)
            val wave = String(header, 8, 4)
            if (riff != "RIFF" || wave != "WAVE") return null

            val sampleRate = buffer.getInt(24)
            val dataSize = file.length() - 44
            val pcmBytes = ByteArray(dataSize.toInt())
            input.read(pcmBytes)

            val floats = pcm16ToFloatArray(pcmBytes)
            return TtsAudio(
                samples = floats,
                sampleRate = if (sampleRate > 0) sampleRate else 24000,
                provider = TtsProvider.KOKORO_OFFLINE,
                voiceId = voiceId
            )
        }
    }

    private fun floatArrayToPcm16(floats: FloatArray): ByteArray {
        val bytes = ByteArray(floats.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            val clamped = f.coerceIn(-1.0f, 1.0f)
            val s = (clamped * 32767.0f).toInt().toShort()
            buffer.putShort(s)
        }
        return bytes
    }

    private fun pcm16ToFloatArray(bytes: ByteArray): FloatArray {
        val floats = FloatArray(bytes.size / 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in floats.indices) {
            floats[i] = buffer.getShort() / 32768.0f
        }
        return floats
    }
}
