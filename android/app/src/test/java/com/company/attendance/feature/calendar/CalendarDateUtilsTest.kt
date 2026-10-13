package com.company.attendance.feature.calendar

import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.feature.calendar.util.CalendarDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarDateUtilsTest {

    @Test
    fun `formatMonthYear formats September 2026 correctly`() {
        val ym = YearMonth.of(2026, 9)
        val formatted = CalendarDateUtils.formatMonthYear(ym)
        assertEquals("September 2026", formatted)
    }

    @Test
    fun `formatDayDetailDate formats Wednesday 30 September 2026 correctly`() {
        val date = LocalDate.of(2026, 9, 30)
        val formatted = CalendarDateUtils.formatDayDetailDate(date)
        assertEquals("Wednesday, 30 September 2026", formatted)
    }

    @Test
    fun `formatPunchTime formats UTC epoch to office timezone 12-hour format`() {
        // 2026-09-30 03:45:00 UTC = 09:15:00 AM IST (Asia/Kolkata +05:30)
        val epochMs = Instant.parse("2026-09-30T03:45:00Z").toEpochMilli()
        val formatted = CalendarDateUtils.formatPunchTime(epochMs, "Asia/Kolkata")
        assertEquals("09:15 AM", formatted)

        // 2026-09-30 13:00:00 UTC = 06:30:00 PM IST (Asia/Kolkata +05:30)
        val outEpochMs = Instant.parse("2026-09-30T13:00:00Z").toEpochMilli()
        val formattedOut = CalendarDateUtils.formatPunchTime(outEpochMs, "Asia/Kolkata")
        assertEquals("06:30 PM", formattedOut)
    }

    @Test
    fun `formatPunchTime returns null for null or non-positive epoch`() {
        assertNull(CalendarDateUtils.formatPunchTime(null, "Asia/Kolkata"))
        assertNull(CalendarDateUtils.formatPunchTime(0L, "Asia/Kolkata"))
        assertNull(CalendarDateUtils.formatPunchTime(-100L, "Asia/Kolkata"))
    }

    @Test
    fun `generateCalendarGrid for September 2026 generates correct 35 or 42 grid cells`() {
        // September 2026 starts on Tuesday (Sept 1 is Tuesday).
        // For Sunday-start week: Sunday Aug 30 (leading 0), Monday Aug 31 (leading 1), Tuesday Sept 1 (day 1).
        // September has 30 days.
        // Total leading days = 2.
        // Days in month = 30.
        // Total = 32 -> Target 35 cells (trailing days = 3 from October).
        val ym = YearMonth.of(2026, 9)
        val today = LocalDate.of(2026, 9, 30)
        val selectedDate = LocalDate.of(2026, 9, 15)

        val summariesMap = mapOf(
            "2026-09-15" to DaySummary("EMP1001", "2026-09-15", 1000L, 2000L, DayStatus.COMPLETE),
            "2026-09-16" to DaySummary("EMP1001", "2026-09-16", 1000L, 1000L, DayStatus.MISSED_PUNCH)
        )

        val grid = CalendarDateUtils.generateCalendarGrid(ym, today, selectedDate, summariesMap)

        assertEquals(35, grid.size)

        // Leading days from August
        assertEquals(LocalDate.of(2026, 8, 30), grid[0].date)
        assertEquals(30, grid[0].dayNumber)
        assertFalse(grid[0].isCurrentMonth)

        assertEquals(LocalDate.of(2026, 8, 31), grid[1].date)
        assertEquals(31, grid[1].dayNumber)
        assertFalse(grid[1].isCurrentMonth)

        // Current month first day
        assertEquals(LocalDate.of(2026, 9, 1), grid[2].date)
        assertEquals(1, grid[2].dayNumber)
        assertTrue(grid[2].isCurrentMonth)

        // Selected day (Sept 15)
        val sept15 = grid.first { it.date == LocalDate.of(2026, 9, 15) }
        assertTrue(sept15.isSelected)
        assertTrue(sept15.isCurrentMonth)
        assertEquals(DayStatus.COMPLETE, sept15.status)

        // Missed punch day (Sept 16)
        val sept16 = grid.first { it.date == LocalDate.of(2026, 9, 16) }
        assertEquals(DayStatus.MISSED_PUNCH, sept16.status)

        // Today (Sept 30)
        val sept30 = grid.first { it.date == LocalDate.of(2026, 9, 30) }
        assertTrue(sept30.isToday)
        assertTrue(sept30.isCurrentMonth)

        // Trailing days from October (Oct 1, 2, 3)
        assertEquals(LocalDate.of(2026, 10, 1), grid[32].date)
        assertFalse(grid[32].isCurrentMonth)
        assertEquals(1, grid[32].dayNumber)

        assertEquals(LocalDate.of(2026, 10, 3), grid[34].date)
        assertFalse(grid[34].isCurrentMonth)
        assertEquals(3, grid[34].dayNumber)
    }

    @Test
    fun `generateCalendarGrid for month starting on Sunday has 0 leading days`() {
        // March 2026 starts on Sunday (March 1 is Sunday).
        val ym = YearMonth.of(2026, 3)
        val today = LocalDate.of(2026, 3, 15)

        val grid = CalendarDateUtils.generateCalendarGrid(ym, today, null, emptyMap())

        assertEquals(LocalDate.of(2026, 3, 1), grid[0].date)
        assertEquals(1, grid[0].dayNumber)
        assertTrue(grid[0].isCurrentMonth)
    }

    @Test
    fun `deriveSyncStatus accurately reflects pending, failed, or synced states`() {
        assertNull(CalendarDateUtils.deriveSyncStatus(emptyList()))

        val syncedPunch = createPunchEntity("1", SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, CalendarDateUtils.deriveSyncStatus(listOf(syncedPunch)))

        val pendingPunch = createPunchEntity("2", SyncStatus.PENDING)
        assertEquals(SyncStatus.PENDING, CalendarDateUtils.deriveSyncStatus(listOf(syncedPunch, pendingPunch)))

        val failedPunch = createPunchEntity("3", SyncStatus.FAILED)
        assertEquals(SyncStatus.FAILED, CalendarDateUtils.deriveSyncStatus(listOf(syncedPunch, pendingPunch, failedPunch)))
    }

    private fun createPunchEntity(uuid: String, syncStatus: SyncStatus): PunchEntity {
        return PunchEntity(
            punchUuid = uuid,
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            type = PunchType.IN.name,
            punchTimeUtc = 1727670000000L,
            timeSource = TimeSource.NTP.name,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 25.0f,
            matchScore = 0.88f,
            imagePath = "/images/punch.jpg",
            syncStatus = syncStatus.name,
            syncAttempts = 0,
            createdAt = 1727670000000L
        )
    }
}
