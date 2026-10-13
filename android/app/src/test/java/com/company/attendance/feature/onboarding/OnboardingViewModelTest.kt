package com.company.attendance.feature.onboarding

import android.graphics.Bitmap
import app.cash.turbine.test
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PhotoUploadResponseDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.onboarding.model.OnboardingStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val faceVerifier: FaceVerifier = mockk()
    private val apiService: AttendanceApiService = mockk()
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val userLocalDao: UserLocalDao = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { tokenStorage.getUserId() } returns "EMP1001"

        viewModel = OnboardingViewModel(
            context = context,
            faceVerifier = faceVerifier,
            apiService = apiService,
            tokenStorage = tokenStorage,
            userLocalDao = userLocalDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial step is CAMERA_PERMISSION`() {
        assertEquals(OnboardingStep.CAMERA_PERMISSION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onCameraPermissionResult true advances to PHOTO_CAPTURE`() {
        viewModel.onCameraPermissionResult(true)
        assertTrue(viewModel.uiState.value.isCameraPermissionGranted)
        assertEquals(OnboardingStep.PHOTO_CAPTURE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onPhotoCaptured with valid face extracts embedding and advances to PHOTO_CONFIRMATION`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val dummyEmbedding = FloatArray(512) { 0.1f * it }

        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(dummyEmbedding)

        viewModel.onPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAnalyzingFace)
        assertNull(viewModel.uiState.value.faceValidationError)
        assertNotNull(viewModel.uiState.value.extractedEmbedding)
        assertEquals(OnboardingStep.PHOTO_CONFIRMATION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `onPhotoCaptured with no face displays clear error guidance`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.failure(
            IllegalArgumentException("No face found in profile photo")
        )

        viewModel.onPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAnalyzingFace)
        assertNull(viewModel.uiState.value.extractedEmbedding)
        assertTrue(viewModel.uiState.value.faceValidationError?.contains("No face detected") == true)
    }

    @Test
    fun `onPhotoCaptured with multiple faces displays clear error guidance`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.failure(
            IllegalArgumentException("More than one face in profile photo")
        )

        viewModel.onPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAnalyzingFace)
        assertTrue(viewModel.uiState.value.faceValidationError?.contains("Multiple faces detected") == true)
    }

    @Test
    fun `onRetakePhoto resets captured state and returns to PHOTO_CAPTURE`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(FloatArray(512))

        viewModel.onPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        viewModel.onRetakePhoto()

        assertNull(viewModel.uiState.value.capturedBitmap)
        assertNull(viewModel.uiState.value.extractedEmbedding)
        assertNull(viewModel.uiState.value.faceValidationError)
        assertEquals(OnboardingStep.PHOTO_CAPTURE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `uploadPhoto success updates TokenStorage, UserLocalDao, and advances to LOCATION_PERMISSION`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val stream = arg<java.io.OutputStream>(2)
            stream.write(byteArrayOf(1, 2, 3, 4))
            true
        }

        val dummyEmbedding = FloatArray(512) { 0.5f }
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(dummyEmbedding)

        val uploadResponse = PhotoUploadResponseDto(
            version = 1,
            photoUrl = "/uploads/profile_1.jpg",
            photoChangeUsed = false
        )
        coEvery { apiService.uploadPhotoV1(any()) } returns Response.success(uploadResponse)

        viewModel.onPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        viewModel.uploadPhoto()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUploading)
        assertNull(viewModel.uiState.value.uploadError)
        assertEquals(1, viewModel.uiState.value.activePhotoVersion)
        assertEquals(OnboardingStep.LOCATION_PERMISSION, viewModel.uiState.value.currentStep)

        verify { tokenStorage.updateHasPhoto(true, 1) }
        verify { tokenStorage.updatePhotoChangeUsed(false) }

        val embeddingSlot = slot<ByteArray>()
        coVerify { userLocalDao.updateReferenceEmbedding("EMP1001", capture(embeddingSlot), any()) }
        assertTrue(embeddingSlot.captured.isNotEmpty())
    }

    @Test
    fun `onLocationPermissionResult true advances to COMPLETED`() {
        viewModel.onLocationPermissionResult(true)
        assertTrue(viewModel.uiState.value.isLocationPermissionGranted)
        assertEquals(OnboardingStep.COMPLETED, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `completeOnboarding emits completion event`() = runTest {
        viewModel.onboardingCompletedEvent.test {
            viewModel.completeOnboarding()
            awaitItem()
        }
    }
}
