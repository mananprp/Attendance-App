package com.company.attendance.face.verifier

import android.content.Context
import android.graphics.Bitmap
import com.company.attendance.core.interfaces.FaceVerificationResult
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.face.download.ModelDownloader
import com.company.attendance.face.model.FaceDetection
import com.company.attendance.face.model.FrameRecord
import com.company.attendance.face.model.ModelType
import com.company.attendance.face.onnx.ArcFaceEmbedder
import com.company.attendance.face.onnx.Landmark3DRunner
import com.company.attendance.face.onnx.LivenessEngine
import com.company.attendance.face.onnx.OnnxSessionManager
import com.company.attendance.face.onnx.ScrfdDetector
import com.company.attendance.face.util.ImageProcessor
import com.company.attendance.face.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

class FaceVerifierImpl(
    private val context: Context,
    private val sessionManager: OnnxSessionManager = OnnxSessionManager(context),
    private val modelDownloader: ModelDownloader,
    private val scrfdDetector: ScrfdDetector = ScrfdDetector(),
    private val landmarkRunner: Landmark3DRunner = Landmark3DRunner(),
    private val arcFaceEmbedder: ArcFaceEmbedder = ArcFaceEmbedder(),
    private val livenessEngine: LivenessEngine = LivenessEngine()
) : FaceVerifier {

    override suspend fun areModelsReady(): Boolean = withContext(Dispatchers.IO) {
        sessionManager.areModelsPresent()
    }

    override suspend fun prepare(): Boolean = withContext(Dispatchers.Default) {
        if (!areModelsReady()) return@withContext false
        sessionManager.warmUp(
            ModelType.DETECTION,
            ModelType.LANDMARK_3D_68,
            ModelType.RECOGNITION
        )
        true
    }

    override fun startStreamingSession(scope: kotlinx.coroutines.CoroutineScope): StreamingPunchSession {
        return StreamingPunchSession(
            sessionManager = sessionManager,
            scrfdDetector = scrfdDetector,
            landmarkRunner = landmarkRunner,
            arcFaceEmbedder = arcFaceEmbedder,
            livenessEngine = livenessEngine,
            scope = scope
        )
    }

    override suspend fun extractReferenceEmbedding(image: Bitmap): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            if (!areModelsReady()) {
                val missing = ModelType.entries.filter {
                    val file = File(modelDownloader.modelsDir, it.fileName)
                    !file.exists() || file.length() == 0L
                }.map { it.modelName }
                return@withContext Result.failure(
                    IllegalStateException("Required models are missing or corrupt: $missing")
                )
            }

            // Step 1: Detect face using det_10g session
            val detections: List<FaceDetection> = sessionManager.withSession(ModelType.DETECTION) { env, session ->
                scrfdDetector.detect(env, session, image)
            }

            if (detections.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No face found in profile photo"))
            }

            val sorted = detections.sortedByDescending { it.area }
            val mainFace = sorted[0]

            if (sorted.size > 1 && sorted[1].area > 0.3f * mainFace.area) {
                return@withContext Result.failure(IllegalArgumentException("More than one face in profile photo"))
            }

            if (mainFace.score < 0.50f) {
                return@withContext Result.failure(IllegalArgumentException("Face in profile photo is unclear"))
            }

            val stats = ImageProcessor.calculateFaceCropStats(image, mainFace.bbox)
            if (stats == null || stats.minSidePx < 100) {
                return@withContext Result.failure(IllegalArgumentException("Face in profile photo is too small"))
            }

            val kps = mainFace.kps ?: return@withContext Result.failure(
                IllegalArgumentException("No facial landmarks detected")
            )

            // Step 2: Align face to 112x112
            val alignedBitmap = arcFaceEmbedder.alignFace(image, kps)

            // Step 3: Extract embedding using w600k_r50 session
            val embedding = sessionManager.withSession(ModelType.RECOGNITION) { env, session ->
                arcFaceEmbedder.extractEmbedding(env, session, alignedBitmap)
            }

            Result.success(embedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPunchFrames(
        frames: List<Bitmap>,
        referenceEmbedding: FloatArray,
        threshold: Float
    ): FaceVerificationResult = withContext(Dispatchers.Default) {
        try {
            if (!areModelsReady()) {
                val missing = ModelType.entries.filter {
                    val file = File(modelDownloader.modelsDir, it.fileName)
                    !file.exists() || file.length() == 0L
                }.map { it.modelName }
                return@withContext FaceVerificationResult.ModelMissingOrCorrupt(missing)
            }

            if (frames.isEmpty()) {
                return@withContext FaceVerificationResult.NoFaceDetected
            }

            // =========================================================================
            // Pass 1: Optimized Strided Face Detection (det_10g.onnx)
            // =========================================================================
            data class RawDetection(val image: Bitmap, val face: FaceDetection, val sharpness: Float)
            val validDetections = mutableListOf<RawDetection>()
            var multipleFacesCount = 0
            var sampledDetectionCount = 0

            // Sample key frames evenly (target: ~6 to 8 face detections max for speed)
            val stride = max(1, frames.size / 8)

            sessionManager.withSession(ModelType.DETECTION) { env, session ->
                var lastValidFace: FaceDetection? = null

                for (idx in frames.indices) {
                    val frame = frames[idx]
                    val isKeyFrame = (idx % stride == 0) || (idx == frames.lastIndex)

                    var faceToUse: FaceDetection? = null

                    if (isKeyFrame) {
                        sampledDetectionCount++
                        val detections = scrfdDetector.detect(env, session, frame)

                        if (detections.isNotEmpty()) {
                            val sorted = detections.sortedByDescending { it.area }
                            val main = sorted[0]

                            if (sorted.size > 1 && sorted[1].area > 0.3f * main.area) {
                                multipleFacesCount++
                            } else if (main.score >= 0.50f) {
                                faceToUse = main
                                lastValidFace = main
                            }
                        }
                    } else {
                        // Intermediate frame: reuse last detected bounding box if face hasn't moved abruptly
                        faceToUse = lastValidFace
                    }

                    if (faceToUse != null) {
                        val stats = ImageProcessor.calculateFaceCropStats(frame, faceToUse.bbox)
                        if (stats != null && stats.minSidePx >= 100) {
                            validDetections.add(RawDetection(frame, faceToUse, stats.sharpness))
                        }
                    }
                }
            }

            if (sampledDetectionCount > 0 && multipleFacesCount > 0.3f * sampledDetectionCount) {
                return@withContext FaceVerificationResult.MultipleFacesDetected(multipleFacesCount)
            }

            if (validDetections.isEmpty()) {
                return@withContext FaceVerificationResult.NoFaceDetected
            }

            // =========================================================================
            // Pass 2: 3D Landmarks & Liveness (1k3d68.onnx)
            // =========================================================================
            val frameRecords = mutableListOf<FrameRecord>()

            sessionManager.withSession(ModelType.LANDMARK_3D_68) { env, session ->
                for (det in validDetections) {
                    val landmarks3d = landmarkRunner.extractLandmarks(env, session, det.image, det.face.bbox)
                    frameRecords.add(
                        FrameRecord(
                            image = det.image,
                            face = det.face,
                            pts = landmarks3d.points,
                            sharpness = det.sharpness,
                            yaw = landmarks3d.yaw
                        )
                    )
                }
            }

            // Run Liveness evaluation if multiple frames are available
            if (frameRecords.size >= 2) {
                val signals = livenessEngine.computeSignals(frameRecords)
                val verdict = livenessEngine.evaluateVerdict(signals, isIdentityConsistent = true)

                if (verdict.isFailed) {
                    return@withContext FaceVerificationResult.LivenessFailed(
                        verdict.reason ?: "Liveness check failed"
                    )
                }
            }

            // =========================================================================
            // Pass 3: ArcFace Recognition (w600k_r50.onnx)
            // =========================================================================
            // Select top 3 sharpest frontal frames for embedding comparison
            val frontal = frameRecords.filter { abs(it.yaw) <= 0.25f }.ifEmpty { frameRecords }
            val bestRecords = frontal.sortedByDescending { it.sharpness }.take(3)

            val sims = mutableListOf<Float>()
            var bestAlignedBitmap: Bitmap? = null
            var bestEmbedding: FloatArray? = null
            var bestFrameBitmap: Bitmap? = null

            sessionManager.withSession(ModelType.RECOGNITION) { env, session ->
                for (rec in bestRecords) {
                    val kps = rec.face.kps ?: continue
                    val aligned = arcFaceEmbedder.alignFace(rec.image, kps)
                    val embedding = arcFaceEmbedder.extractEmbedding(env, session, aligned)
                    val sim = MathUtils.cosineSimilarity(referenceEmbedding, embedding)
                    sims.add(sim)

                    if (bestAlignedBitmap == null || sim > (sims.maxOrNull() ?: 0f)) {
                        bestAlignedBitmap = aligned
                        bestEmbedding = embedding
                        bestFrameBitmap = rec.image
                    }
                }
            }

            if (sims.isEmpty() || bestAlignedBitmap == null || bestEmbedding == null) {
                return@withContext FaceVerificationResult.NoFaceDetected
            }

            // Use best (max) similarity score achieved among candidate frames
            val rawBestScore = sims.maxOrNull() ?: 0f

            // Calibrate raw cosine score and threshold into intuitive confidence percentages (e.g. 0.43 -> ~70%)
            val calibratedScore = MathUtils.calibrateConfidence(rawBestScore)
            val calibratedThreshold = if (threshold <= 0.50f) MathUtils.calibrateConfidence(threshold) else threshold

            val isMatch = rawBestScore >= (if (threshold > 0.50f) 0.38f else threshold) || calibratedScore >= calibratedThreshold

            return@withContext if (isMatch) {
                FaceVerificationResult.Match(
                    score = calibratedScore,
                    threshold = calibratedThreshold,
                    alignedFaceBitmap = bestAlignedBitmap!!,
                    embedding = bestEmbedding!!
                )
            } else {
                FaceVerificationResult.Mismatch(
                    score = calibratedScore,
                    threshold = calibratedThreshold,
                    bestFrameBitmap = bestFrameBitmap
                )
            }
        } catch (e: Exception) {
            FaceVerificationResult.Error(e, e.message ?: "Verification failed unexpectedly")
        }
    }

    override fun releaseResources() {
        sessionManager.releaseAllSessions()
    }
}
