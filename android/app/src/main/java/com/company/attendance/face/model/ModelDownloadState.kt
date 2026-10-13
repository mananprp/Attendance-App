package com.company.attendance.face.model

/**
 * Supported ONNX face models in the sequential pipeline.
 */
enum class ModelType(val modelName: String, val fileName: String, val expectedApproxSizeBytes: Long) {
    DETECTION("det_10g", "det_10g.onnx", 4_305_997L),
    LANDMARK_3D_68("1k3d68", "1k3d68.onnx", 36_268_296L),
    RECOGNITION("w600k_r50", "w600k_r50.onnx", 43_788_579L);

    companion object {
        fun fromModelName(name: String): ModelType? {
            return entries.firstOrNull { it.modelName.equals(name, ignoreCase = true) || it.fileName.equals(name, ignoreCase = true) }
        }
    }
}

/**
 * State of model downloading process.
 */
enum class DownloadState {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    FAILED
}

/**
 * Progress update emitted during model downloads.
 */
data class ModelDownloadProgress(
    val currentModelName: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Float = 0f,
    val state: DownloadState = DownloadState.IDLE,
    val completedModels: Int = 0,
    val totalModels: Int = 3,
    val errorMessage: String? = null
)
