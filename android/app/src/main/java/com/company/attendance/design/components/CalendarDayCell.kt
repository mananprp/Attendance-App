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

/**
 * CalendarDayCell: Minimalist single day cell in the monthly grid matching Findez design.
 *
 * Visual Rules:
 * - Selected: filled circle with mint accent color (#4EFFCA), dark on-accent text (#04120D).
 * - Today: outlined circle with mint accent color (1dp #4EFFCA), accent text.
 * - Other month: Dimmed gray text, not interactive.
 * - Clean: no status dots under dates.
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
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}
