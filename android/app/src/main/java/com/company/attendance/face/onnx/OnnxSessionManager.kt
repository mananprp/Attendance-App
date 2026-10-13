package com.company.attendance.face.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.ActivityManager
import android.content.Context
import com.company.attendance.face.model.ModelType
import com.company.attendance.face.util.StageTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class OnnxSessionManager(
    private val context: Context,
    val modelsDir: File = File(context.filesDir, "models")
) {
    private val mutex = Mutex()
    private var env: OrtEnvironment? = null
    private val cachedSessions = ConcurrentHashMap<ModelType, OrtSession>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var idleReleaseJob: Job? = null

    @Synchronized
    fun getEnvironment(): OrtEnvironment {
        if (env == null) {
            env = OrtEnvironment.getEnvironment()
        }
        return env!!
    }

    fun isLowRamDevice(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return am.isLowRamDevice || mi.totalMem < (2.5 * 1024 * 1024 * 1024)
    }

    private fun createSessionOptions(): OrtSession.SessionOptions {
        val cores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        return OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(cores)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
        }
    }

    /**
     * Ensures the ONNX model file exists on disk, copying from APK assets if necessary.
     */
    fun getModelFile(modelType: ModelType): File {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        val file = File(modelsDir, modelType.fileName)
        if (!file.exists() || file.length() == 0L) {
            try {
                context.assets.open("models/${modelType.fileName}").use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {}
        }
        return file
    }

    fun isWarm(): Boolean {
        return ModelType.entries.all { cachedSessions.containsKey(it) }
    }

    fun cancelIdleRelease() {
        idleReleaseJob?.cancel()
        idleReleaseJob = null
    }

    fun scheduleIdleRelease(afterMs: Long = 120_000L) {
        cancelIdleRelease()
        idleReleaseJob = scope.launch {
            delay(afterMs)
            StageTimer.log("Idle release timer expired. Releasing ONNX sessions.")
            releaseAllSessions()
        }
    }

    /**
     * Pre-loads / warms up ONNX sessions into memory in the background.
     */
    suspend fun warmUp(vararg modelTypes: ModelType) = withContext(Dispatchers.Default) {
        cancelIdleRelease()
        mutex.withLock {
            val ortEnv = getEnvironment()
            for (modelType in modelTypes) {
                if (cachedSessions.containsKey(modelType)) continue
                val modelFile = getModelFile(modelType)
                if (modelFile.exists() && modelFile.length() > 0) {
                    StageTimer.time("WarmUp ${modelType.modelName}") {
                        val sessionOptions = createSessionOptions()
                        try {
                            val session = ortEnv.createSession(modelFile.absolutePath, sessionOptions)
                            cachedSessions[modelType] = session
                        } catch (e: Exception) {
                            StageTimer.log("Failed to warm up ${modelType.modelName}: ${e.message}")
                            try { sessionOptions.close() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes inference using a cached session if available, or temporary session otherwise.
     */
    suspend fun <T> withSession(
        modelType: ModelType,
        block: (OrtEnvironment, OrtSession) -> T
    ): T {
        cancelIdleRelease()
        return mutex.withLock {
            val ortEnv = getEnvironment()

            // If a warmed-up session exists, use it directly
            val cachedSession = cachedSessions[modelType]
            if (cachedSession != null) {
                return@withLock block(ortEnv, cachedSession)
            }

            // Fallback: create temporary session and close after use
            val modelFile = getModelFile(modelType)
            require(modelFile.exists() && modelFile.length() > 0) {
                "Model file ${modelType.fileName} not found in ${modelsDir.absolutePath}"
            }

            val sessionOptions = createSessionOptions()
            var session: OrtSession? = null
            try {
                session = ortEnv.createSession(modelFile.absolutePath, sessionOptions)
                block(ortEnv, session)
            } finally {
                try {
                    session?.close()
                } catch (_: Exception) {}
                try {
                    sessionOptions.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Checks if all required ONNX models exist in storage.
     */
    fun areModelsPresent(): Boolean {
        return ModelType.entries.all { modelType ->
            val file = getModelFile(modelType)
            file.exists() && file.length() > 0
        }
    }

    /**
     * Closes all cached sessions and releases memory.
     */
    fun releaseAllSessions() {
        for ((_, session) in cachedSessions) {
            try {
                session.close()
            } catch (_: Exception) {}
        }
        cachedSessions.clear()
    }

    /**
     * Closes the environment and releases all native handles.
     */
    fun release() {
        cancelIdleRelease()
        releaseAllSessions()
        try {
            env?.close()
            env = null
        } catch (_: Exception) {}
    }
}
