package com.company.attendance.core.repository

import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.model.TrustedTime
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PunchRepositoryTest {

    private val punchDao: PunchDao = mockk(relaxed = true)
    private val daySummaryDao: DaySummaryDao = mockk(relaxed = true)

    private lateinit var repository: PunchRepositoryImpl

    private val testUserId = "EMP1001"
    private val testDate = LocalDate.of(2024, 9, 30)
    private val testTrustedTime = TrustedTime(
        epochMillis = 1727670000000L,
        source = TimeSource.NTP,
        isUnverified = false,
        localDate = testDate,
        timezoneId = "Asia/Kolkata"
    )

    @Before
    fun setUp() {
        repository = PunchRepositoryImpl(punchDao, daySummaryDao)
    }

    @Test
    fun `recordPunch IN creates pending punch and day summary record`() = runTest {
        coEvery { daySummaryDao.getDaySummaryDirect(testUserId, "2024-09-30") } returns null
        coEvery { daySummaryDao.getUnclosedSummariesBeforeDate(testUserId, "2024-09-30") } returns emptyList()

        val punchSlot = slot<PunchEntity>()
        val summarySlot = slot<DaySummaryEntity>()
        coEvery { punchDao.insertPunch(capture(punchSlot)) } returns Unit
        coEvery { daySummaryDao.insertOrUpdate(capture(summarySlot)) } returns Unit

        val result = repository.recordPunch(
            userId = testUserId,
            punchType = PunchType.IN,
            time = testTrustedTime,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            capturedImagePath = "/data/user/0/com.company.attendance/punch_in.jpg"
        )

        assertTrue(result.isSuccess)
        val punch = result.getOrNull()
        assertNotNull(punch)
        assertEquals(testUserId, punch?.userId)
        assertEquals(PunchType.IN, punch?.type)
        assertEquals(SyncStatus.PENDING, punch?.syncStatus)

        // Verify captured PunchEntity
        assertEquals("IN", punchSlot.captured.type)
        assertEquals(1727670000000L, punchSlot.captured.punchTimeUtc)

        // Verify captured DaySummaryEntity
        assertEquals(testUserId, summarySlot.captured.userId)
        assertEquals("2024-09-30", summarySlot.captured.dateLocal)
        assertEquals(1727670000000L, summarySlot.captured.inTimeUtc)
        assertEquals(null, summarySlot.captured.outTimeUtc)
        assertEquals(DayStatus.NO_RECORD.name, summarySlot.captured.status)
    }

    @Test
    fun `recordPunch OUT updates day summary to COMPLETE with outTime`() = runTest {
        val existingSummary = DaySummaryEntity(
            userId = testUserId,
            dateLocal = "2024-09-30",
            inTimeUtc = 1727670000000L,
            outTimeUtc = null,
            status = DayStatus.NO_RECORD.name,
            timeUnverified = false
        )
        coEvery { daySummaryDao.getDaySummaryDirect(testUserId, "2024-09-30") } returns existingSummary
        coEvery { daySummaryDao.getUnclosedSummariesBeforeDate(testUserId, "2024-09-30") } returns emptyList()

        val summarySlot = slot<DaySummaryEntity>()
        coEvery { daySummaryDao.insertOrUpdate(capture(summarySlot)) } returns Unit

        val outTime = testTrustedTime.copy(epochMillis = 1727700000000L)
        val result = repository.recordPunch(
            userId = testUserId,
            punchType = PunchType.OUT,
            time = outTime,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 20f,
            matchScore = 0.88f,
            capturedImagePath = "/data/user/0/com.company.attendance/punch_out.jpg"
        )

        assertTrue(result.isSuccess)
        assertEquals(1727670000000L, summarySlot.captured.inTimeUtc)
        assertEquals(1727700000000L, summarySlot.captured.outTimeUtc)
        assertEquals(DayStatus.COMPLETE.name, summarySlot.captured.status)
    }

    @Test
    fun `resolvePastMissedPunches marks earlier unclosed days as MISSED_PUNCH with outTime equal to inTime`() = runTest {
        val pastUnclosedDay = DaySummaryEntity(
            userId = testUserId,
            dateLocal = "2024-09-29",
            inTimeUtc = 1727583600000L, // 2024-09-29 in_time
            outTimeUtc = null,
            status = DayStatus.NO_RECORD.name,
            timeUnverified = false
        )

        coEvery {
            daySummaryDao.getUnclosedSummariesBeforeDate(testUserId, "2024-09-30")
        } returns listOf(pastUnclosedDay)

        val updatedSlot = slot<DaySummaryEntity>()
        coEvery { daySummaryDao.insertOrUpdate(capture(updatedSlot)) } returns Unit

        repository.resolvePastMissedPunches(testUserId, "2024-09-30")

        coVerify(exactly = 1) { daySummaryDao.insertOrUpdate(any()) }
        assertEquals("2024-09-29", updatedSlot.captured.dateLocal)
        assertEquals(1727583600000L, updatedSlot.captured.inTimeUtc)
        assertEquals(1727583600000L, updatedSlot.captured.outTimeUtc) // out_time = in_time rule
        assertEquals(DayStatus.MISSED_PUNCH.name, updatedSlot.captured.status)
    }

    @Test
    fun `markPunchesSynced calls dao markPunchesSynced`() = runTest {
        val uuids = listOf("uuid-1", "uuid-2")
        repository.markPunchesSynced(uuids)
        coVerify(exactly = 1) { punchDao.markPunchesSynced(uuids) }
    }

    @Test
    fun `markPunchSyncFailed updates sync status to FAILED with attempt count`() = runTest {
        repository.markPunchSyncFailed("uuid-123", 3)
        coVerify(exactly = 1) { punchDao.updateSyncStatus("uuid-123", SyncStatus.FAILED.name, 3) }
    }
}
