package com.company.attendance.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.calendar.model.CalendarDayUiModel
import com.company.attendance.feature.calendar.model.CalendarUiState
import com.company.attendance.feature.calendar.model.DayDetailUiState
import com.company.attendance.feature.calendar.model.MonthAttendanceSummary
import com.company.attendance.feature.calendar.util.CalendarDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val punchRepository: PunchRepository,
    private val punchDao: PunchDao,
    private val tokenStorage: TokenStorage,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val trustedTimeProvider: TrustedTimeProvider
) : ViewModel() {

    private val currentOfficeDate: LocalDate
        get() = trustedTimeProvider.currentTimeFlow.value.localDate

    private val _currentYearMonth = MutableStateFlow(YearMonth.from(currentOfficeDate))
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _selectedDayDetail = MutableStateFlow<DayDetailUiState?>(null)
    val selectedDayDetail: StateFlow<DayDetailUiState?> = _selectedDayDetail

    private val _isDetailSheetVisible = MutableStateFlow(false)
    val isDetailSheetVisible: StateFlow<Boolean> = _isDetailSheetVisible

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Observe active user session and year-month to trigger summary flows
    private val monthSummariesFlow = combine(
        tokenStorage.sessionFlow,
        _currentYearMonth
    ) { session, yearMonth ->
        Pair(session?.userId ?: tokenStorage.getUserId() ?: "", yearMonth)
    }.flatMapLatest { (userId, yearMonth) ->
        val ymString = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        punchRepository.observeMonthSummaries(userId, ymString)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentYearMonth,
        _selectedDate,
        _selectedDayDetail,
        _isDetailSheetVisible,
        monthSummariesFlow,
        remoteConfigRepository.observeConfig(),
        trustedTimeProvider.currentTimeFlow,
        _isLoading,
        _errorMessage
    ) { values ->
        val yearMonth = values[0] as YearMonth
        val selected = values[1] as LocalDate?
        val detail = values[2] as DayDetailUiState?
        val isSheetVisible = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val summaries = values[4] as List<DaySummary>
        val config = values[5] as com.company.attendance.core.model.AppConfig
        val trustedTime = values[6] as com.company.attendance.core.model.TrustedTime
        val loading = values[7] as Boolean
        val error = values[8] as String?

        val summariesMap = summaries.associateBy { it.dateLocal }

        val grid = CalendarDateUtils.generateCalendarGrid(
            yearMonth = yearMonth,
            today = trustedTime.localDate,
            selectedDate = selected,
            summariesMap = summariesMap
        )

        val completeCount = summaries.count { it.status == DayStatus.COMPLETE }
        val missedCount = summaries.count { it.status == DayStatus.MISSED_PUNCH }

        CalendarUiState(
            currentYearMonth = yearMonth,
            monthYearTitle = CalendarDateUtils.formatMonthYear(yearMonth),
            daysOfWeek = CalendarDateUtils.DAYS_OF_WEEK,
            calendarDays = grid,
            selectedDate = selected,
            selectedDayDetail = detail,
            isDetailSheetVisible = isSheetVisible,
            summaryStats = MonthAttendanceSummary(
                completeDays = completeCount,
                missedPunchDays = missedCount,
                totalRecordedDays = completeCount + missedCount
            ),
            officeTimezone = config.timezone,
            isLoading = loading,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(
            currentYearMonth = YearMonth.from(currentOfficeDate),
            monthYearTitle = CalendarDateUtils.formatMonthYear(YearMonth.from(currentOfficeDate)),
            daysOfWeek = CalendarDateUtils.DAYS_OF_WEEK,
            calendarDays = CalendarDateUtils.generateCalendarGrid(
                yearMonth = YearMonth.from(currentOfficeDate),
                today = currentOfficeDate,
                selectedDate = null,
                summariesMap = emptyMap()
            )
        )
    )

    fun onPreviousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun onTodayClick() {
        val today = currentOfficeDate
        _currentYearMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun onDaySelected(day: CalendarDayUiModel) {
        if (!day.isCurrentMonth) return

        _selectedDate.value = day.date
        val userId = tokenStorage.getUserId() ?: ""
        val dateString = day.date.toString()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val punches = if (userId.isNotEmpty()) {
                    punchDao.getPunchesForDateDirect(userId, dateString)
                } else {
                    emptyList()
                }

                val config = remoteConfigRepository.getConfig()
                val timezone = config.timezone

                val inTime = CalendarDateUtils.formatPunchTime(
                    epochMs = day.summary?.inTimeUtcEpochMs,
                    timezoneId = timezone
                )

                val outTime = CalendarDateUtils.formatPunchTime(
                    epochMs = day.summary?.outTimeUtcEpochMs,
                    timezoneId = timezone
                )

                val syncStatus = CalendarDateUtils.deriveSyncStatus(punches)

                _selectedDayDetail.value = DayDetailUiState(
                    date = day.date,
                    dateFormatted = CalendarDateUtils.formatDayDetailDate(day.date),
                    inTimeFormatted = inTime,
                    outTimeFormatted = outTime,
                    status = day.status,
                    timeUnverified = day.summary?.timeUnverified ?: false,
                    syncStatus = syncStatus
                )
                _isDetailSheetVisible.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load day details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDismissDetailSheet() {
        _isDetailSheetVisible.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
