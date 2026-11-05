package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppOnAccent
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AttendanceTheme
import java.time.LocalDate

/**
 * Data model for a single day in the Week Strip.
 */
data class WeekDayItem(
    val date: LocalDate,
    val dayNumber: Int,
    val dayOfWeekText: String, // e.g. "M", "T", "W" or "Mon"
    val isToday: Boolean,
    val isSelected: Boolean,
    val status: DayStatus = DayStatus.NO_RECORD
)

/**
 * WeekStrip: Horizontally scrollable week strip with date circles and status dots (Findez style).
 */
@Composable
fun WeekStrip(
    days: List<WeekDayItem>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { item ->
            WeekDayCell(
                item = item,
                onClick = { onDateSelected(item.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WeekDayCell(
    item: WeekDayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = AppDimensions.Spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Weekday label (e.g. "M", "T", "W")
        Text(
            text = item.dayOfWeekText,
            style = AppTypographyTokens.Caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (item.isSelected) AppAccent else AppSecondaryText
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing6))

        // Day Number Circle (36dp)
        val circleBackgroundModifier = when {
            item.isSelected -> Modifier.background(AppAccent, CircleShape)
            else -> Modifier
        }

        val circleBorderModifier = when {
            item.isToday && !item.isSelected -> Modifier.border(
                BorderStroke(1.5.dp, AppAccent),
                CircleShape
            )
            else -> Modifier
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .then(circleBackgroundModifier)
                .then(circleBorderModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.dayNumber.toString(),
                style = AppTypographyTokens.BodyMedium.copy(
                    fontWeight = if (item.isSelected || item.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        item.isSelected -> AppOnAccent
                        item.isToday -> AppAccent
                        else -> AppPrimaryText
                    }
                )
            )
        }


    }
}

@Preview(name = "WeekStrip Preview")
@Composable
private fun PreviewWeekStrip() {
    AttendanceTheme {
        val today = LocalDate.of(2026, 9, 30)
        val sampleDays = (0..6).map { offset ->
            val date = today.plusDays(offset.toLong() - 2)
            WeekDayItem(
                date = date,
                dayNumber = date.dayOfMonth,
                dayOfWeekText = date.dayOfWeek.name.take(1),
                isToday = offset == 2,
                isSelected = offset == 2,
                status = if (offset == 0) DayStatus.COMPLETE else if (offset == 1) DayStatus.MISSED_PUNCH else DayStatus.NO_RECORD
            )
        }
        WeekStrip(days = sampleDays, onDateSelected = {})
    }
}