package com.company.attendance.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
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
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

@Composable
fun OnboardingCompleteStep(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(AppDimensions.ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = AppSuccessTint,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Setup Complete",
                        tint = AppSuccess,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            Text(
                text = "You're All Set!",
                style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                color = AppPrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

            Text(
                text = "Your profile photo and permissions are ready. You can now record daily punches directly from the home screen.",
                style = AppTypographyTokens.Body,
                color = AppSecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            FeatureCheckItem(
                icon = Icons.Default.Face,
                title = "Face Profile Configured",
                subtitle = "On-device biometric verification active"
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

            FeatureCheckItem(
                icon = Icons.Default.LocationOn,
                title = "Geofence Access Enabled",
                subtitle = "Office proximity verification enabled"
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

            PrimaryButton(
                text = "Go to Home",
                onClick = onComplete
            )
        }
    }
}

@Composable
private fun FeatureCheckItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(AppDimensions.BorderWidth, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppSuccessTint,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPrimaryText
                )
                Text(
                    text = subtitle,
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppSuccess,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
