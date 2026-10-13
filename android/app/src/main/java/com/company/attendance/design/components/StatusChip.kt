package com.company.attendance.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint

/**
 * Status Chip with rounded corners (8dp), background tint and distinct text color.
 */
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius)
            )
            .padding(
                horizontal = AppDimensions.ChipPaddingHorizontal,
                vertical = AppDimensions.ChipPaddingVertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = contentColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(AppDimensions.Spacing4))
            }
            Text(
                text = text,
                style = AppTypographyTokens.CaptionMedium.copy(color = contentColor)
            )
        }
    }
}

/**
 * Convenience StatusChip for DayStatus (COMPLETE, MISSED_PUNCH, NO_RECORD).
 */
@Composable
fun DayStatusChip(
    status: DayStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        DayStatus.COMPLETE -> StatusChip(
            text = "Complete",
            containerColor = AppSuccessTint,
            contentColor = AppSuccess,
            modifier = modifier
        )
        DayStatus.MISSED_PUNCH -> StatusChip(
            text = "Missed Punch",
            containerColor = AppWarningTint,
            contentColor = AppWarning,
            modifier = modifier
        )
        DayStatus.NO_RECORD -> StatusChip(
            text = "No Record",
            containerColor = AppDisabled,
            contentColor = AppDisabledText,
            modifier = modifier,
            showDot = false
        )
    }
}

/**
 * Convenience StatusChip for SyncStatus (PENDING, SYNCED, FAILED).
 */
@Composable
fun SyncStatusChip(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        SyncStatus.SYNCED -> StatusChip(
            text = "Synced",
            containerColor = AppSuccessTint,
            contentColor = AppSuccess,
            modifier = modifier
        )
        SyncStatus.PENDING -> StatusChip(
            text = "Pending Sync",
            containerColor = AppDisabled,
            contentColor = AppSecondaryText,
            modifier = modifier
        )
        SyncStatus.FAILED -> StatusChip(
            text = "Sync Failed",
            containerColor = AppErrorTint,
            contentColor = AppError,
            modifier = modifier
        )
    }
}
