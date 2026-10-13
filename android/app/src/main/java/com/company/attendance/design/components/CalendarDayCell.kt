package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning

/**
 * CalendarDayCell: Represents a single day cell in the monthly grid.
 *
 * Visual Rules:
 * - Selected: filled circle with accent color (#2563EB), white text.
 * - Today: outlined circle with accent color (1dp #2563EB), primary text.
 * - Status dot: Green (#16A34A) for COMPLETE, Amber (#D97706) for MISSED_PUNCH, None for NO_RECORD.
 * - Other month: Dimmed gray text, not interactive.
 */
@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    status: DayStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val textColor = when {
        !isCurrentMonth -> AppDisabledText
        isSelected -> Color.White
        isToday -> AppAccent
        else -> AppPrimaryText
    }

    val cellBackgroundModifier = when {
        isSelected -> Modifier.background(AppAccent, CircleShape)
        else -> Modifier
    }

    val cellBorderModifier = when {
        isToday && !isSelected -> Modifier.border(
            BorderStroke(1.dp, AppAccent),
            CircleShape
        )
        else -> Modifier
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(cellBackgroundModifier)
            .then(cellBorderModifier)
            .clickable(
                enabled = isCurrentMonth,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = AppTypographyTokens.BodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Status dot indicator
        if (isCurrentMonth && status != null && status != DayStatus.NO_RECORD) {
            val dotColor = when {
                isSelected -> Color.White
                status == DayStatus.COMPLETE -> AppSuccess
                status == DayStatus.MISSED_PUNCH -> AppWarning
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .size(AppDimensions.CalendarStatusDotSize)
                    .background(dotColor, CircleShape)
            )
        } else {
            // Placeholder space so day number remains vertically balanced
            Spacer(modifier = Modifier.size(AppDimensions.CalendarStatusDotSize))
        }
    }
}
