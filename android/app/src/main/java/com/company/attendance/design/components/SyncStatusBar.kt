package com.company.attendance.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning

/**
 * SyncStatusBar: Sleek status pill displayed ONLY when offline punches are pending sync
 * or when an active sync operation is in progress. When all punches are synced, renders nothing
 * to avoid cluttering the home dashboard.
 */
@Composable
fun SyncStatusBar(
    pendingCount: Int,
    lastSyncedText: String?,
    isSyncing: Boolean,
    onSyncNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only display when there are actually pending punches or actively syncing
    if (pendingCount <= 0 && !isSyncing) {
        return
    }

    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(AppSurface)
            .border(1.dp, AppBorder, pillShape)
            .padding(horizontal = 14.dp, vertical = AppDimensions.Spacing8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = AppAccent,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = AppWarning,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimensions.Spacing8))

                val statusText = if (isSyncing) {
                    "Syncing offline punches..."
                } else {
                    val pluralSuffix = if (pendingCount > 1) "es" else ""
                    "$pendingCount offline punch$pluralSuffix pending"
                }

                Text(
                    text = statusText,
                    style = AppTypographyTokens.Caption.copy(
                        fontSize = 12.sp,
                        color = AppSecondaryText
                    )
                )
            }

            if (!isSyncing) {
                val buttonShape = RoundedCornerShape(8.dp)
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .clip(buttonShape)
                        .background(AppAccent.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onSyncNowClick
                        )
                        .padding(horizontal = 10.dp, vertical = AppDimensions.Spacing4),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sync",
                        style = AppTypographyTokens.Caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppAccent
                        )
                    )
                }
            } else {
                Text(
                    text = "Syncing",
                    style = AppTypographyTokens.Caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppSecondaryText
                    )
                )
            }
        }
    }
}
