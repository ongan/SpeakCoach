package com.example.audio.kokoro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTrackPlayer {

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false

    suspend fun playAudio(
        samples: FloatArray,
        sampleRate: Int,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {
        stop()
        if (samples.isEmpty()) {
            onDone()
            return@withContext
        }

        try {
            val bytes = floatToPcm16(samples)
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = Math.max(minBufferSize, bytes.size)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            isPlaying = true

            track.play()
            onStart()

            val chunkSize = 4096
            var offset = 0
            while (isPlaying && offset < bytes.size) {
                val length = Math.min(chunkSize, bytes.size - offset)
                val written = track.write(bytes, offset, length)
                if (written < 0) {
                    Log.e("AudioTrackPlayer", "AudioTrack write error: $written")
                    break
                }
                offset += written
            }

            // Wait until remaining playback finishes
            if (isPlaying && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val durationMs = ((bytes.size / 2).toFloat() / sampleRate.toFloat() * 1000f).toLong()
                val sleepChunk = 50L
                var elapsed = 0L
                while (isPlaying && elapsed < durationMs + 200L) {
                    Thread.sleep(sleepChunk)
                    elapsed += sleepChunk
                }
            }

            stopTrackSilently()
            isPlaying = false
            onDone()

        } catch (e: Exception) {
            Log.e("AudioTrackPlayer", "Error during AudioTrack playback", e)
            stopTrackSilently()
            isPlaying = false
            onError(e)
        }
    }

    fun stop() {
        isPlaying = false
        stopTrackSilently()
    }

    private fun stopTrackSilently() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                flush()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioTrackPlayer", "Error stopping track: ${e.message}")
        } finally {
            audioTrack = null
        }
    }

    private fun floatToPcm16(floats: FloatArray): ByteArray {
        val bytes = ByteArray(floats.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            val clamped = f.coerceIn(-1.0f, 1.0f)
            val s = (clamped * 32767.0f).toInt().toShort()
            buffer.putShort(s)
        }
        return bytes
    }
}
