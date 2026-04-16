package com.aicall.companion.assistant

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object GemmaModelManager {
    private const val MODEL_DIR_NAME = "llm_models"
    private const val MODEL_FILE_NAME = "gemma4.litertlm"

    data class ModelSource(
        val name: String,
        val downloadUrl: String,
        val expectedSizeBytes: Long,
    )

    val DEFAULT_MODEL = ModelSource(
        name = "Gemma-4-E2B-it-LiteRT-LM",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        expectedSizeBytes = 2_583_085_056L,
    )

    fun getModelDir(context: Context): File {
        val dir = File(context.filesDir, MODEL_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(context: Context, source: ModelSource = DEFAULT_MODEL): File {
        return File(getModelDir(context), MODEL_FILE_NAME)
    }

    fun hasModel(context: Context, source: ModelSource = DEFAULT_MODEL): Boolean {
        val file = getModelFile(context, source)
        return file.exists() && file.length() > source.expectedSizeBytes * 0.9
    }

    fun getModelInfo(context: Context, source: ModelSource = DEFAULT_MODEL): ModelInfo {
        val file = getModelFile(context, source)
        val sizeBytes = if (file.exists()) file.length() else 0L
        return ModelInfo(
            exists = file.exists(),
            path = file.absolutePath,
            sizeBytes = sizeBytes,
            sizeMb = if (file.exists()) sizeBytes / 1024 / 1024 else 0L,
            matchesExpectedSource = file.exists() && sizeBytes > source.expectedSizeBytes * 0.9,
        )
    }

    suspend fun downloadModel(
        context: Context,
        source: ModelSource = DEFAULT_MODEL,
        onProgress: (Int) -> Unit = {},
    ): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = getModelFile(context, source)
        if (outputFile.exists() && outputFile.length() > source.expectedSizeBytes * 0.9) {
            return@withContext Result.success(outputFile)
        }

        try {
            val connection = URL(source.downloadUrl).openConnection()
            connection.connect()
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.getInputStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toDouble() / totalBytes * 100).toInt()
                            onProgress(progress.coerceIn(0, 100))
                        }
                    }
                }
            }

            if (outputFile.length() > source.expectedSizeBytes * 0.9) {
                Result.success(outputFile)
            } else {
                outputFile.delete()
                Result.failure(Exception("다운로드된 모델 파일이 너무 작습니다."))
            }
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    data class ModelInfo(
        val exists: Boolean,
        val path: String,
        val sizeBytes: Long,
        val sizeMb: Long,
        val matchesExpectedSource: Boolean,
    )
}
