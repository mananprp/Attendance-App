package com.company.attendance.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.R
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.design.components.CalendarDayCell
import com.company.attendance.design.components.DayStatusChip
import com.company.attendance.design.components.ErrorStateWithRetry
import com.company.attendance.design.components.IconCircleButton
import com.company.attendance.design.components.SectionHeader
import com.company.attendance.design.components.TimeUnverifiedBadge
import com.company.attendance.design.components.WeekDayItem
import com.company.attendance.design.components.WeekStrip
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AttendanceTheme
import com.company.attendance.feature.calendar.detail.DayDetailBottomSheet
import com.company.attendance.feature.calendar.model.CalendarDayUiModel
import com.company.attendance.feature.calendar.model.CalendarUiState
import com.company.attendance.feature.calendar.model.DayDetailUiState
import com.company.attendance.feature.calendar.model.MonthAttendanceSummary
import com.company.attendance.feature.calendar.util.CalendarDateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarScreen: Stateful entry point for Findez-style Attendance Calendar.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CalendarScreenContent(
        uiState = uiState,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
        onDayClick = viewModel::onDaySelected,
        onDismissDetail = viewModel::onDismissDetailSheet,
        onRetryError = viewModel::clearError,
        onToggleMonthView = viewModel::toggleMonthView,
        onDateSelected = { date -> viewModel.selectDate(date, openSheet = false) },
        onSeeMoreClick = viewModel::openDetailSheet,
        modifier = modifier
    )
}

/**
 * CalendarScreenContent: Stateless content for Monthly/Weekly Attendance Calendar.
 */
@Composable
fun CalendarScreenContent(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDayClick: (CalendarDayUiModel) -> Unit,
    onDismissDetail: () -> Unit,
    onRetryError: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleMonthView: () -> Unit = {},
    onDateSelected: (LocalDate) -> Unit = {},
    onSeeMoreClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val today = remember(uiState.calendarDays) {
        uiState.calendarDays.firstOrNull { it.isToday }?.date ?: LocalDate.now()
    }
    val activeSelectedDate = uiState.selectedDate ?: today

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = AppDimensions.ScreenPadding)
        ) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // 1. Header Zone: Left circular 4-dots button | Center title + subtitle | Right circular "today" button
            CalendarHeaderZone(
                monthYearTitle = uiState.monthYearTitle,
                selectedDate = activeSelectedDate,
                onToggleMonthView = onToggleMonthView,
                onTodayClick = onTodayClick
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // 2. Week Strip OR Month Grid depending on isMonthViewExpanded
            if (uiState.isMonthViewExpanded) {
                // Expanded Month Grid
                MonthGridCard(
                    uiState = uiState,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onDayClick = onDayClick
                )
            } else {
                // Collapsed Week Strip (horizontally scrollable weeks without hard limits)
                HorizontallyScrollableWeekStrip(
                    today = today,
                    selectedDate = activeSelectedDate,
                    summariesMap = uiState.summariesMap,
                    onDateSelected = onDateSelected
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

            // 3. Your Attendance Section (Header with "See more" + Check In & Check Out Cards)
            SectionHeader(
                title = "Your Attendance"
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

            SelectedDayAttendanceCards(
                detailState = uiState.selectedDayDetail,
                selectedDate = activeSelectedDate,
                summariesMap = uiState.summariesMap,
                timezone = uiState.officeTimezone
            )



            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))
        }

// Day detail displayed directly in cards below

        // Error Banner
        if (uiState.errorMessage != null) {
            ErrorStateWithRetry(
                message = uiState.errorMessage,
                onRetry = onRetryError,
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground.copy(alpha = 0.9f))
            )
        }
    }
}

/**
 * Header zone with toggle button, month title, formatted date, and today shortcut.
 */
@Composable
private fun CalendarHeaderZone(
    monthYearTitle: String,
    selectedDate: LocalDate,
    onToggleMonthView: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(
            onClick = onToggleMonthView,
            painter = painterResource(R.drawable.ic_findez_menu_four_dots),
            contentDescription = "Toggle month or week view"
        )

        Text(
            text = monthYearTitle,
            style = AppTypographyTokens.SectionHeading.copy(fontWeight = FontWeight.SemiBold),
            color = AppPrimaryText
        )

        IconCircleButton(
            onClick = onTodayClick,
            painter = painterResource(R.drawable.ic_findez_calendar_8),
            contentDescription = "Go to today",
            tint = AppAccent
        )
    }
}

