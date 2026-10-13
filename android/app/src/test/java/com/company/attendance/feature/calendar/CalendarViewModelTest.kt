package com.company.attendance.feature.calendar

import app.cash.turbine.test
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.model.TrustedTime
import com.company.attendance.core.model.UserSession
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.calendar.model.CalendarDayUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val punchRepository: PunchRepository = mockk(relaxed = true)
    private val punchDao: PunchDao = mockk(relaxed = true)
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)
    private val trustedTimeProvider: TrustedTimeProvider = mockk(relaxed = true)

    private val testUserId = "EMP1001"
    private val testToday = LocalDate.of(2026, 9, 30)
    private val testTrustedTime = TrustedTime(
        epochMillis = Instant.parse("2026-09-30T09:15:00Z").toEpochMilli(),
        source = TimeSource.NTP,
        isUnverified = false,
        localDate = testToday,
        timezoneId = "Asia/Kolkata"
    )

    private val testConfig = AppConfig(
        officeLat = 28.6139,
        officeLng = 77.2090,
        radiusMeters = 100,
        timezone = "Asia/Kolkata",
        syncWindows = listOf("16:00", "00:00"),
        syncJitterMinutes = 30,
        matchThreshold = 0.55f
    )

    private val testSession = UserSession(
        userId = testUserId,
        displayName = "Alex Employee",
        token = "jwt.token.here",
        photoChangeUsed = false,
        hasPhoto = true,
        activePhotoVersion = 1
    )

    private val sessionFlow = MutableStateFlow<UserSession?>(testSession)
    private val timeFlow = MutableStateFlow(testTrustedTime)

    private val testSummaries = listOf(
        DaySummary(
            userId = testUserId,
            dateLocal = "2026-09-01",
            inTimeUtcEpochMs = Instant.parse("2026-09-01T03:30:00Z").toEpochMilli(), // 09:00 AM IST
            outTimeUtcEpochMs = Instant.parse("2026-09-01T12:30:00Z").toEpochMilli(), // 06:00 PM IST
            status = DayStatus.COMPLETE,
            timeUnverified = false
        ),
        DaySummary(
            userId = testUserId,
            dateLocal = "2026-09-02",
            inTimeUtcEpochMs = Instant.parse("2026-09-02T03:45:00Z").toEpochMilli(), // 09:15 AM IST
            outTimeUtcEpochMs = Instant.parse("2026-09-02T03:45:00Z").toEpochMilli(), // 09:15 AM IST (Missed punch)
            status = DayStatus.MISSED_PUNCH,
            timeUnverified = true
        )
    )

    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { tokenStorage.sessionFlow } returns sessionFlow
        every { tokenStorage.getUserId() } returns testUserId
        every { trustedTimeProvider.currentTimeFlow } returns timeFlow
        every { remoteConfigRepository.observeConfig() } returns flowOf(testConfig)
        coEvery { remoteConfigRepository.getConfig() } returns testConfig
        every { punchRepository.observeMonthSummaries(testUserId, "2026-09") } returns flowOf(testSummaries)
        every { punchRepository.observeMonthSummaries(testUserId, "2026-08") } returns flowOf(emptyList())
        every { punchRepository.observeMonthSummaries(testUserId, "2026-10") } returns flowOf(emptyList())

        viewModel = CalendarViewModel(
            punchRepository = punchRepository,
            punchDao = punchDao,
            tokenStorage = tokenStorage,
            remoteConfigRepository = remoteConfigRepository,
            trustedTimeProvider = trustedTimeProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState contains current month, today, days of week, and generated grid`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(YearMonth.of(2026, 9), state.currentYearMonth)
            assertEquals("September 2026", state.monthYearTitle)
            assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), state.daysOfWeek)
            assertFalse(state.isDetailSheetVisible)
            assertNull(state.selectedDayDetail)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observing month summaries computes stats and updates cell statuses`() = runTest {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(1, state.summaryStats.completeDays)
            assertEquals(1, state.summaryStats.missedPunchDays)
            assertEquals(2, state.summaryStats.totalRecordedDays)

            val sept1 = state.calendarDays.first { it.date == LocalDate.of(2026, 9, 1) }
            assertEquals(DayStatus.COMPLETE, sept1.status)

            val sept2 = state.calendarDays.first { it.date == LocalDate.of(2026, 9, 2) }
            assertEquals(DayStatus.MISSED_PUNCH, sept2.status)

            val sept3 = state.calendarDays.first { it.date == LocalDate.of(2026, 9, 3) }
            assertEquals(DayStatus.NO_RECORD, sept3.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPreviousMonth and onNextMonth update currentYearMonth and title`() = runTest {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onPreviousMonth()
            testDispatcher.scheduler.advanceUntilIdle()
            var state = expectMostRecentItem()
            assertEquals(YearMonth.of(2026, 8), state.currentYearMonth)
            assertEquals("August 2026", state.monthYearTitle)

            viewModel.onNextMonth()
            testDispatcher.scheduler.advanceUntilIdle()
            state = expectMostRecentItem()
            assertEquals(YearMonth.of(2026, 9), state.currentYearMonth)
            assertEquals("September 2026", state.monthYearTitle)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTodayClick jumps to today date and month`() = runTest {
        viewModel.onPreviousMonth()
        assertEquals(YearMonth.of(2026, 8), viewModel.currentYearMonth.value)

        viewModel.onTodayClick()
        assertEquals(YearMonth.of(2026, 9), viewModel.currentYearMonth.value)
        assertEquals(testToday, viewModel.selectedDate.value)
    }

    @Test
    fun `onDaySelected for current month day opens DayDetailBottomSheet with formatted times and sync status`() = runTest {
        val testPunches = listOf(
            PunchEntity(
                punchUuid = "punch-1",
                userId = testUserId,
                dateLocal = "2026-09-01",
                type = PunchType.IN.name,
                punchTimeUtc = Instant.parse("2026-09-01T03:30:00Z").toEpochMilli(),
                timeSource = TimeSource.NTP.name,
                timeUnverified = false,
                latitude = 28.6139,
                longitude = 77.2090,
                distanceMeters = 15f,
                matchScore = 0.92f,
                imagePath = "/images/1.jpg",
                syncStatus = SyncStatus.SYNCED.name,
                syncAttempts = 1,
                createdAt = 1727670000000L
            ),
            PunchEntity(
                punchUuid = "punch-2",
                userId = testUserId,
                dateLocal = "2026-09-01",
                type = PunchType.OUT.name,
                punchTimeUtc = Instant.parse("2026-09-01T12:30:00Z").toEpochMilli(),
                timeSource = TimeSource.NTP.name,
                timeUnverified = false,
                latitude = 28.6139,
                longitude = 77.2090,
                distanceMeters = 18f,
                matchScore = 0.89f,
                imagePath = "/images/2.jpg",
                syncStatus = SyncStatus.SYNCED.name,
                syncAttempts = 1,
                createdAt = 1727702400000L
            )
        )
        coEvery { punchDao.getPunchesForDateDirect(testUserId, "2026-09-01") } returns testPunches

        val dayModel = CalendarDayUiModel(
            date = LocalDate.of(2026, 9, 1),
            dayNumber = 1,
            isCurrentMonth = true,
            isToday = false,
            isSelected = false,
            status = DayStatus.COMPLETE,
            summary = testSummaries[0]
        )

        viewModel.onDaySelected(dayModel)
        testDispatcher.scheduler.advanceUntilIdle()

        val detail = viewModel.selectedDayDetail.value
        assertNotNull(detail)
        assertEquals("Tuesday, 1 September 2026", detail?.dateFormatted)
        assertEquals("09:00 AM", detail?.inTimeFormatted)
        assertEquals("06:00 PM", detail?.outTimeFormatted)
        assertEquals(DayStatus.COMPLETE, detail?.status)
        assertFalse(detail?.timeUnverified ?: true)
        assertEquals(SyncStatus.SYNCED, detail?.syncStatus)
        assertTrue(viewModel.isDetailSheetVisible.value)
    }

    @Test
    fun `onDaySelected for day with time_unverified sets flag in DayDetailUiState`() = runTest {
        coEvery { punchDao.getPunchesForDateDirect(testUserId, "2026-09-02") } returns listOf(
            PunchEntity(
                punchUuid = "punch-3",
                userId = testUserId,
                dateLocal = "2026-09-02",
                type = PunchType.IN.name,
                punchTimeUtc = Instant.parse("2026-09-02T03:45:00Z").toEpochMilli(),
                timeSource = TimeSource.DEVICE.name,
                timeUnverified = true,
                latitude = 28.6139,
                longitude = 77.2090,
                distanceMeters = 20f,
                matchScore = 0.85f,
                imagePath = "/images/3.jpg",
                syncStatus = SyncStatus.PENDING.name,
                syncAttempts = 0,
                createdAt = 1727670000000L
            )
        )

        val dayModel = CalendarDayUiModel(
            date = LocalDate.of(2026, 9, 2),
            dayNumber = 2,
            isCurrentMonth = true,
            isToday = false,
            isSelected = false,
            status = DayStatus.MISSED_PUNCH,
            summary = testSummaries[1]
        )

        viewModel.onDaySelected(dayModel)
        testDispatcher.scheduler.advanceUntilIdle()

        val detail = viewModel.selectedDayDetail.value
        assertNotNull(detail)
        assertTrue(detail?.timeUnverified ?: false)
        assertEquals(SyncStatus.PENDING, detail?.syncStatus)
        assertEquals(DayStatus.MISSED_PUNCH, detail?.status)
    }

    @Test
    fun `onDaySelected for non current month day does not open sheet`() = runTest {
        val nonCurrentDay = CalendarDayUiModel(
            date = LocalDate.of(2026, 8, 31),
            dayNumber = 31,
            isCurrentMonth = false,
            isToday = false,
            isSelected = false
        )

        viewModel.onDaySelected(nonCurrentDay)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isDetailSheetVisible.value)
        assertNull(viewModel.selectedDayDetail.value)
    }

    @Test
    fun `onDismissDetailSheet hides the bottom sheet`() = runTest {
        val dayModel = CalendarDayUiModel(
            date = LocalDate.of(2026, 9, 1),
            dayNumber = 1,
            isCurrentMonth = true,
            isToday = false,
            isSelected = false,
            status = DayStatus.COMPLETE,
            summary = testSummaries[0]
        )
        viewModel.onDaySelected(dayModel)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isDetailSheetVisible.value)

        viewModel.onDismissDetailSheet()
        assertFalse(viewModel.isDetailSheetVisible.value)
    }
}
