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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuardDialog(
    hasCameraPermission: Boolean,
    hasLocationPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicAlertDialog(
        onDismissRequest = { /* Non-dismissible: permissions are mandatory */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.Spacing24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield / Security Icon
                Surface(
                    shape = CircleShape,
                    color = AppAccentTint,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions Required",
                            tint = AppAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                Text(
                    text = "Permissions Required",
                    style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                    color = AppPrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Text(
                    text = "Attendance requires both Camera and Location access to verify your identity and office geofence.",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                // Camera requirement item
                PermissionRequirementItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera Access",
                    description = "Required for on-device face matching and liveness verification.",
                    isGranted = hasCameraPermission
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // Location requirement item
                PermissionRequirementItem(
                    icon = Icons.Default.LocationOn,
                    title = "Precise Location",
                    description = "Required to verify you are within the designated office perimeter.",
                    isGranted = hasLocationPermission
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                // Action Buttons
                PrimaryButton(
                    text = "Grant Permissions",
                    onClick = onRequestPermissions
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                SecondaryButton(
                    text = "Open App Settings",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun PermissionRequirementItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = if (isGranted) AppAccentTint.copy(alpha = 0.5f) else AppSurface,
        border = BorderStroke(
            AppDimensions.BorderWidth,
            if (isGranted) AppAccent.copy(alpha = 0.4f) else AppBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.Spacing12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppAccentTint,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppPrimaryText
                    )
                    if (isGranted) {
                        Text(
                            text = "Granted",
                            style = AppTypographyTokens.Caption.copy(
                                color = com.company.attendance.design.theme.AppSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }
        }
    }
}
