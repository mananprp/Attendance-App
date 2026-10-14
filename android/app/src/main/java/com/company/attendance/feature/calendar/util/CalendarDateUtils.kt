package com.company.attendance.feature.calendar.util

import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.feature.calendar.model.CalendarDayUiModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for calendar grid generation, date formatting,
 * and office timezone conversions.
 */
object CalendarDateUtils {

    val DAYS_OF_WEEK = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val DAY_DETAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)
    private val PUNCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    /**
     * Formats YearMonth to header display (e.g. "September 2026").
     */
    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(MONTH_YEAR_FORMATTER)
    }

    /**
     * Formats LocalDate to day detail header (e.g. "Wednesday, 30 September 2026").
     */
    fun formatDayDetailDate(date: LocalDate): String {
        return date.format(DAY_DETAIL_DATE_FORMATTER)
    }

    /**
     * Formats UTC epoch timestamp to 12-hour time (e.g. "09:15 AM") in the specified office timezone.
     */
    fun formatPunchTime(epochMs: Long?, timezoneId: String): String? {
        if (epochMs == null || epochMs <= 0) return null
        return try {
            val zoneId = ZoneId.of(timezoneId)
            Instant.ofEpochMilli(epochMs)
                .atZone(zoneId)
                .format(PUNCH_TIME_FORMATTER)
        } catch (_: Exception) {
            val fallbackZone = ZoneId.of("Asia/Kolkata")
            Instant.ofEpochMilli(epochMs)
                .atZone(fallbackZone)
                .format(PUNCH_TIME_FORMATTER)
        }
    }

    /**
     * Derives overall sync status for a date from its punches.
     */
    fun deriveSyncStatus(punches: List<PunchEntity>): SyncStatus? {
        if (punches.isEmpty()) return null
        if (punches.any { it.syncStatus == SyncStatus.FAILED.name }) return SyncStatus.FAILED
        if (punches.any { it.syncStatus == SyncStatus.PENDING.name }) return SyncStatus.PENDING
        return SyncStatus.SYNCED
    }

    /**
     * Generates a 7-column calendar grid (35 or 42 cells) for a given [YearMonth].
     *
     * Rules:
     * - Weeks start on Sunday (Sun, Mon, Tue, Wed, Thu, Fri, Sat).
     * - Leading days from previous month are flagged with isCurrentMonth = false.
     * - Days of the current month have isCurrentMonth = true.
     * - Trailing days to fill the rectangular grid are flagged with isCurrentMonth = false.
     */
    fun generateCalendarGrid(
        yearMonth: YearMonth,
        today: LocalDate,
        selectedDate: LocalDate?,
        summariesMap: Map<String, DaySummary>
    ): List<CalendarDayUiModel> {
        val days = mutableListOf<CalendarDayUiModel>()

        val firstDayOfMonth = yearMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek
        // In Java DayOfWeek: MONDAY is 1, SUNDAY is 7.
        // For Sunday-start week, Sunday is index 0, Monday is 1, ..., Saturday is 6.
        val leadingDaysCount = if (firstDayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfWeek.value

        // 1. Leading days from previous month
        if (leadingDaysCount > 0) {
            val prevMonth = yearMonth.minusMonths(1)
            val prevMonthLength = prevMonth.lengthOfMonth()
            for (i in (leadingDaysCount - 1) downTo 0) {
                val dayNumber = prevMonthLength - i
                val date = prevMonth.atDay(dayNumber)
                val summary = summariesMap[date.toString()]
                days.add(
                    CalendarDayUiModel(
                        date = date,
                        dayNumber = dayNumber,
                        isCurrentMonth = false,
                        isToday = (date == today),
                        isSelected = (date == selectedDate),
                        status = summary?.status ?: DayStatus.NO_RECORD,
                        summary = summary
                    )
                )
            }
        }

        // 2. Days of current month
        val currentMonthLength = yearMonth.lengthOfMonth()
        for (dayNumber in 1..currentMonthLength) {
            val date = yearMonth.atDay(dayNumber)
            val summary = summariesMap[date.toString()]
            val rawStatus = summary?.status ?: DayStatus.NO_RECORD
            val effectiveStatus = if (date == today && rawStatus == DayStatus.MISSED_PUNCH) {
                DayStatus.NO_RECORD
            } else {
                rawStatus
            }
            days.add(
                CalendarDayUiModel(
                    date = date,
                    dayNumber = dayNumber,
                    isCurrentMonth = true,
                    isToday = (date == today),
                    isSelected = (date == selectedDate),
                    status = effectiveStatus,
                    summary = summary
                )
            )
        }

        // 3. Trailing days from next month to complete 5 or 6 rows (35 or 42 cells)
        val totalAdded = days.size
        val targetCellCount = if (totalAdded <= 35) 35 else 42
        val trailingDaysCount = targetCellCount - totalAdded

        if (trailingDaysCount > 0) {
            val nextMonth = yearMonth.plusMonths(1)
            for (dayNumber in 1..trailingDaysCount) {
                val date = nextMonth.atDay(dayNumber)
                val summary = summariesMap[date.toString()]
                days.add(
                    CalendarDayUiModel(
                        date = date,
                        dayNumber = dayNumber,
                        isCurrentMonth = false,
                        isToday = (date == today),
                        isSelected = (date == selectedDate),
                        status = summary?.status ?: DayStatus.NO_RECORD,
                        summary = summary
                    )
                )
            }
        }

        return days
    }
}
