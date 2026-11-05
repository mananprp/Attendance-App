package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppIconCircle
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * StatCard: A 20dp-radius minimal tile matching Findez 2x2 stat grid.
 * Top row: Value (Left) + Icon (Right)
 * Bottom row: Label (Left)
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = AppAccent,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
    sublabel: String? = null,
    sublabelColor: Color = AppSecondaryText
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(AppDimensions.StatTileCornerRadius),
        color = AppSurface,
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing16),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Value (Left) and Icon / Spinner (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = AppTypographyTokens.StatValue,
                    color = AppPrimaryText
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                } else if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

            // Bottom row: Label
            Text(
                text = label,
                style = AppTypographyTokens.StatLabel,
                color = AppSecondaryText
            )
        }
    }
}
