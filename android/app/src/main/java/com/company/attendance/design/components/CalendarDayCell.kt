package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Visual Rules (No Dots - Soft-Tinted Attendance Discs):
 * - Selected: Solid filled mint circle (#00E5A3), dark on-accent text (#04120D).
 * - Present (COMPLETE): Soft mint tinted background (16% alpha) with subtle mint border and mint text.
 * - Missed Punch (MISSED_PUNCH): Soft amber tinted background (18% alpha) with subtle amber border and amber text.
 * - Today: 1.5dp distinct border ring.
 * - Other month: Dimmed gray text, not interactive.
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
        status == DayStatus.COMPLETE -> AppAccent
        status == DayStatus.MISSED_PUNCH -> AppWarning
        isToday -> AppAccent
        else -> AppPrimaryText
    }

    val cellBackgroundModifier = when {
        !isCurrentMonth -> Modifier
        isSelected -> Modifier.background(AppAccent, CircleShape)
        status == DayStatus.COMPLETE -> Modifier.background(AppAccent.copy(alpha = 0.16f), CircleShape)
        status == DayStatus.MISSED_PUNCH -> Modifier.background(AppWarning.copy(alpha = 0.18f), CircleShape)
        else -> Modifier
    }

    val cellBorderModifier = when {
        !isCurrentMonth -> Modifier
        isSelected -> Modifier
        isToday -> Modifier.border(
            BorderStroke(1.5.dp, if (status == DayStatus.MISSED_PUNCH) AppWarning else AppAccent),
            CircleShape
        )
        status == DayStatus.COMPLETE -> Modifier.border(
            BorderStroke(1.dp, AppAccent.copy(alpha = 0.45f)),
            CircleShape
        )
        status == DayStatus.MISSED_PUNCH -> Modifier.border(
            BorderStroke(1.dp, AppWarning.copy(alpha = 0.50f)),
            CircleShape
        )
        else -> Modifier
    }

    Box(
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
