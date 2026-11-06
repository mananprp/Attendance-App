package com.company.attendance.feature.punch

import android.content.Context
import android.graphics.Bitmap
import app.cash.turbine.test
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.interfaces.FaceVerificationResult
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.model.TrustedTime
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.punch.capture.CameraCaptureUtils
import com.company.attendance.feature.punch.capture.CaptureStep
import com.company.attendance.feature.punch.capture.PunchCaptureNavigationEvent
import com.company.attendance.feature.punch.capture.PunchCaptureViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PunchCaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = mockk(relaxed = true)
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val userLocalDao: UserLocalDao = mockk(relaxed = true)
    private val faceVerifier: FaceVerifier = mockk(relaxed = true)
    private val apiService: com.company.attendance.core.network.AttendanceApiService = mockk(relaxed = true)
    private val okHttpClient: okhttp3.OkHttpClient = mockk(relaxed = true)
    private val trustedTimeProvider: TrustedTimeProvider = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)

    private val testConfig = AppConfig(
        officeLat = 12.9716,
        officeLng = 77.5946,
        radiusMeters = 100,
        timezone = "Asia/Kolkata",
        syncWindows = listOf("16:00", "00:00"),
        syncJitterMinutes = 30,
        matchThreshold = 0.55f
    )

    private val sampleEmbeddingFloats = FloatArray(512) { 0.1f }
    private val sampleEmbeddingBytes = CameraCaptureUtils.floatArrayToByteArray(sampleEmbeddingFloats)

    private val testUser = UserLocalEntity(
        userId = "EMP1001",
        displayName = "Alice Smith",
        photoChangeUsed = false,
        hasPhoto = true,
        referenceEmbedding = sampleEmbeddingBytes,
        updatedAt = System.currentTimeMillis()
    )

    private val dummyBitmap: Bitmap = mockk(relaxed = true)

    private val testTrustedTime = TrustedTime(
        epochMillis = 1759230000000L,
        source = TimeSource.NTP,
        isUnverified = false,
        localDate = LocalDate.of(2026, 9, 30),
        timezoneId = "Asia/Kolkata"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val filesDir = File.createTempFile("test_punches", "dir").parentFile
        every { context.filesDir } returns filesDir
        every { tokenStorage.getUserId() } returns "EMP1001"
        coEvery { userLocalDao.getUserDirect("EMP1001") } returns testUser
        coEvery { remoteConfigRepository.getConfig() } returns testConfig
        coEvery { faceVerifier.areModelsReady() } returns true
        val mockSession = mockk<com.company.attendance.face.verifier.StreamingPunchSession>(relaxed = true)
        every { faceVerifier.startStreamingSession(any()) } returns mockSession
        coEvery { mockSession.finish(any(), any()) } coAnswers {
            faceVerifier.verifyPunchFrames(emptyList(), firstArg(), secondArg())
        }
        coEvery { trustedTimeProvider.getTrustedTime() } returns testTrustedTime
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PunchCaptureViewModel(
        context, tokenStorage, userLocalDao, faceVerifier,
        apiService, okHttpClient,
        trustedTimeProvider, remoteConfigRepository, punchRepository
    )

    @Test
    fun `models not ready transitions to failure step`() = runTest {
        coEvery { faceVerifier.areModelsReady() } returns false

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 15f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("Models Missing", state.failureTitle)
    }

    @Test
    fun `missing reference embedding transitions to failure step`() = runTest {
        coEvery { userLocalDao.getUserDirect("EMP1001") } returns testUser.copy(referenceEmbedding = null)
        every { tokenStorage.hasPhoto() } returns false

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 15f)
        advanceUntilIdle()

        // Process frames without reference embedding
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("Profile Photo Missing", state.failureTitle)
    }

    @Test
    fun `successful face match records punch and transitions to success step`() = runTest {
        val dummyPunch = Punch(
            punchUuid = "uuid-1234",
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = testTrustedTime.epochMillis,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 12.9716,
            longitude = 77.5946,
            distanceMeters = 15f,
            matchScore = 0.88f,
            imagePath = "/path/to/image.jpg",
            syncStatus = SyncStatus.PENDING
        )

        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.Match(
            score = 0.88f,
            threshold = 0.55f,
            alignedFaceBitmap = dummyBitmap,
            embedding = sampleEmbeddingFloats
        )

        coEvery {
            punchRepository.recordPunch(
                userId = "EMP1001",
                punchType = PunchType.IN,
                time = testTrustedTime,
                latitude = 12.9716,
                longitude = 77.5946,
                distanceMeters = 15f,
                matchScore = 0.88f,
                capturedImagePath = any()
            )
        } returns Result.success(dummyPunch)

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 15f)
        advanceUntilIdle()

        assertEquals(CaptureStep.READY, viewModel.uiState.value.step)
        viewModel.startCapture()
        assertEquals(CaptureStep.CAPTURING, viewModel.uiState.value.step)

        // Simulate frame sampling
        for (i in 1..15) {
            viewModel.onFrameSampled(dummyBitmap)
        }

        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.SUCCESS, state.step)
        assertEquals(0.88f, state.matchScore ?: 0f, 0.01f)
        assertNotNull(state.recordedTimeFormatted)
        assertNotNull(state.recordedDateFormatted)
        assertFalse(state.isTimeUnverified)

        coVerify {
            punchRepository.recordPunch(
                userId = "EMP1001",
                punchType = PunchType.IN,
                time = testTrustedTime,
                latitude = 12.9716,
                longitude = 77.5946,
                distanceMeters = 15f,
                matchScore = 0.88f,
                capturedImagePath = any()
            )
        }
    }

    @Test
    fun `face mismatch transitions to failure and does not record punch`() = runTest {
        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.Mismatch(
            score = 0.41f,
            threshold = 0.55f,
            bestFrameBitmap = dummyBitmap
        )

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 15f)
        advanceUntilIdle()

        viewModel.startCapture()
        viewModel.onFrameSampled(dummyBitmap)
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("Face Mismatch", state.failureTitle)
        assertEquals(0.41f, state.matchScore ?: 0f, 0.01f)

        coVerify(exactly = 0) {
            punchRepository.recordPunch(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `no face detected transitions to failure and does not record punch`() = runTest {
        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.NoFaceDetected

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.OUT, 12.9716, 77.5946, 20f)
        advanceUntilIdle()

        viewModel.startCapture()
        viewModel.onFrameSampled(dummyBitmap)
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("No Face Detected", state.failureTitle)

        coVerify(exactly = 0) {
            punchRepository.recordPunch(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `multiple faces detected transitions to failure with face count`() = runTest {
        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.MultipleFacesDetected(faceCount = 2)

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 10f)
        advanceUntilIdle()

        viewModel.startCapture()
        viewModel.onFrameSampled(dummyBitmap)
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("Multiple Faces Detected", state.failureTitle)
        assertTrue(state.failureMessage?.contains("2 faces") == true)

        coVerify(exactly = 0) {
            punchRepository.recordPunch(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `liveness failure transitions to failure with reason`() = runTest {
        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.LivenessFailed("No natural eye blinks detected")

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 10f)
        advanceUntilIdle()

        viewModel.startCapture()
        viewModel.onFrameSampled(dummyBitmap)
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CaptureStep.FAILURE, state.step)
        assertEquals("Liveness Check Failed", state.failureTitle)
        assertTrue(state.failureReason?.contains("No natural eye blinks") == true)

        coVerify(exactly = 0) {
            punchRepository.recordPunch(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `try again restarts capture without consuming punch`() = runTest {
        coEvery {
            faceVerifier.verifyPunchFrames(any(), any(), any())
        } returns FaceVerificationResult.NoFaceDetected

        val viewModel = createViewModel()

        viewModel.initSession(PunchType.IN, 12.9716, 77.5946, 10f)
        advanceUntilIdle()

        viewModel.startCapture()
        viewModel.onFrameSampled(dummyBitmap)
        viewModel.processCapturedFrames()
        advanceUntilIdle()

        assertEquals(CaptureStep.FAILURE, viewModel.uiState.value.step)

        // User clicks Try Again -> returns to READY state
        viewModel.onTryAgain()
        advanceUntilIdle()

        assertEquals(CaptureStep.READY, viewModel.uiState.value.step)
        assertEquals(0, viewModel.uiState.value.sampledFramesCount)
    }

    @Test
    fun `done and cancel buttons trigger navigation events`() = runTest {
        val viewModel = createViewModel()

        viewModel.navigationEvents.test {
            viewModel.onDoneClicked()
            advanceUntilIdle()
            assertEquals(PunchCaptureNavigationEvent.FinishSuccess, awaitItem())

            viewModel.onCancelClicked()
            advanceUntilIdle()
            assertEquals(PunchCaptureNavigationEvent.FinishCancel, awaitItem())
        }
    }
}
