package com.company.attendance.core.time

import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.TimeSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrustedTimeProviderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val sntpClient: SntpClient = mockk()
    private val clockWrapper: ClockWrapper = mockk()
    private val remoteConfigRepository: RemoteConfigRepository = mockk()

    private lateinit var timeProvider: TrustedTimeProviderImpl

    private val sampleConfig = AppConfig(
        officeLat = 28.613939,
        officeLng = 77.209021,
        radiusMeters = 100,
        timezone = "Asia/Kolkata",
        syncWindows = listOf("16:00", "00:00"),
        syncJitterMinutes = 30,
        matchThreshold = 0.55f,
        lastFetchedAt = 0L
    )

    @Before
    fun setUp() {
        coEvery { remoteConfigRepository.getConfig() } returns sampleConfig
        every { remoteConfigRepository.observeConfig() } returns flowOf(sampleConfig)
    }

    @Test
    fun `when NTP sync succeeds, provides anchored monotonic time and source is NTP`() = testScope.runTest {
        val ntpEpochMs = 1727670000000L // 2024-09-30 04:20:00 UTC (09:50:00 IST)
        val initialElapsedMs = 10000L

        coEvery { sntpClient.fetchNtpTime(any()) } returns ntpEpochMs
        every { clockWrapper.elapsedRealtime() } returns initialElapsedMs
        every { clockWrapper.currentTimeMillis() } returns 1727000000000L // Tampered device clock

        timeProvider = TrustedTimeProviderImpl(
            sntpClient = sntpClient,
            clockWrapper = clockWrapper,
            remoteConfigRepository = remoteConfigRepository
        )

        val syncSuccess = timeProvider.syncNtp()
        assertTrue(syncSuccess)
        assertTrue(timeProvider.isNtpSynchronized())

        // Simulate 5000ms elapsed monotonic time passing while device clock is unchanged
        every { clockWrapper.elapsedRealtime() } returns (initialElapsedMs + 5000L)

        val trustedTime = timeProvider.getTrustedTime()
        assertEquals(ntpEpochMs + 5000L, trustedTime.epochMillis)
        assertEquals(TimeSource.NTP, trustedTime.source)
        assertFalse(trustedTime.isUnverified)
        assertEquals(LocalDate.of(2024, 9, 30), trustedTime.localDate)
        assertEquals("Asia/Kolkata", trustedTime.timezoneId)
    }

    @Test
    fun `when NTP sync fails and unanchored, falls back to device clock with unverified flag`() = testScope.runTest {
        val deviceEpochMs = 1727670000000L
        coEvery { sntpClient.fetchNtpTime(any()) } returns null
        every { clockWrapper.currentTimeMillis() } returns deviceEpochMs
        every { clockWrapper.elapsedRealtime() } returns 5000L

        timeProvider = TrustedTimeProviderImpl(
            sntpClient = sntpClient,
            clockWrapper = clockWrapper,
            remoteConfigRepository = remoteConfigRepository
        )

        val syncSuccess = timeProvider.syncNtp()
        assertFalse(syncSuccess)
        assertFalse(timeProvider.isNtpSynchronized())

        val trustedTime = timeProvider.getTrustedTime()
        assertEquals(deviceEpochMs, trustedTime.epochMillis)
        assertEquals(TimeSource.DEVICE, trustedTime.source)
        assertTrue(trustedTime.isUnverified)
    }

    @Test
    fun `timezone conversion correctly computes local date across midnight boundaries in Asia Kolkata`() = testScope.runTest {
        // 2024-09-30 18:29:00 UTC = 2024-09-30 23:59:00 IST (Date is Sep 30)
        val epoch2359 = 1727720940000L
        // 2024-09-30 18:31:00 UTC = 2024-10-01 00:01:00 IST (Date rolls to Oct 01)
        val epoch0001 = 1727721060000L

        coEvery { sntpClient.fetchNtpTime(any()) } returns null
        every { clockWrapper.elapsedRealtime() } returns 1000L
        every { clockWrapper.currentTimeMillis() } returns epoch2359

        timeProvider = TrustedTimeProviderImpl(
            sntpClient = sntpClient,
            clockWrapper = clockWrapper,
            remoteConfigRepository = remoteConfigRepository
        )

        val timeBeforeMidnight = timeProvider.getTrustedTime()
        assertEquals(LocalDate.of(2024, 9, 30), timeBeforeMidnight.localDate)

        every { clockWrapper.currentTimeMillis() } returns epoch0001
        val timeAfterMidnight = timeProvider.getTrustedTime()
        assertEquals(LocalDate.of(2024, 10, 1), timeAfterMidnight.localDate)
    }
}
