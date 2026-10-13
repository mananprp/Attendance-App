package com.company.attendance.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint

/**
 * TimeUnverifiedBadge: Unobtrusive badge displayed when offline punch relies on device clock.
 */
@Composable
fun TimeUnverifiedBadge(
    modifier: Modifier = Modifier,
    text: String = "Time unverified (offline clock)"
) {
    Box(
        modifier = modifier
            .background(
                color = AppWarningTint,
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
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = AppWarning,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(AppDimensions.Spacing4))
            Text(
                text = text,
                style = AppTypographyTokens.CaptionMedium.copy(color = AppWarning)
            )
        }
    }
}
