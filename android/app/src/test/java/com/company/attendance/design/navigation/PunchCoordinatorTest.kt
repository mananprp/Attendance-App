package com.company.attendance.design.navigation

import app.cash.turbine.test
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.location.LocationMonitor
import com.company.attendance.core.location.LocationStatus
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.UserSession
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.design.components.CenterPunchState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PunchCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)
    private val trustedTimeProvider: TrustedTimeProvider = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)
    private val locationChecker: LocationChecker = mockk(relaxed = true)
    private val locationMonitor: LocationMonitor = mockk(relaxed = true)

    private val sessionFlow = MutableStateFlow<UserSession?>(null)

    private lateinit var coordinator: PunchCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { tokenStorage.sessionFlow } returns sessionFlow
        every { tokenStorage.getUserId() } returns "user_123"
        coEvery { trustedTimeProvider.getOfficeLocalDate() } returns LocalDate.of(2026, 11, 5)

        coEvery { remoteConfigRepository.getConfig() } returns AppConfig(
            officeLat = 28.490293,
            officeLng = 77.060544,
            radiusMeters = 100,
            timezone = "Asia/Kolkata",
            syncWindows = listOf("16:00", "00:00"),
            syncJitterMinutes = 30,
            matchThreshold = 0.46f,
            lastFetchedAt = 0L
        )

        coordinator = PunchCoordinator(
            tokenStorage = tokenStorage,
            punchRepository = punchRepository,
            trustedTimeProvider = trustedTimeProvider,
            remoteConfigRepository = remoteConfigRepository,
            locationChecker = locationChecker,
            locationMonitor = locationMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateCenterState_mapsDaySummaryCorrectly() {
        // No punches
        coordinator.updateCenterState(null)
        assertEquals(CenterPunchState.PUNCH_IN, coordinator.centerPunchState.value)

        // In punch recorded
        val checkedInSummary = DaySummary(
            userId = "user_123",
            dateLocal = "2026-11-05",
            inTimeUtcEpochMs = 1000L,
            outTimeUtcEpochMs = null,
            status = DayStatus.NO_RECORD
        )
        coordinator.updateCenterState(checkedInSummary)
        assertEquals(CenterPunchState.PUNCH_OUT, coordinator.centerPunchState.value)

        // In and Out punches recorded
        val completedSummary = checkedInSummary.copy(outTimeUtcEpochMs = 5000L, status = DayStatus.COMPLETE)
        coordinator.updateCenterState(completedSummary)
        assertEquals(CenterPunchState.COMPLETE, coordinator.centerPunchState.value)
    }

    @Test
    fun triggerPunch_whenInsideGeofence_emitsNavigateToCaptureIn() = runTest(testDispatcher) {
        coEvery { punchRepository.getDaySummary("user_123", "2026-11-05") } returns null
        coEvery { locationMonitor.freshFixOrRefresh(any(), any()) } returns LocationStatus.Fix(
            latitude = 28.4903,
            longitude = 77.0605,
            accuracyMeters = 10f,
            distanceMeters = 15f,
            allowedRadiusMeters = 100,
            isInside = true,
            fixElapsedRealtimeMs = 1000L
        )

        coordinator.punchEvents.test {
            coordinator.triggerPunch()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is GlobalPunchEvent.NavigateToCapture)
            val captureEvent = event as GlobalPunchEvent.NavigateToCapture
            assertEquals(PunchType.IN, captureEvent.punchType)
            assertEquals("OFFICE", captureEvent.locationType)
            assertEquals(15f, captureEvent.distanceMeters, 0.01f)
        }
    }

    @Test
    fun triggerPunch_whenOutsideGeofence_emitsShowGeofenceError() = runTest(testDispatcher) {
        coEvery { punchRepository.getDaySummary("user_123", "2026-11-05") } returns null
        coEvery { locationMonitor.freshFixOrRefresh(any(), any()) } returns LocationStatus.Fix(
            latitude = 28.5500,
            longitude = 77.1000,
            accuracyMeters = 12f,
            distanceMeters = 850f,
            allowedRadiusMeters = 100,
            isInside = false,
            fixElapsedRealtimeMs = 1000L
        )

        coordinator.punchEvents.test {
            coordinator.triggerPunch()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is GlobalPunchEvent.ShowGeofenceError)
            val errorEvent = event as GlobalPunchEvent.ShowGeofenceError
            assertEquals("Outside Office Geofence", errorEvent.title)
        }
    }

    @Test
    fun triggerPunch_whenAlreadyCheckedIn_triggersPunchOut() = runTest(testDispatcher) {
        val checkedInSummary = DaySummary(
            userId = "user_123",
            dateLocal = "2026-11-05",
            inTimeUtcEpochMs = 1000L,
            outTimeUtcEpochMs = null,
            status = DayStatus.NO_RECORD
        )
        coEvery { punchRepository.getDaySummary("user_123", "2026-11-05") } returns checkedInSummary
        coEvery { locationMonitor.freshFixOrRefresh(any(), any()) } returns LocationStatus.Fix(
            latitude = 28.4903,
            longitude = 77.0605,
            accuracyMeters = 10f,
            distanceMeters = 10f,
            allowedRadiusMeters = 100,
            isInside = true,
            fixElapsedRealtimeMs = 1000L
        )

        coordinator.punchEvents.test {
            coordinator.triggerPunch()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is GlobalPunchEvent.NavigateToCapture)
            val captureEvent = event as GlobalPunchEvent.NavigateToCapture
            assertEquals(PunchType.OUT, captureEvent.punchType)
        }
    }
}
