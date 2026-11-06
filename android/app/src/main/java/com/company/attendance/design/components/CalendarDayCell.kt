package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppOnAccent
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning

/**
 * CalendarDayCell: Minimalist single day cell in the monthly grid.
 *
 * Visual Rules:
 * - Selected: Fully solid green filled circle (#00E5A3), dark on-accent text.
 * - Today (Current Date): Pure white outline ring (1.5dp), white text, NO background.
 * - Present (COMPLETE): Mint green outline ring (1.5dp #00E5A3), mint text, NO background.
 * - Missed Punch (MISSED_PUNCH): Amber outline ring (1.5dp #FFB020), amber text, NO background.
 * - Other days: Default text, NO ring, NO background.
 * - Fixed 36dp circle size matching collapsed week strip.
 */
@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    status: DayStatus? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val textColor = when {
        !isCurrentMonth -> AppDisabledText
        isSelected -> AppOnAccent
        isToday -> Color.White
        status == DayStatus.COMPLETE -> AppAccent
        status == DayStatus.MISSED_PUNCH -> AppWarning
        else -> AppPrimaryText
    }

    val circleBackgroundModifier = when {
        !isCurrentMonth -> Modifier
        isSelected && status == DayStatus.MISSED_PUNCH -> Modifier.background(AppWarning, CircleShape)
        isSelected -> Modifier.background(AppAccent, CircleShape)
        else -> Modifier // No background for any non-selected date!
    }

    val circleBorderModifier = when {
        !isCurrentMonth -> Modifier
        isSelected -> Modifier // Selected is fully solid green, no border
        isToday -> Modifier.border(BorderStroke(1.5.dp, Color.White), CircleShape)
        status == DayStatus.COMPLETE -> Modifier.border(BorderStroke(1.5.dp, AppAccent), CircleShape)
        status == DayStatus.MISSED_PUNCH -> Modifier.border(BorderStroke(1.5.dp, AppWarning), CircleShape)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(
                enabled = isCurrentMonth,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(circleBackgroundModifier)
                .then(circleBorderModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = AppTypographyTokens.BodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || isToday || (status != null && status != DayStatus.NO_RECORD)) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            )
        }
    }
}
