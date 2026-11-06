package com.company.attendance.feature.tracking.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FieldTrackerCard(
    session: TrackingSessionEntity?,
    isStopping: Boolean,
    onStartClick: () -> Unit,
    onAddStopClick: () -> Unit,
    onStopClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing16)
        ) {
            if (session == null || session.status == "COMPLETED") {
                // IDLE STATE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppAccentTint,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = null,
                                tint = AppAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Field & Site Tracking",
                            style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppPrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Track your travel path and record inspection stops.",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryButton(
                    text = "Start Tracking",
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (session.status == "PAUSED") {
                // PAUSED STATE (Location is OFF)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppWarningTint,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = AppWarning,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tracking Paused",
                            style = AppTypographyTokens.BodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppWarning
                            )
                        )
                        Text(
                            text = "Location is off. Turn on GPS to resume recording.",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    PrimaryButton(
                        text = "Resume Tracking",
                        onClick = onResumeClick,
                        modifier = Modifier.weight(1.2f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    SecondaryButton(
                        text = "Stop & Submit",
                        onClick = onStopClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // ACTIVE STATE
                val startTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(Date(session.startTimeUtc))
                val distanceKm = session.totalDistanceM / 1000f
                val distanceText = String.format(Locale.getDefault(), "%.1f km", distanceKm)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AppAccentTint,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRACKING ACTIVE",
                            style = AppTypographyTokens.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AppAccent
                            )
                        )
                    }

                    Text(
                        text = "Started $startTimeStr",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Distance Covered",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                        Text(
                            text = distanceText,
                            style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                            color = AppPrimaryText
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Log Interval",
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                        Text(
                            text = "Every 1 min",
                            style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        text = "Add Stop",
                        onClick = onAddStopClick,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    PrimaryButton(
                        text = "Stop Tracking",
                        onClick = onStopClick,
                        isLoading = isStopping,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}
