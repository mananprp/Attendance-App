package com.company.attendance.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.core.model.DayStatus
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.components.CalendarDayCell
import com.company.attendance.design.components.DayStatusChip
import com.company.attendance.design.components.ErrorStateWithRetry
import com.company.attendance.design.components.LoadingState
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
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
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarScreen: Stateful entry point for Monthly Attendance Calendar.
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
        modifier = modifier
    )
}

/**
 * CalendarScreenContent: Stateless content for Monthly Attendance Calendar.
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

            // Screen Title
            Text(
                text = "Attendance Calendar",
                style = AppTypographyTokens.Title,
                color = AppPrimaryText
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Month Navigation Card
            MonthNavigationHeader(
                monthYearTitle = uiState.monthYearTitle,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onTodayClick = onTodayClick
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Monthly Attendance Summary Card
            MonthlySummaryCard(summary = uiState.summaryStats)

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Main Calendar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = androidx.compose.foundation.BorderStroke(AppDimensions.BorderWidth, AppBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.Spacing16)
                ) {
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

            // Legend Footer
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
            CalendarLegend()

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))
        }

        // Day Detail Bottom Sheet
        if (uiState.isDetailSheetVisible && uiState.selectedDayDetail != null) {
            DayDetailBottomSheet(
                detailState = uiState.selectedDayDetail,
                onDismissRequest = onDismissDetail
            )
        }

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
 * Month navigation header with Previous, Next and Today controls.
 */
@Composable
private fun MonthNavigationHeader(
    monthYearTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.Spacing8, vertical = AppDimensions.Spacing4),
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
                text = monthYearTitle,
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText,
                textAlign = TextAlign.Center
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTodayClick) {
                    Icon(
                        imageVector = Icons.Outlined.Today,
                        contentDescription = "Today",
                        tint = AppAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next Month",
                        tint = AppPrimaryText
                    )
                }
            }
        }
    }
}

/**
 * Monthly attendance summary showing complete and missed punch totals.
 */
@Composable
private fun MonthlySummaryCard(summary: MonthAttendanceSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.Spacing16, vertical = AppDimensions.Spacing12),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Complete Days Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.CalendarStatusDotSize)
                        .background(AppSuccess, CircleShape)
                )
                Text(
                    text = "${summary.completeDays} Complete",
                    style = AppTypographyTokens.BodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = AppPrimaryText
                    )
                )
            }

            // Divider Dot
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(AppBorder, CircleShape)
            )

            // Missed Punch Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.CalendarStatusDotSize)
                        .background(AppWarning, CircleShape)
                )
                Text(
                    text = "${summary.missedPunchDays} Missed",
                    style = AppTypographyTokens.BodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = AppPrimaryText
                    )
                )
            }
        }
    }
}

/**
 * Calendar status dot legend.
 */
@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.Spacing8),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing4)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AppSuccess, CircleShape)
            )
            Text(
                text = "Complete",
                style = AppTypographyTokens.Caption,
                color = AppSecondaryText
            )
        }

        Spacer(modifier = Modifier.width(AppDimensions.Spacing16))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing4)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AppWarning, CircleShape)
            )
            Text(
                text = "Missed Punch",
                style = AppTypographyTokens.Caption,
                color = AppSecondaryText
            )
        }

        Spacer(modifier = Modifier.width(AppDimensions.Spacing16))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing4)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(1.dp, AppAccent, CircleShape)
            )
            Text(
                text = "Today",
                style = AppTypographyTokens.Caption,
                color = AppSecondaryText
            )
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