/**
 * Horizontally scrollable week strip with Compose HorizontalPager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontallyScrollableWeekStrip(
    today: LocalDate,
    selectedDate: LocalDate,
    summariesMap: Map<String, DaySummary>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = 5000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 10000 })

    val diffToSunday = if (today.dayOfWeek == DayOfWeek.SUNDAY) 0 else today.dayOfWeek.value
    val baseSunday = today.minusDays(diffToSunday.toLong())

    LaunchedEffect(selectedDate) {
        if (selectedDate == today && pagerState.currentPage != initialPage) {
            pagerState.animateScrollToPage(initialPage)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.Spacing8, horizontal = AppDimensions.Spacing4)
        ) { page ->
            val weekOffset = page - initialPage
            val pageSunday = baseSunday.plusWeeks(weekOffset.toLong())

            val days = (0..6).map { dayIndex ->
                val date = pageSunday.plusDays(dayIndex.toLong())
                val summary = summariesMap[date.toString()]
                val rawStatus = summary?.status ?: DayStatus.NO_RECORD
                val effectiveStatus = if (date == today && rawStatus == DayStatus.MISSED_PUNCH) {
                    DayStatus.NO_RECORD
                } else {
                    rawStatus
                }

                WeekDayItem(
                    date = date,
                    dayNumber = date.dayOfMonth,
                    dayOfWeekText = when (date.dayOfWeek) {
                        DayOfWeek.SUNDAY -> "S"
                        DayOfWeek.MONDAY -> "M"
                        DayOfWeek.TUESDAY -> "T"
                        DayOfWeek.WEDNESDAY -> "W"
                        DayOfWeek.THURSDAY -> "T"
                        DayOfWeek.FRIDAY -> "F"
                        DayOfWeek.SATURDAY -> "S"
                    },
                    isToday = (date == today),
                    isSelected = (date == selectedDate),
                    status = effectiveStatus
                )
            }

            WeekStrip(
                days = days,
                onDateSelected = onDateSelected
            )
        }
    }
}

/**
 * Expanded month grid with navigation chevrons and 7-column day cells.
 */
@Composable
private fun MonthGridCard(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (CalendarDayUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing16)
        ) {
            // Month Navigation Chevrons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = AppPrimaryText
                    )
                }

                Text(
                    text = uiState.monthYearTitle,
                    style = AppTypographyTokens.SectionHeading,
                    color = AppPrimaryText
                )

                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next Month",
                        tint = AppPrimaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

            // Days of Week Header (Sun, Mon, Tue, Wed, Thu, Fri, Sat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                uiState.daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = AppTypographyTokens.Caption.copy(
                            fontWeight = FontWeight.Medium,
                            color = AppSecondaryText
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

            // 7-Column Grid of Calendar Day Cells
            val rows = uiState.calendarDays.chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { cell ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarDayCell(
                                dayNumber = cell.dayNumber,
                                isCurrentMonth = cell.isCurrentMonth,
                                isToday = cell.isToday,
                                isSelected = cell.isSelected,
                                status = cell.status,
                                onClick = { onDayClick(cell) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Your Attendance section: Check In & Check Out cards side by side.
 */
@Composable
private fun SelectedDayAttendanceCards(
    detailState: DayDetailUiState?,
    selectedDate: LocalDate,
    summariesMap: Map<String, DaySummary>,
    timezone: String = "Asia/Kolkata",
    modifier: Modifier = Modifier
) {
    val summary = summariesMap[selectedDate.toString()]
    val inTime = detailState?.inTimeFormatted
        ?: summary?.inTimeUtcEpochMs?.let { CalendarDateUtils.formatPunchTime(it, timezone) }
    val outTime = detailState?.outTimeFormatted
        ?: summary?.outTimeUtcEpochMs?.let { CalendarDateUtils.formatPunchTime(it, timezone) }

    val hasIn = inTime != null
    val hasOut = outTime != null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
    ) {
        // Check In Card (Findez reference: Icon + Title side-by-side, bold time, subtle caption)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.Spacing16)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_findez_check_in),
                        contentDescription = null,
                        tint = if (hasIn) AppAccent else AppSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Check In",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = inTime ?: "--:--",
                    style = AppTypographyTokens.HeroTime,
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (hasIn) "On Time" else "Not Yet",
                    style = AppTypographyTokens.Caption,
                    color = if (hasIn) AppSecondaryText else AppDisabledText
                )
            }
        }

        // Check Out Card (Findez reference: Icon + Title side-by-side, bold time, subtle caption)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = if (hasOut) AppSurface else AppSurface.copy(alpha = 0.6f)
            ),
            border = BorderStroke(
                AppDimensions.BorderWidth,
                if (hasOut) AppBorder else AppBorder.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.Spacing16)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_findez_check_out),
                        contentDescription = null,
                        tint = if (hasOut) AppAccent else AppSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Check Out",
                        style = AppTypographyTokens.BodyMedium,
                        color = if (hasOut) AppSecondaryText else AppDisabledText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = outTime ?: "--:--",
                    style = AppTypographyTokens.HeroTime,
                    color = if (hasOut) AppPrimaryText else AppDisabledText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (hasOut) "Completed" else "Not Yet",
                    style = AppTypographyTokens.Caption,
                    color = if (hasOut) AppSecondaryText else AppDisabledText
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    AttendanceTheme {
        val today = LocalDate.of(2026, 9, 30)
        val ym = YearMonth.of(2026, 9)
        CalendarScreenContent(
            uiState = CalendarUiState(
                currentYearMonth = ym,
                monthYearTitle = "September 2026",
                calendarDays = listOf(
                    CalendarDayUiModel(today, 30, true, isToday = true, isSelected = true, status = DayStatus.COMPLETE)
                ),
                summaryStats = MonthAttendanceSummary(completeDays = 20, missedPunchDays = 1, totalRecordedDays = 21)
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onTodayClick = {},
            onDayClick = {},
            onDismissDetail = {},
            onRetryError = {}
        )
    }
}
