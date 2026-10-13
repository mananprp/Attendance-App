package com.company.attendance.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * Primary action button: Accent filled (#2563EB), 12dp rounded, 52dp height, full width by default.
 * Disabled state: Light gray (#F3F4F6) with gray text (#9CA3AF).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeight),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppAccent,
            contentColor = Color.White,
            disabledContainerColor = AppDisabled,
            disabledContentColor = AppDisabledText
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = AppDimensions.Spacing16)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (enabled) Color.White else AppDisabledText,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                }
                Text(
                    text = text,
                    style = AppTypographyTokens.ButtonText
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Secondary outlined button: 12dp rounded, 52dp height, 1dp border (#E5E7EB), Surface background.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeight),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
        border = BorderStroke(
            AppDimensions.BorderWidth,
            if (enabled) AppBorder else AppDisabled
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppSurface,
            contentColor = AppPrimaryText,
            disabledContainerColor = AppDisabled,
            disabledContentColor = AppDisabledText
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = AppDimensions.Spacing16)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppPrimaryText,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                }
                Text(
                    text = text,
                    style = AppTypographyTokens.ButtonText.copy(
                        color = if (enabled) AppPrimaryText else AppDisabledText
                    )
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Destructive button for critical actions (e.g., Sign out, Remove).
 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeight),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppError,
            contentColor = Color.White,
            disabledContainerColor = AppDisabled,
            disabledContentColor = AppDisabledText
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = AppDimensions.Spacing16)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = AppTypographyTokens.ButtonText
            )
        }
    }
}

/**
 * Text-only action button (e.g. Sync Now, Change photo link).
 */
@Composable
fun AttendanceTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppAccent
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = AppDisabledText
        )
    ) {
        Text(
            text = text,
            style = AppTypographyTokens.BodyMedium.copy(
                color = if (enabled) color else AppDisabledText,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
