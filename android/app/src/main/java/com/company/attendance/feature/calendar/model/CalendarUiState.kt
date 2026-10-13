package com.company.attendance.feature.calendar.model

import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.SyncStatus
import java.time.LocalDate
import java.time.YearMonth

/**
 * UI model representing a single cell in the calendar grid.
 */
data class CalendarDayUiModel(
    val date: LocalDate,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val status: DayStatus = DayStatus.NO_RECORD,
    val summary: DaySummary? = null
)

/**
 * UI state for the Day Detail bottom sheet.
 */
data class DayDetailUiState(
    val date: LocalDate,
    val dateFormatted: String, // e.g., "Wednesday, 30 September 2026"
    val inTimeFormatted: String?, // e.g., "09:15 AM" or null
    val outTimeFormatted: String?, // e.g., "06:30 PM" or null
    val status: DayStatus,
    val timeUnverified: Boolean = false,
    val syncStatus: SyncStatus? = null
)

/**
 * Monthly attendance summary counters.
 */
data class MonthAttendanceSummary(
    val completeDays: Int = 0,
    val missedPunchDays: Int = 0,
    val totalRecordedDays: Int = 0
)

/**
 * Overall UI State for the Calendar screen.
 */
data class CalendarUiState(
    val currentYearMonth: YearMonth,
    val monthYearTitle: String, // e.g., "September 2026"
    val daysOfWeek: List<String> = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    val calendarDays: List<CalendarDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedDayDetail: DayDetailUiState? = null,
    val isDetailSheetVisible: Boolean = false,
    val summaryStats: MonthAttendanceSummary = MonthAttendanceSummary(),
    val officeTimezone: String = "Asia/Kolkata",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
