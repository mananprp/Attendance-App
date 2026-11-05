package com.company.attendance.feature.punch

import app.cash.turbine.test
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.GeofenceResult
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.interfaces.ManualSyncResult
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.core.interfaces.SyncUiState
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.model.TrustedTime
import com.company.attendance.core.model.UserSession
import com.company.attendance.design.components.CenterPunchState
import com.company.attendance.design.components.WorkRingState
import com.company.attendance.core.storage.TokenStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val trustedTimeProvider: TrustedTimeProvider = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)
    private val locationChecker: LocationChecker = mockk(relaxed = true)
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)
    private val userLocalDao: com.company.attendance.core.database.dao.UserLocalDao = mockk(relaxed = true)
    private val apiService: com.company.attendance.core.network.AttendanceApiService = mockk(relaxed = true)
    private val okHttpClient: okhttp3.OkHttpClient = mockk(relaxed = true)
    private val locationMonitor: com.company.attendance.core.location.LocationMonitor = mockk(relaxed = true)
    private val faceVerifier: com.company.attendance.core.interfaces.FaceVerifier = mockk(relaxed = true)

    private fun createViewModel() = HomeViewModel(
        context = context,
        tokenStorage = tokenStorage,
        userLocalDao = userLocalDao,
        apiService = apiService,
        okHttpClient = okHttpClient,
        trustedTimeProvider = trustedTimeProvider,
        remoteConfigRepository = remoteConfigRepository,
        punchRepository = punchRepository,
        locationChecker = locationChecker,
        locationMonitor = locationMonitor,
        syncScheduler = syncScheduler,
        faceVerifier = faceVerifier
    )

    private val timeFlow = MutableStateFlow(createTrustedTime(2026, 9, 30, 9, 30, 0))
    private val syncStateFlow = MutableStateFlow(SyncUiState())
    private val daySummaryFlow = MutableStateFlow<DaySummary?>(null)

    private val testConfig = AppConfig(
        officeLat = 12.9716,
        officeLng = 77.5946,
        radiusMeters = 100,
        timezone = "Asia/Kolkata",
        syncWindows = listOf("16:00", "00:00"),
        syncJitterMinutes = 30,
        matchThreshold = 0.55f
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { tokenStorage.getUserSession() } returns UserSession(
            userId = "EMP1001",
            displayName = "Alice Smith",
            token = "jwt_token_alice",
            photoChangeUsed = false,
            hasPhoto = true
        )
        every { tokenStorage.getUserId() } returns "EMP1001"

        every { trustedTimeProvider.currentTimeFlow } returns timeFlow
        coEvery { trustedTimeProvider.getTrustedTime() } answers { timeFlow.value }
        coEvery { trustedTimeProvider.getOfficeLocalDate() } answers { timeFlow.value.localDate }

        coEvery { remoteConfigRepository.getConfig() } returns testConfig
        coEvery { remoteConfigRepository.fetchAndCacheConfig() } returns Result.success(testConfig)
        every { syncScheduler.syncState } returns syncStateFlow
        every { punchRepository.observeDaySummary("EMP1001", any()) } returns daySummaryFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTrustedTime(
        year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int,
        isUnverified: Boolean = false
    ): TrustedTime {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val zdt = ZonedDateTime.of(year, month, day, hour, minute, second, 0, zoneId)
        return TrustedTime(
            epochMillis = zdt.toInstant().toEpochMilli(),
            source = if (isUnverified) TimeSource.DEVICE else TimeSource.NTP,
            isUnverified = isUnverified,
            localDate = LocalDate.of(year, month, day),
            timezoneId = "Asia/Kolkata"
        )
    }

    @Test
    fun `initial state before punch in has punch in enabled and punch out disabled`() = runTest {
        daySummaryFlow.value = null
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
                assertEquals("Alice Smith", state.displayName)
        assertEquals("EMP1001", state.userId)
        assertTrue(state.isPunchInEnabled)
        assertFalse(state.isPunchInBlocked)
        assertEquals("Punch In", state.punchInButtonText)

        assertFalse(state.isPunchOutEnabled)
        assertFalse(state.isPunchOutBlocked)
        assertEquals("Punch Out", state.punchOutButtonText)
        assertNull(state.inTimeFormatted)
        assertNull(state.outTimeFormatted)
    }

    @Test
    fun `after punch in, punch in is blocked with time and punch out is enabled`() = runTest {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val inTimeEpoch = ZonedDateTime.of(2026, 9, 30, 9, 15, 0, 0, zoneId).toInstant().toEpochMilli()

        daySummaryFlow.value = DaySummary(
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            inTimeUtcEpochMs = inTimeEpoch,
            outTimeUtcEpochMs = null,
            status = DayStatus.NO_RECORD
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.inTimeFormatted)
        assertNull(state.outTimeFormatted)
        assertFalse(state.isPunchInEnabled)
        assertTrue(state.isPunchInBlocked)
        assertTrue(state.punchInButtonText.contains("Punched In at"))

        assertTrue(state.isPunchOutEnabled)
        assertFalse(state.isPunchOutBlocked)
        assertEquals("Punch Out", state.punchOutButtonText)
    }

    @Test
    fun `after punch out, both buttons are blocked with times`() = runTest {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val inTimeEpoch = ZonedDateTime.of(2026, 9, 30, 9, 15, 0, 0, zoneId).toInstant().toEpochMilli()
        val outTimeEpoch = ZonedDateTime.of(2026, 9, 30, 18, 30, 0, 0, zoneId).toInstant().toEpochMilli()

        daySummaryFlow.value = DaySummary(
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            inTimeUtcEpochMs = inTimeEpoch,
            outTimeUtcEpochMs = outTimeEpoch,
            status = DayStatus.COMPLETE
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.inTimeFormatted)
        assertNotNull(state.outTimeFormatted)
        assertFalse(state.isPunchInEnabled)
        assertTrue(state.isPunchInBlocked)
        assertFalse(state.isPunchOutEnabled)
        assertTrue(state.isPunchOutBlocked)
        assertTrue(state.punchInButtonText.contains("Punched In at"))
        assertTrue(state.punchOutButtonText.contains("Punched Out at"))
    }

    @Test
    fun `midnight rollover resets button states for next day`() = runTest {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val inTimeEpoch = ZonedDateTime.of(2026, 9, 30, 9, 15, 0, 0, zoneId).toInstant().toEpochMilli()
        val outTimeEpoch = ZonedDateTime.of(2026, 9, 30, 18, 30, 0, 0, zoneId).toInstant().toEpochMilli()

        // Sep 30: fully punched
        daySummaryFlow.value = DaySummary(
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            inTimeUtcEpochMs = inTimeEpoch,
            outTimeUtcEpochMs = outTimeEpoch,
            status = DayStatus.COMPLETE
        )

        val nextDayFlow = MutableStateFlow<DaySummary?>(null)
        every { punchRepository.observeDaySummary("EMP1001", "2026-10-01") } returns nextDayFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Verify day 30 state
        assertTrue(viewModel.uiState.value.isPunchInBlocked)

        // Advance clock across midnight to Oct 01, 00:01 AM
        timeFlow.value = createTrustedTime(2026, 10, 1, 0, 1, 0)
        advanceUntilIdle()

        // Buttons reset for the new day
        val state = viewModel.uiState.value
        assertTrue(state.isPunchInEnabled)
        assertFalse(state.isPunchInBlocked)
        assertFalse(state.isPunchOutEnabled)
        assertFalse(state.isPunchOutBlocked)
        assertNull(state.inTimeFormatted)
        assertNull(state.outTimeFormatted)
    }

    @Test
    fun `greeting text matches time of day`() = runTest {
        val viewModel = createViewModel()

        // Morning (08:00)
        timeFlow.value = createTrustedTime(2026, 9, 30, 8, 0, 0)
        advanceUntilIdle()
        assertEquals("Good morning", viewModel.uiState.value.greeting)

        // Afternoon (14:30)
        timeFlow.value = createTrustedTime(2026, 9, 30, 14, 30, 0)
        advanceUntilIdle()
        assertEquals("Good afternoon", viewModel.uiState.value.greeting)

        // Evening (19:45)
        timeFlow.value = createTrustedTime(2026, 9, 30, 19, 45, 0)
        advanceUntilIdle()
        assertEquals("Good evening", viewModel.uiState.value.greeting)
    }

    @Test
    fun `geofence inside triggers navigation to punch capture and resolves past missed punches`() = runTest {
        coEvery {
            locationMonitor.freshFixOrRefresh(any(), any())
        } returns com.company.attendance.core.location.LocationStatus.Fix(
            latitude = 12.9718,
            longitude = 77.5947,
            accuracyMeters = 10f,
            distanceMeters = 25f,
            allowedRadiusMeters = 100,
            isInside = true,
            fixElapsedRealtimeMs = System.currentTimeMillis(),
            fixTimestampMs = System.currentTimeMillis()
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.onPunchInClicked()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeNavigationEvent.NavigateToPunchCapture)
            val captureEvent = event as HomeNavigationEvent.NavigateToPunchCapture
            assertEquals(PunchType.IN, captureEvent.punchType)
            assertEquals(25f, captureEvent.distanceMeters, 0.01f)
            assertEquals(12.9718, captureEvent.latitude, 0.0001)

            // Verified that past unclosed days were resolved
            coVerify { punchRepository.resolvePastMissedPunches("EMP1001", "2026-09-30") }
        }
    }

    @Test
    fun `geofence outside shows outside radius alert dialog with distance`() = runTest {
        coEvery {
            locationMonitor.freshFixOrRefresh(any(), any())
        } returns com.company.attendance.core.location.LocationStatus.Fix(
            latitude = 12.9750,
            longitude = 77.5990,
            accuracyMeters = 10f,
            distanceMeters = 350f,
            allowedRadiusMeters = 100,
            isInside = false,
            fixElapsedRealtimeMs = System.currentTimeMillis(),
            fixTimestampMs = System.currentTimeMillis()
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPunchInClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.geofenceDialogState)
        assertTrue(state.geofenceDialogState is GeofenceDialogState.OutsideRadius)
        val dialog = state.geofenceDialogState as GeofenceDialogState.OutsideRadius
        assertEquals(350f, dialog.distanceMeters, 0.1f)
        assertEquals(100, dialog.allowedRadiusMeters)
        assertTrue(dialog.formattedMessage.contains("350m"))
    }

    @Test
    fun `geofence permission denied shows permission dialog`() = runTest {
        coEvery {
            locationMonitor.freshFixOrRefresh(any(), any())
        } returns com.company.attendance.core.location.LocationStatus.PermissionDenied

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPunchInClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GeofenceDialogState.PermissionRequired, state.geofenceDialogState)
    }

    @Test
    fun `geofence location services disabled shows disabled dialog`() = runTest {
        coEvery {
            locationMonitor.freshFixOrRefresh(any(), any())
        } returns com.company.attendance.core.location.LocationStatus.ServicesDisabled

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPunchInClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GeofenceDialogState.LocationServicesDisabled, state.geofenceDialogState)
    }

    @Test
    fun `sync now button triggers sync scheduler`() = runTest {
        coEvery { syncScheduler.triggerSyncNow() } returns ManualSyncResult.Success(2)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSyncNowClicked()
        advanceUntilIdle()

        coVerify { syncScheduler.triggerSyncNow() }
    }
    @Test
    fun `ring and center button state transitions`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 1. Initial / Not started
        assertEquals(WorkRingState.NotStarted, viewModel.uiState.value.ringState)
        assertEquals(CenterPunchState.PUNCH_IN, viewModel.uiState.value.centerPunchState)

        // 2. Punched in -> InProgress & PUNCH_OUT
        val inEpoch = ZonedDateTime.of(2026, 9, 30, 9, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        daySummaryFlow.value = DaySummary(
            dateLocal = "2026-09-30",
            userId = "EMP1001",
            shiftType = com.company.attendance.core.model.ShiftType.GENERAL,
            inTimeUtcEpochMs = inEpoch,
            outTimeUtcEpochMs = null,
            status = DayStatus.NO_RECORD
        )
        advanceUntilIdle()
        assertEquals(WorkRingState.InProgress, viewModel.uiState.value.ringState)
        assertEquals(CenterPunchState.PUNCH_OUT, viewModel.uiState.value.centerPunchState)

        // 3. Punched out -> Complete & COMPLETE
        val outEpoch = ZonedDateTime.of(2026, 9, 30, 18, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        daySummaryFlow.value = DaySummary(
            dateLocal = "2026-09-30",
            userId = "EMP1001",
            shiftType = com.company.attendance.core.model.ShiftType.GENERAL,
            inTimeUtcEpochMs = inEpoch,
            outTimeUtcEpochMs = outEpoch,
            status = DayStatus.COMPLETE
        )
        advanceUntilIdle()
        assertEquals(WorkRingState.Complete, viewModel.uiState.value.ringState)
        assertEquals(CenterPunchState.COMPLETE, viewModel.uiState.value.centerPunchState)
    }

    @Test
    fun `total attended days updates from month summaries`() = runTest {
        val summaries = listOf(
            DaySummary(
                dateLocal = "2026-09-01",
                userId = "EMP1001",
                shiftType = com.company.attendance.core.model.ShiftType.GENERAL,
                inTimeUtcEpochMs = 1700000000L,
                outTimeUtcEpochMs = null,
                status = DayStatus.COMPLETE
            ),
            DaySummary(
                dateLocal = "2026-09-02",
                userId = "EMP1001",
                shiftType = com.company.attendance.core.model.ShiftType.GENERAL,
                inTimeUtcEpochMs = 1700100000L,
                outTimeUtcEpochMs = null,
                status = DayStatus.COMPLETE
            )
        )
        every { punchRepository.observeMonthSummaries("EMP1001", "2026-09") } returns flowOf(summaries)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.totalAttendedDays)
    }
}
