package com.company.attendance.feature.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.R
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppHeaderBackground
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // 1. Header Zone (#14181D)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppHeaderBackground)
                .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing20)
        ) {
            Column {
                Text(
                    text = "Work Tracker",
                    style = AppTypographyTokens.SectionHeading.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppPrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Activity & Productivity Insights",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }
        }

        // 2. Scrollable Content Area (#070B0F)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing20),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)
        ) {
            // Hero "Coming Soon" Showcase Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.Spacing24),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Mint Graphic Ring
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AppAccentTint)
                            .border(1.5.dp, AppAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_findez_nav_tracker),
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                    // Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMING SOON",
                            style = AppTypographyTokens.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppAccent,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                    Text(
                        text = "Smart Activity & Productivity",
                        style = AppTypographyTokens.Title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = AppPrimaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                    Text(
                        text = "Real-time shifts, overtime calculations, productivity milestones, and team presence trends are actively in development.",
                        style = AppTypographyTokens.Body.copy(
                            fontSize = 13.sp,
                            color = AppSecondaryText,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                text = "Upcoming Features",
                style = AppTypographyTokens.BodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppPrimaryText
                ),
                modifier = Modifier.padding(top = AppDimensions.Spacing8, start = 4.dp)
            )

            // Feature Preview 1: Shift & Break Analytics
            FeaturePreviewCard(
                icon = Icons.Outlined.Schedule,
                title = "Shift & Break Analytics",
                description = "Automated tracking of active shift compliance, productive intervals, and field duration."
            )

            // Feature Preview 2: Overtime & Milestones
            FeaturePreviewCard(
                icon = Icons.Outlined.Timeline,
                title = "Overtime & Milestones",
                description = "Instant computation of extra hours, streak milestones, and punctuality scoring."
            )

            // Feature Preview 3: Exportable Audits
            FeaturePreviewCard(
                icon = Icons.Outlined.Assessment,
                title = "Exportable Payroll Audits",
                description = "One-tap export of verified presence summaries directly formatted for payroll sync."
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))
        }
    }
}

@Composable
private fun FeaturePreviewCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing14))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypographyTokens.BodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppPrimaryText
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = AppTypographyTokens.Caption.copy(
                        color = AppSecondaryText,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
