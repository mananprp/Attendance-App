package com.company.attendance.face

import android.content.Context
import android.graphics.Bitmap
import com.company.attendance.core.interfaces.FaceVerificationResult
import com.company.attendance.face.download.ModelDownloader
import com.company.attendance.face.model.ModelType
import com.company.attendance.face.onnx.ArcFaceEmbedder
import com.company.attendance.face.onnx.Landmark3DRunner
import com.company.attendance.face.onnx.LivenessEngine
import com.company.attendance.face.onnx.OnnxSessionManager
import com.company.attendance.face.onnx.ScrfdDetector
import com.company.attendance.face.verifier.FaceVerifierImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FaceVerifierStateTest {

    private lateinit var mockContext: Context
    private lateinit var mockSessionManager: OnnxSessionManager
    private lateinit var mockDownloader: ModelDownloader
    private lateinit var mockDetector: ScrfdDetector
    private lateinit var mockLandmarkRunner: Landmark3DRunner
    private lateinit var mockEmbedder: ArcFaceEmbedder
    private lateinit var mockLivenessEngine: LivenessEngine
    private lateinit var faceVerifier: FaceVerifierImpl

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockDownloader = mockk(relaxed = true)
        mockDetector = mockk(relaxed = true)
        mockLandmarkRunner = mockk(relaxed = true)
        mockEmbedder = mockk(relaxed = true)
        mockLivenessEngine = mockk(relaxed = true)

        faceVerifier = FaceVerifierImpl(
            context = mockContext,
            sessionManager = mockSessionManager,
            modelDownloader = mockDownloader,
            scrfdDetector = mockDetector,
            landmarkRunner = mockLandmarkRunner,
            arcFaceEmbedder = mockEmbedder,
            livenessEngine = mockLivenessEngine
        )
    }

    @Test
    fun testVerifyPunchFramesReturnsModelMissingWhenModelsNotPresent() = runTest {
        every { mockSessionManager.areModelsPresent() } returns false
        every { mockDownloader.modelsDir } returns java.io.File("/tmp/models")

        val result = faceVerifier.verifyPunchFrames(
            frames = listOf(mockk()),
            referenceEmbedding = FloatArray(512),
            threshold = 0.55f
        )

        assertTrue(result is FaceVerificationResult.ModelMissingOrCorrupt)
        val missing = (result as FaceVerificationResult.ModelMissingOrCorrupt).missingModels
        assertTrue(missing.contains(ModelType.DETECTION.modelName))
    }

    @Test
    fun testVerifyPunchFramesReturnsNoFaceDetectedWhenEmptyFramesProvided() = runTest {
        every { mockSessionManager.areModelsPresent() } returns true

        val result = faceVerifier.verifyPunchFrames(
            frames = emptyList(),
            referenceEmbedding = FloatArray(512),
            threshold = 0.55f
        )

        assertEquals(FaceVerificationResult.NoFaceDetected, result)
    }

    @Test
    fun testExtractReferenceEmbeddingFailsWhenModelsMissing() = runTest {
        every { mockSessionManager.areModelsPresent() } returns false
        every { mockDownloader.modelsDir } returns java.io.File("/tmp/models")

        val mockBitmap = mockk<Bitmap>()
        val result = faceVerifier.extractReferenceEmbedding(mockBitmap)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Required models are missing") == true)
    }

    @Test
    fun testMatchResultEqualsAndHashCode() {
        val bitmap = mockk<Bitmap>()
        val emb1 = floatArrayOf(0.1f, 0.2f, 0.3f)
        val emb2 = floatArrayOf(0.1f, 0.2f, 0.3f)

        val match1 = FaceVerificationResult.Match(0.85f, 0.55f, bitmap, emb1)
        val match2 = FaceVerificationResult.Match(0.85f, 0.55f, bitmap, emb2)

        assertEquals(match1, match2)
        assertEquals(match1.hashCode(), match2.hashCode())
    }
}
