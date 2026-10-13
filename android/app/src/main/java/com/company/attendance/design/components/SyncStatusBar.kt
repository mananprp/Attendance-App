package com.company.attendance.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * SyncStatusBar: Compact status line displaying pending punch count, last synced time,
 * and an immediate "Sync Now" action.
 */
@Composable
fun SyncStatusBar(
    pendingCount: Int,
    lastSyncedText: String?,
    isSyncing: Boolean,
    onSyncNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = AppAccent,
                    strokeWidth = 1.5.dp
                )
            } else if (pendingCount == 0) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = AppSuccess,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    tint = AppSecondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing6))

            val statusText = when {
                isSyncing -> "Syncing punches..."
                pendingCount > 0 -> "$pendingCount pending upload"
                !lastSyncedText.isNullOrBlank() -> "Last synced $lastSyncedText"
                else -> "All punches synced"
            }

            Text(
                text = statusText,
                style = AppTypographyTokens.Caption,
                color = AppSecondaryText
            )
        }

        AttendanceTextButton(
            text = if (isSyncing) "Syncing..." else "Sync Now",
            onClick = onSyncNowClick,
            enabled = !isSyncing
        )
    }
}
