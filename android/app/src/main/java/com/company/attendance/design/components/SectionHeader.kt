package com.company.attendance.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * Clean Section Header with title and optional text action (e.g. "See more").
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTypographyTokens.SectionHeading,
            color = AppPrimaryText
        )
        if (actionText != null && onActionClick != null) {
            AttendanceTextButton(
                text = actionText,
                onClick = onActionClick,
                color = AppAccent
            )
        }
    }
}