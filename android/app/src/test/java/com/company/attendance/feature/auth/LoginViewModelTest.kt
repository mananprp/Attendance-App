package com.company.attendance.feature.auth

import app.cash.turbine.test
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.UserSession
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.DaySummaryDto
import com.company.attendance.core.network.model.LoginRequestDto
import com.company.attendance.core.network.model.LoginResponseDto
import com.company.attendance.core.network.model.MonthlyAttendanceResponseDto
import com.company.attendance.core.network.model.UserProfileDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.auth.model.LoginNavigationEvent
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: android.content.Context = mockk(relaxed = true)
    private val okHttpClient: okhttp3.OkHttpClient = mockk(relaxed = true)
    private val faceVerifier: com.company.attendance.core.interfaces.FaceVerifier = mockk(relaxed = true)
    private val apiService: AttendanceApiService = mockk()
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val userLocalDao: UserLocalDao = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { apiService.getActivePhoto() } returns Response.success(mockk(relaxed = true))
        viewModel = LoginViewModel(
            context = context,
            apiService = apiService,
            okHttpClient = okHttpClient,
            tokenStorage = tokenStorage,
            userLocalDao = userLocalDao,
            faceVerifier = faceVerifier,
            remoteConfigRepository = remoteConfigRepository,
            punchRepository = punchRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onUserIdChanged updates userId and clears error`() {
        viewModel.onUserIdChanged("EMP1001")
        assertEquals("EMP1001", viewModel.uiState.value.userId)
        assertNull(viewModel.uiState.value.userIdError)
    }

    @Test
    fun `onPasswordChanged updates password and clears error`() {
        viewModel.onPasswordChanged("password123")
        assertEquals("password123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `login with blank userId sets userIdError`() {
        viewModel.onUserIdChanged("")
        viewModel.onPasswordChanged("password123")

        viewModel.login()

        assertNotNull(viewModel.uiState.value.userIdError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login with blank password sets passwordError`() {
        viewModel.onUserIdChanged("EMP1001")
        viewModel.onPasswordChanged("")

        viewModel.login()

        assertNotNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful login with existing photo navigates to Home and fetches history`() = runTest {
        val userDto = UserProfileDto(
            userId = "EMP1001",
            displayName = "Alice Smith",
            photoChangeUsed = false,
            hasPhoto = true,
            activePhotoVersion = 1
        )
        val loginResponse = LoginResponseDto(token = "jwt-test-token", user = userDto)

        val historyResponse = MonthlyAttendanceResponseDto(
            month = "2026-09",
            days = listOf(
                DaySummaryDto(
                    dateLocal = "2026-09-29",
                    inTime = "2026-09-29T09:00:00Z",
                    outTime = "2026-09-29T17:30:00Z",
                    status = "COMPLETE",
                    timeUnverified = false
                )
            ),
            punches = emptyList()
        )

        coEvery { apiService.login(any()) } returns Response.success(loginResponse)
        coEvery { apiService.getMonthlyAttendance(any()) } returns Response.success(historyResponse)
        coEvery { remoteConfigRepository.fetchAndCacheConfig() } returns Result.success(mockk(relaxed = true))

        viewModel.onUserIdChanged("EMP1001")
        viewModel.onPasswordChanged("secret")

        viewModel.navigationEvent.test {
            viewModel.login()
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(LoginNavigationEvent.NavigateToHome, event)

            val sessionSlot = slot<UserSession>()
            verify { tokenStorage.saveSession(capture(sessionSlot)) }
            assertEquals("EMP1001", sessionSlot.captured.userId)
            assertEquals("jwt-test-token", sessionSlot.captured.token)
            assertTrue(sessionSlot.captured.hasPhoto)

            val userEntitySlot = slot<UserLocalEntity>()
            coVerify { userLocalDao.insertOrUpdate(capture(userEntitySlot)) }
            assertEquals("EMP1001", userEntitySlot.captured.userId)

            coVerify { remoteConfigRepository.fetchAndCacheConfig() }

            val summariesList = mutableListOf<List<DaySummary>>()
            coVerify(atLeast = 1) { punchRepository.upsertDaySummaries(capture(summariesList)) }
            assertTrue(summariesList.isNotEmpty())
            assertTrue(summariesList.first().isNotEmpty())
            assertEquals("2026-09-29", summariesList.first()[0].dateLocal)
            assertEquals(DayStatus.COMPLETE, summariesList.first()[0].status)
        }
    }

    @Test
    fun `successful login without photo navigates to Onboarding`() = runTest {
        val userDto = UserProfileDto(
            userId = "EMP1002",
            displayName = "Bob Jones",
            photoChangeUsed = false,
            hasPhoto = false,
            activePhotoVersion = null
        )
        val loginResponse = LoginResponseDto(token = "jwt-test-token-2", user = userDto)

        coEvery { apiService.login(any()) } returns Response.success(loginResponse)
        coEvery { remoteConfigRepository.fetchAndCacheConfig() } returns Result.success(mockk(relaxed = true))

        viewModel.onUserIdChanged("EMP1002")
        viewModel.onPasswordChanged("password")

        viewModel.navigationEvent.test {
            viewModel.login()
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(LoginNavigationEvent.NavigateToOnboarding, event)

            val sessionSlot = slot<UserSession>()
            verify { tokenStorage.saveSession(capture(sessionSlot)) }
            assertEquals("EMP1002", sessionSlot.captured.userId)
            assertFalse(sessionSlot.captured.hasPhoto)

            coVerify(exactly = 0) { punchRepository.upsertDaySummaries(any()) }
        }
    }

    @Test
    fun `login network error sets generalError message`() = runTest {
        coEvery { apiService.login(any()) } returns Response.error(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid user ID or password\"}".toResponseBody())

        viewModel.onUserIdChanged("EMP1001")
        viewModel.onPasswordChanged("wrongpassword")

        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.generalError?.contains("Invalid user ID") == true)
    }
}
