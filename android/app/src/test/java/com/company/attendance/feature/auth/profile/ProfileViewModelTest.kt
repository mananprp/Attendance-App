package com.company.attendance.feature.auth.profile

import android.graphics.Bitmap
import app.cash.turbine.test
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.model.UserSession
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PhotoDetailResponseDto
import com.company.attendance.core.network.model.PhotoUploadResponseDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.auth.profile.model.ProfileNavigationEvent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val userLocalDao: UserLocalDao = mockk(relaxed = true)
    private val apiService: AttendanceApiService = mockk()
    private val faceVerifier: FaceVerifier = mockk()
    private val context: android.content.Context = mockk(relaxed = true)
    private val okHttpClient: okhttp3.OkHttpClient = mockk(relaxed = true)

    private lateinit var viewModel: ProfileViewModel

    private val initialSession = UserSession(
        userId = "EMP1001",
        displayName = "Alice Smith",
        token = "jwt-test-token",
        photoChangeUsed = false,
        hasPhoto = true,
        activePhotoVersion = 1
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { tokenStorage.getUserSession() } returns initialSession
        every { tokenStorage.getUserId() } returns initialSession.userId
        every { tokenStorage.getDisplayName() } returns initialSession.displayName
        every { tokenStorage.isPhotoChangeUsed() } returns initialSession.photoChangeUsed
        every { tokenStorage.hasPhoto() } returns initialSession.hasPhoto
        every { tokenStorage.getActivePhotoVersion() } returns initialSession.activePhotoVersion

        val photoDetail = PhotoDetailResponseDto(
            version = 1,
            photoUrl = "/uploads/profile_1.jpg",
            uploadedAt = "2026-09-30T10:00:00Z",
            hash = "sha256-hash-1"
        )
        coEvery { apiService.getActivePhoto() } returns Response.success(photoDetail)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            context = context,
            okHttpClient = okHttpClient,
            tokenStorage = tokenStorage,
            userLocalDao = userLocalDao,
            apiService = apiService,
            faceVerifier = faceVerifier
        )
    }

    @Test
    fun `loadUserProfile populates details and fetches active photo`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("EMP1001", viewModel.uiState.value.userId)
        assertEquals("Alice Smith", viewModel.uiState.value.displayName)
        assertFalse(viewModel.uiState.value.photoChangeUsed)
        assertTrue(viewModel.uiState.value.hasPhoto)
        assertEquals(1, viewModel.uiState.value.activePhotoVersion)
        assertEquals("/uploads/profile_1.jpg", viewModel.uiState.value.photoUrl)
        assertTrue(viewModel.uiState.value.canChangePhoto)
    }

    @Test
    fun `when photoChangeUsed is true canChangePhoto is false and openPhotoCapture is blocked`() = runTest {
        val usedSession = initialSession.copy(photoChangeUsed = true, activePhotoVersion = 2)
        every { tokenStorage.getUserSession() } returns usedSession
        every { tokenStorage.isPhotoChangeUsed() } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canChangePhoto)

        viewModel.openPhotoCapture()
        assertFalse(viewModel.uiState.value.isPhotoCaptureOpen)
    }

    @Test
    fun `onNewPhotoCaptured extracts embedding on single face success`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val dummyEmbedding = FloatArray(512) { 0.2f }
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(dummyEmbedding)

        viewModel.openPhotoCapture()
        viewModel.onNewPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAnalyzingFace)
        assertNull(viewModel.uiState.value.faceValidationError)
        assertNotNull(viewModel.uiState.value.extractedNewEmbedding)
    }

    @Test
    fun `confirmPhotoReplacement success updates local Dao and TokenStorage and disables further changes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val stream = arg<java.io.OutputStream>(2)
            stream.write(byteArrayOf(5, 6, 7, 8))
            true
        }

        val dummyEmbedding = FloatArray(512) { 0.3f }
        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(dummyEmbedding)

        val replaceResponse = PhotoUploadResponseDto(
            version = 2,
            photoUrl = "/uploads/profile_2.jpg",
            photoChangeUsed = true
        )
        coEvery { apiService.replacePhotoV2(any()) } returns Response.success(replaceResponse)

        viewModel.openPhotoCapture()
        viewModel.onNewPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        viewModel.confirmPhotoReplacement()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReplacingPhoto)
        assertTrue(viewModel.uiState.value.photoChangeUsed)
        assertEquals(2, viewModel.uiState.value.activePhotoVersion)
        assertFalse(viewModel.uiState.value.canChangePhoto)
        assertFalse(viewModel.uiState.value.isPhotoCaptureOpen)
        assertNotNull(viewModel.uiState.value.successMessage)

        verify { tokenStorage.updatePhotoChangeUsed(true) }
        verify { tokenStorage.updateHasPhoto(true, 2) }

        val embeddingSlot = slot<ByteArray>()
        coVerify { userLocalDao.updateReferenceEmbedding("EMP1001", capture(embeddingSlot), any()) }
        coVerify { userLocalDao.updatePhotoChangeUsed("EMP1001", true, any()) }
    }

    @Test
    fun `confirmPhotoReplacement 403 Forbidden locks photoChangeUsed locally`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val stream = arg<java.io.OutputStream>(2)
            stream.write(byteArrayOf(9, 10))
            true
        }

        coEvery { faceVerifier.extractReferenceEmbedding(mockBitmap) } returns Result.success(FloatArray(512))
        coEvery { apiService.replacePhotoV2(any()) } returns Response.error(
            403,
            "{\"code\":\"PHOTO_CHANGE_EXHAUSTED\",\"message\":\"Profile photo can only be replaced once.\"}".toResponseBody()
        )

        viewModel.openPhotoCapture()
        viewModel.onNewPhotoCaptured(mockBitmap)
        advanceUntilIdle()

        viewModel.confirmPhotoReplacement()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReplacingPhoto)
        assertTrue(viewModel.uiState.value.photoChangeUsed)
        assertFalse(viewModel.uiState.value.canChangePhoto)
        assertTrue(viewModel.uiState.value.replacePhotoError?.contains("Photo change limit reached") == true)

        verify { tokenStorage.updatePhotoChangeUsed(true) }
        coVerify { userLocalDao.updatePhotoChangeUsed("EMP1001", true, any()) }
    }

    @Test
    fun `signOut clears session and navigates to Login`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.signOut()
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(ProfileNavigationEvent.NavigateToLogin, event)

            verify { tokenStorage.clearSession() }
            coVerify { userLocalDao.clearAll() }
        }
    }
}
