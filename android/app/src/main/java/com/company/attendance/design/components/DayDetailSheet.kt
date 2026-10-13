package com.company.attendance.design.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * DayDetailSheetContent: Content layout for Calendar day detail bottom sheet.
 */
@Composable
fun DayDetailSheetContent(
    dateTitle: String,
    inTime: String?,
    outTime: String?,
    status: DayStatus,
    timeUnverified: Boolean = false,
    syncStatus: SyncStatus? = null,
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurface)
            .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing16)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .background(AppBorder, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

        // Title and Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateTitle,
                    style = AppTypographyTokens.Title
                )
                Spacer(modifier = Modifier.height(AppDimensions.Spacing4))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                ) {
                    DayStatusChip(status = status)
                    if (syncStatus != null) {
                        SyncStatusChip(status = syncStatus)
                    }
                }
            }

            if (onCloseClick != null) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = AppSecondaryText
                    )
                }
            }
        }

        if (timeUnverified) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))
            TimeUnverifiedBadge()
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

        // Punch Rows
        PunchDetailRow(
            label = "Punch In",
            time = inTime ?: "--:--",
            icon = Icons.Outlined.Login
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

        PunchDetailRow(
            label = "Punch Out",
            time = outTime ?: "--:--",
            icon = Icons.Outlined.Logout
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
    }
}

@Composable
private fun PunchDetailRow(
    label: String,
    time: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(AppDimensions.CardCornerRadius))
            .padding(vertical = AppDimensions.Spacing8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppSecondaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppDimensions.Spacing12))
            Text(
                text = label,
                style = AppTypographyTokens.BodyMedium,
                color = AppSecondaryText
            )
        }
        Text(
            text = time,
            style = AppTypographyTokens.SectionHeading.copy(
                color = if (time != "--:--") AppPrimaryText else AppDisabledText
            )
        )
    }
}
