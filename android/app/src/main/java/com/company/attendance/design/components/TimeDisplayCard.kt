package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * TimeDisplayCard: Displays today's Punch In and Punch Out times.
 * Formatted with 12dp rounded corners, 1dp border (#E5E7EB), no shadow, and dashes ("--:--") when empty.
 */
@Composable
fun TimeDisplayCard(
    inTime: String?,
    outTime: String?,
    modifier: Modifier = Modifier,
    title: String = "Today's Attendance"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        border = BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
        colors = CardDefaults.cardColors(
            containerColor = AppSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.CardPadding)
        ) {
            Text(
                text = title,
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Punch In Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Punch In",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))
                    Text(
                        text = inTime ?: "--:--",
                        style = AppTypographyTokens.Title.copy(
                            fontSize = 20.sp,
                            fontWeight = if (inTime != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (inTime != null) AppPrimaryText else AppDisabledText
                        )
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(AppBorder)
                )

                // Punch Out Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = AppDimensions.Spacing16),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Punch Out",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))
                    Text(
                        text = outTime ?: "--:--",
                        style = AppTypographyTokens.Title.copy(
                            fontSize = 20.sp,
                            fontWeight = if (outTime != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (outTime != null) AppPrimaryText else AppDisabledText
                        )
                    )
                }
            }
        }
    }
}
