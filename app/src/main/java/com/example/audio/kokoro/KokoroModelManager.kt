package com.example.audio.kokoro

import android.content.Context
import android.util.Log
import com.example.audio.TtsStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class KokoroModelManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow<TtsStatus>(TtsStatus.Idle)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var downloadJob: Job? = null

    val modelDir: File
        get() = File(context.filesDir, "tts/kokoro/${KokoroModelManifest.MODEL_VERSION}")

    val tempDir: File
        get() = File(context.filesDir, "tts/kokoro/temp_${System.currentTimeMillis()}")

    val archivePartFile: File
        get() = File(context.filesDir, "tts/kokoro/kokoro_download.part")

    init {
        cleanLeftoverTempFiles()
        checkModelStatus()
    }

    fun isModelReady(): Boolean {
        if (!modelDir.exists() || !modelDir.isDirectory) return false
        
        // Find if model file exists (e.g. model.onnx or kokoro-multi-lang-v1_1.int8.onnx)
        val files = modelDir.walkTopDown().toList()
        val hasOnnx = files.any { it.extension == "onnx" }
        val hasVoices = files.any { it.name == "voices.bin" }
        val hasTokens = files.any { it.name == "tokens.txt" }
        val hasLexicon = files.any { it.name == "lexicon-us-en.txt" }
        val hasEspeakData = files.any { it.name == "espeak-ng-data" && it.isDirectory }

        return hasOnnx && hasVoices && hasTokens && hasLexicon && hasEspeakData
    }

    fun checkModelStatus(): TtsStatus {
        val currentStatus = _status.value
        if (currentStatus is TtsStatus.Downloading) {
            return currentStatus
        }
        val ready = isModelReady()
        val newStatus = if (ready) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
        _status.value = newStatus
        return newStatus
    }

    fun getModelDiskSizeMb(): Float {
        if (!modelDir.exists()) return 0f
        val bytes = modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return bytes / (1024f * 1024f)
    }

    fun downloadModel(scope: CoroutineScope, onComplete: (Boolean, String?) -> Unit) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                cleanLeftoverTempFiles()
                val parentDir = archivePartFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }

                _status.value = TtsStatus.Downloading(0L, KokoroModelManifest.ESTIMATED_DOWNLOAD_BYTES, 0f)

                val request = Request.Builder()
                    .url(KokoroModelManifest.DOWNLOAD_URL)
                    .header("User-Agent", "Mozilla/5.0 SpeakCoach/1.0 Android")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val msg = "İndirme başarısız (HTTP ${response.code})"
                    _status.value = TtsStatus.Error(com.example.audio.TtsProvider.KOKORO_OFFLINE, msg)
                    onComplete(false, msg)
                    return@launch
                }

                val body = response.body ?: throw Exception("Boş yanıt alındı")
                val totalBytes = if (body.contentLength() > 0) body.contentLength() else KokoroModelManifest.ESTIMATED_DOWNLOAD_BYTES

                body.byteStream().use { input ->
                    FileOutputStream(archivePartFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!downloadJob!!.isActive) {
                                throw CancellationException("İndirme iptal edildi")
                            }
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            _status.value = TtsStatus.Downloading(downloadedBytes, totalBytes, progress)
                        }
                        output.flush()
                    }
                }

                // Verifying SHA-256 if expected
                _status.value = TtsStatus.Verifying
                val expectedHash = KokoroModelManifest.EXPECTED_SHA256
                if (!expectedHash.isNullOrEmpty()) {
                    val fileHash = calculateSha256(archivePartFile)
                    if (!fileHash.equals(expectedHash, ignoreCase = true)) {
                        archivePartFile.delete()
                        val errMsg = "Model doğrulaması başarısız (SHA-256 uyuşmuyor)"
                        _status.value = TtsStatus.Error(com.example.audio.TtsProvider.KOKORO_OFFLINE, errMsg)
                        onComplete(false, errMsg)
                        return@launch
                    }
                }

                // Decompress Tar/BZip2 safely
                _status.value = TtsStatus.Verifying
                val extractTargetDir = tempDir
                if (extractTargetDir.exists()) extractTargetDir.deleteRecursively()
                extractTargetDir.mkdirs()

                extractTarBz2(archivePartFile, extractTargetDir)

                // Verify essential files in extracted content
                val extractedFiles = extractTargetDir.walkTopDown().toList()
                val onnxFile = extractedFiles.firstOrNull { it.extension == "onnx" }
                val voicesFile = extractedFiles.firstOrNull { it.name == "voices.bin" }
                val tokensFile = extractedFiles.firstOrNull { it.name == "tokens.txt" }
                val lexiconFile = extractedFiles.firstOrNull { it.name == "lexicon-us-en.txt" }
                val espeakDataDir = extractedFiles.firstOrNull { it.name == "espeak-ng-data" && it.isDirectory }

                if (onnxFile == null || voicesFile == null || tokensFile == null || lexiconFile == null || espeakDataDir == null) {
                    archivePartFile.delete()
                    extractTargetDir.deleteRecursively()
                    val msg = "Model dosyası eksik veya bozuk"
                    _status.value = TtsStatus.Error(com.example.audio.TtsProvider.KOKORO_OFFLINE, msg)
                    onComplete(false, msg)
                    return@launch
                }

                // Move extracted files atomically to final modelDir
                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
                }
                modelDir.mkdirs()

                // Move inner folder or contents
                val rootContent = extractTargetDir.listFiles()
                val sourceDir = if (rootContent?.size == 1 && rootContent[0].isDirectory) rootContent[0] else extractTargetDir
                
                sourceDir.copyRecursively(modelDir, overwrite = true)
                
                // Cleanup temp archive and temp extracted folder
                archivePartFile.delete()
                extractTargetDir.deleteRecursively()

                _status.value = TtsStatus.Idle
                Log.i("KokoroModelManager", "Kokoro model successfully installed to ${modelDir.absolutePath}")
                onComplete(true, null)

            } catch (e: CancellationException) {
                archivePartFile.delete()
                _status.value = TtsStatus.Error(com.example.audio.TtsProvider.KOKORO_OFFLINE, "Ses üretimi / indirme iptal edildi.")
                onComplete(false, "İndirme iptal edildi")
            } catch (e: Exception) {
                Log.e("KokoroModelManager", "Error downloading/extracting Kokoro model", e)
                archivePartFile.delete()
                val errMsg = e.message ?: "İndirme ve kurulum hatası"
                _status.value = TtsStatus.Error(com.example.audio.TtsProvider.KOKORO_OFFLINE, errMsg)
                onComplete(false, errMsg)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        archivePartFile.delete()
        _status.value = if (isModelReady()) TtsStatus.Idle else TtsStatus.ModelNotDownloaded
    }

    fun deleteModel(): Boolean {
        cancelDownload()
        val success = if (modelDir.exists()) {
            modelDir.deleteRecursively()
        } else true
        cleanLeftoverTempFiles()
        _status.value = TtsStatus.ModelNotDownloaded
        return success
    }

    private fun extractTarBz2(tarBz2File: File, outputDir: File) {
        val canonicalDest = outputDir.canonicalPath
        FileInputStream(tarBz2File).use { fis ->
            BufferedInputStream(fis).use { bis ->
                BZip2CompressorInputStream(bis).use { bzIn ->
                    TarArchiveInputStream(bzIn).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextEntry
                        while (entry != null) {
                            val destFile = File(outputDir, entry.name)
                            // Path traversal vulnerability / Zip Slip prevention check
                            if (!destFile.canonicalPath.startsWith(canonicalDest + File.separator) &&
                                destFile.canonicalPath != canonicalDest) {
                                throw SecurityException("Path traversal riski tespit edildi: ${entry.name}")
                            }

                            if (entry.isDirectory) {
                                destFile.mkdirs()
                            } else {
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun cleanLeftoverTempFiles() {
        try {
            if (archivePartFile.exists()) archivePartFile.delete()
            val parent = File(context.filesDir, "tts/kokoro")
            if (parent.exists() && parent.isDirectory) {
                parent.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name.startsWith("temp_")) {
                        file.deleteRecursively()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("KokoroModelManager", "Error cleaning temp files: ${e.message}")
        }
    }

    fun findModelFile(extension: String): File? {
        if (!isModelReady()) return null
        return modelDir.walkTopDown().firstOrNull { it.extension == extension || it.name == extension }
    }

    fun findFileByName(filename: String): File? {
        if (!isModelReady()) return null
        return modelDir.walkTopDown().firstOrNull { it.name == filename }
    }
}
