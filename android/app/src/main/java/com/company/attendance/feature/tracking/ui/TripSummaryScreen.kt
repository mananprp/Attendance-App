package com.company.attendance.feature.tracking.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppHeaderBackground
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.feature.tracking.repository.TrackingTripSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripSummaryScreen(
    summary: TrackingTripSummary,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMins = Math.max(1, ((summary.endTimeUtc - summary.startTimeUtc) / 60000).toInt())
    val hrs = durationMins / 60
    val remMins = durationMins % 60
    val durationText = if (hrs > 0) "${hrs}h ${remMins}m" else "${durationMins}m"

    val distanceKm = summary.totalDistanceM / 1000f
    val distanceText = String.format(Locale.getDefault(), "%.2f km", distanceKm)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground),
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Success Icon
            Surface(
                shape = CircleShape,
                color = AppAccentTint,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Completed",
                        tint = AppAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            Text(
                text = "Site Trip Completed",
                style = AppTypographyTokens.HeroTitle.copy(fontWeight = FontWeight.Bold),
                color = AppPrimaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your route and stops have been logged and synced to the supervisor portal.",
                style = AppTypographyTokens.BodyMedium,
                color = AppSecondaryText
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            // Metrics Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
            ) {
                // Distance
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = null,
                                tint = AppAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DISTANCE",
                                style = AppTypographyTokens.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppSecondaryText
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = distanceText,
                            style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                            color = AppPrimaryText
                        )
                    }
                }

                // Duration
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = AppAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DURATION",
                                style = AppTypographyTokens.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppSecondaryText
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = durationText,
                            style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                            color = AppPrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Stops Visited Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recorded Site Stops (${summary.stops.size})",
                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppPrimaryText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (summary.stops.isEmpty()) {
                        Text(
                            text = "No stops were explicitly marked during this journey.",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                    } else {
                        summary.stops.forEachIndexed { idx, stop ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppAccentTint,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = AppTypographyTokens.Caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = AppAccent
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stop.stopName ?: "Stop ${idx + 1}",
                                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AppPrimaryText
                                    )
                                    Text(
                                        text = "Arrived at ${timeFormat.format(Date(stop.arrivedAtUtc))}",
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Breadcrumb Summary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Recorded Coordinate Points",
                            style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppPrimaryText
                        )
                        Text(
                            text = "1-minute GPS location breadcrumbs",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                    }
                    Text(
                        text = "${summary.points.size} points",
                        style = AppTypographyTokens.Title.copy(color = AppAccent),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

            PrimaryButton(
                text = "Return to Dashboard",
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
        }
    }
}
