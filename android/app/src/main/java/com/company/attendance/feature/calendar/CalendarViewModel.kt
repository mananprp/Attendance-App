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
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val punchRepository: PunchRepository,
    private val punchDao: PunchDao,
    private val tokenStorage: TokenStorage,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val trustedTimeProvider: TrustedTimeProvider,
    private val apiService: AttendanceApiService? = null
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

    private val _isMonthViewExpanded = MutableStateFlow(false)
    val isMonthViewExpanded: StateFlow<Boolean> = _isMonthViewExpanded

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    @Volatile
    private var cachedSummariesMap: Map<String, DaySummary> = emptyMap()

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
        _errorMessage,
        _isMonthViewExpanded
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
        val isMonthExpanded = values[9] as Boolean

        val summariesMap = summaries.associateBy { it.dateLocal }
        cachedSummariesMap = summariesMap

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
            errorMessage = error,
            isMonthViewExpanded = isMonthExpanded,
            summariesMap = summariesMap
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

    init {
        selectDate(currentOfficeDate, openSheet = false)
        refreshMonthFromApi(YearMonth.from(currentOfficeDate))
    }

    fun toggleMonthView() {
        _isMonthViewExpanded.value = !_isMonthViewExpanded.value
    }

    fun setMonthViewExpanded(expanded: Boolean) {
        _isMonthViewExpanded.value = expanded
    }

    fun onVisibleMonthChanged(yearMonth: YearMonth) {
        if (_currentYearMonth.value != yearMonth) {
            _currentYearMonth.value = yearMonth
            refreshMonthFromApi(yearMonth)
        }
    }

    fun onPreviousMonth() {
        val newYm = _currentYearMonth.value.minusMonths(1)
        _currentYearMonth.value = newYm
        refreshMonthFromApi(newYm)
    }

    fun onNextMonth() {
        val newYm = _currentYearMonth.value.plusMonths(1)
        _currentYearMonth.value = newYm
        refreshMonthFromApi(newYm)
    }

    fun refreshMonthFromApi(yearMonth: YearMonth) {
        val api = apiService ?: return
        viewModelScope.launch {
            runCatching {
                val ymStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val response = api.getMonthlyAttendance(ymStr)
                val parsed = NetworkHelper.parseResponse(response)
                parsed.onSuccess { attendanceDto ->
                    val userId = tokenStorage.getUserId() ?: ""
                    if (userId.isNotEmpty()) {
                        val summaries = attendanceDto.days.map { dayDto ->
                            DaySummary(
                                userId = userId,
                                dateLocal = dayDto.dateLocal,
                                inTimeUtcEpochMs = dayDto.inTime?.let { parseIsoToEpochMs(it) },
                                outTimeUtcEpochMs = dayDto.outTime?.let { parseIsoToEpochMs(it) },
                                status = runCatching { DayStatus.valueOf(dayDto.status) }.getOrDefault(DayStatus.NO_RECORD),
                                timeUnverified = dayDto.timeUnverified
                            )
                        }
                        punchRepository.upsertDaySummaries(summaries)
                    }
                }
            }
        }
    }

    private fun parseIsoToEpochMs(isoString: String): Long? {
        return runCatching {
            Instant.parse(isoString).toEpochMilli()
        }.getOrNull()
    }

    fun onTodayClick() {
        val today = currentOfficeDate
        _currentYearMonth.value = YearMonth.from(today)
        selectDate(today, openSheet = false)
    }

    fun openDetailSheet() {
        if (_selectedDayDetail.value != null) {
            _isDetailSheetVisible.value = true
        } else {
            val date = _selectedDate.value ?: currentOfficeDate
            selectDate(date, openSheet = true)
        }
    }

    fun selectDate(date: LocalDate, openSheet: Boolean = false) {
        _selectedDate.value = date
        val targetMonth = YearMonth.from(date)
        if (targetMonth != _currentYearMonth.value) {
            _currentYearMonth.value = targetMonth
        }

        val userId = tokenStorage.getUserId() ?: ""
        val dateString = date.toString()

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

                val summary = cachedSummariesMap[dateString]

                val hasRealOutPunch = punches.any { it.type == "OUT" }
                val rawInEpoch = summary?.inTimeUtcEpochMs ?: punches.firstOrNull { it.type == "IN" }?.punchTimeUtc
                val rawOutEpoch = summary?.outTimeUtcEpochMs ?: punches.lastOrNull { it.type == "OUT" }?.punchTimeUtc

                val isMissedOrUnclosed = (summary?.status == DayStatus.MISSED_PUNCH) ||
                    (rawInEpoch != null && rawOutEpoch == rawInEpoch && !hasRealOutPunch) ||
                    (date < currentOfficeDate && rawInEpoch != null && !hasRealOutPunch)

                val inTime = CalendarDateUtils.formatPunchTime(rawInEpoch, timezone)
                // For missed punch, checkout time should show the same time as check in!
                val outTime = if (isMissedOrUnclosed) inTime else CalendarDateUtils.formatPunchTime(rawOutEpoch, timezone)

                val status = when {
                    isMissedOrUnclosed -> DayStatus.MISSED_PUNCH
                    inTime != null && outTime != null -> DayStatus.COMPLETE
                    date == currentOfficeDate -> DayStatus.NO_RECORD
                    else -> summary?.status ?: DayStatus.NO_RECORD
                }

                val syncStatus = CalendarDateUtils.deriveSyncStatus(punches)
                val timeUnverified = summary?.timeUnverified ?: punches.any { it.timeUnverified }

                _selectedDayDetail.value = DayDetailUiState(
                    date = date,
                    dateFormatted = CalendarDateUtils.formatDayDetailDate(date),
                    inTimeFormatted = inTime,
                    outTimeFormatted = outTime,
                    status = status,
                    timeUnverified = timeUnverified,
                    syncStatus = syncStatus
                )
                if (openSheet) {
                    _isDetailSheetVisible.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load day details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDaySelected(day: CalendarDayUiModel) {
        if (!day.isCurrentMonth) return
        selectDate(day.date, openSheet = false)
    }

    fun onDismissDetailSheet() {
        _isDetailSheetVisible.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
