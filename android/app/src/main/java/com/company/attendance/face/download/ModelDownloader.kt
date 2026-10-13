package com.company.attendance.face.download

import android.content.Context
import com.company.attendance.face.model.DownloadState
import com.company.attendance.face.model.ModelDownloadProgress
import com.company.attendance.face.model.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ModelDownloader(
    private val context: Context,
    val modelsDir: File = File(context.filesDir, "models")
) {
    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }

    /**
     * Checks if all required models exist on disk and pass non-empty size checks.
     * If missing from internal storage, automatically unpacks them from bundled APK assets.
     */
    fun areAllModelsReady(): Boolean {
        val allPresent = ModelType.entries.all { modelType ->
            val file = File(modelsDir, modelType.fileName)
            file.exists() && file.length() > 0
        }
        if (allPresent) return true

        return tryUnpackModelsFromAssets()
    }

    /**
     * Unpacks bundled ONNX models from APK assets/models/ to internal app storage.
     */
    fun tryUnpackModelsFromAssets(): Boolean {
        return try {
            val assetManager = context.assets
            val assetModelsList = runCatching { assetManager.list("models") ?: emptyArray() }.getOrDefault(emptyArray()).toSet()
            val rootList = runCatching { assetManager.list("") ?: emptyArray() }.getOrDefault(emptyArray()).toSet()

            for (modelType in ModelType.entries) {
                val targetFile = File(modelsDir, modelType.fileName)
                if (targetFile.exists() && targetFile.length() > 0) continue

                val assetPath = when {
                    assetModelsList.contains(modelType.fileName) -> "models/${modelType.fileName}"
                    rootList.contains(modelType.fileName) -> modelType.fileName
                    else -> null
                } ?: return false

                assetManager.open(assetPath).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            ModelType.entries.all {
                val file = File(modelsDir, it.fileName)
                file.exists() && file.length() > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns list of model names that are missing or corrupt.
     */
    suspend fun getMissingOrCorruptModels(): List<String> = withContext(Dispatchers.IO) {
        val missing = mutableListOf<String>()

        for (modelType in ModelType.entries) {
            val file = File(modelsDir, modelType.fileName)
            if (!file.exists() || file.length() == 0L) {
                missing.add(modelType.modelName)
            }
        }
        missing
    }

    /**
     * Flow that verifies or unpacks models from bundled assets.
     */
    fun downloadModels(): Flow<ModelDownloadProgress> = flow {
        emit(
            ModelDownloadProgress(
                state = DownloadState.CONNECTING,
                totalModels = ModelType.entries.size
            )
        )

        val success = tryUnpackModelsFromAssets()
        if (success) {
            emit(
                ModelDownloadProgress(
                    state = DownloadState.COMPLETED,
                    percent = 100f,
                    completedModels = ModelType.entries.size,
                    totalModels = ModelType.entries.size
                )
            )
        } else {
            val isReady = areAllModelsReady()
            if (isReady) {
                emit(
                    ModelDownloadProgress(
                        state = DownloadState.COMPLETED,
                        percent = 100f,
                        completedModels = ModelType.entries.size,
                        totalModels = ModelType.entries.size
                    )
                )
            } else {
                emit(
                    ModelDownloadProgress(
                        state = DownloadState.FAILED,
                        errorMessage = "Bundled ONNX model assets missing from APK."
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        file.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
