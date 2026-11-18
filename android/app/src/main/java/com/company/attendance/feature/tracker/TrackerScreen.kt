package com.company.attendance.feature.tracker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.database.entity.TrackingStopEntity
import com.company.attendance.core.util.PermissionManager
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
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
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.feature.tracking.TrackingViewModel
import com.company.attendance.feature.tracking.dialogs.AddStopDialog
import com.company.attendance.feature.tracking.ui.FieldTrackerCard
import com.company.attendance.feature.tracking.ui.TripSummaryScreen
import com.company.attendance.feature.tracking.util.TrackingActionActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val activeStops by viewModel.activeStops.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()
    val completedTripSummary by viewModel.completedTripSummary.collectAsState()
    val showAddStopDialog by viewModel.showAddStopDialog.collectAsState()
    val isStopping by viewModel.isStopping.collectAsState()
    val syncFailedSessionId by viewModel.syncFailedSessionId.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(PermissionManager.hasLocationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasLocationPermission = PermissionManager.hasLocationPermission(context)
        if (hasLocationPermission) {
            viewModel.startTracking()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            // 1. Header Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppHeaderBackground)
                    .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing20)
            ) {
                Column {
                    Text(
                        text = "Field & Site Tracker",
                        style = AppTypographyTokens.SectionHeading.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppPrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time route logging, stops & attendance telemetry",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText
                    )
                }
            }

            // 2. Scrollable Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing20),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)
            ) {
                // Interactive Hero Card (Active / Paused / Idle)
                FieldTrackerCard(
                    session = activeSession,
                    isStopping = isStopping,
                    onStartClick = {
                        if (!hasLocationPermission) {
                            val missing = PermissionManager.getMissingPermissions(context)
                            if (missing.isNotEmpty()) {
                                permissionLauncher.launch(missing)
                            } else {
                                viewModel.startTracking()
                            }
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    onAddStopClick = { viewModel.openAddStopDialog() },
                    onStopClick = { viewModel.stopTracking() },
                    onResumeClick = {
                        val resumeIntent = TrackingActionActivity.createResumeIntent(context)
                        context.startActivity(resumeIntent)
                    }
                )

                // Current Session Stops / Milestones
                if (activeSession != null) {
                    Text(
                        text = "Logged Stops (${activeStops.size})",
                        style = AppTypographyTokens.BodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppPrimaryText
                        ),
                        modifier = Modifier.padding(top = AppDimensions.Spacing8, start = 4.dp)
                    )

                    if (activeStops.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = AppSurface,
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Place,
                                        contentDescription = null,
                                        tint = AppSecondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(AppDimensions.Spacing12))
                                Text(
                                    text = "No stops logged yet. Tap '+ Add Stop' above whenever you reach an inspection site or client office.",
                                    style = AppTypographyTokens.Caption.copy(
                                        color = AppSecondaryText,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeStops.forEachIndexed { index, stop ->
                                StopItemCard(index = index + 1, stop = stop)
                            }
                        }
                    }
                }

                // Past Completed Trips / History
                if (activeSession == null && completedSessions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppDimensions.Spacing8, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Trip History (${completedSessions.size})",
                            style = AppTypographyTokens.BodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppPrimaryText
                            )
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        completedSessions.forEach { session ->
                            TripHistoryCard(
                                session = session,
                                onClick = { viewModel.viewPastTripSummary(session.sessionId) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing32))
            }
        }

        // Add Stop Dialog
        if (showAddStopDialog) {
            AddStopDialog(
                onDismiss = { viewModel.dismissAddStopDialog() },
                onConfirm = { stopName -> viewModel.confirmAddStop(stopName) }
            )
        }

        // Trip Summary Overlay
        completedTripSummary?.let { summary ->
            TripSummaryScreen(
                summary = summary,
                onDoneClick = { viewModel.clearCompletedTrip() }
            )
        }

        // Sync Failure / Offline Retry Dialog
        syncFailedSessionId?.let { sessionId ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSyncFailedDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = "Offline",
                        tint = AppWarning,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Trip Saved Locally",
                        style = AppTypographyTokens.SectionHeading.copy(fontWeight = FontWeight.Bold),
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = "Your route and stops were recorded locally on your device, but could not be uploaded to the server due to network connectivity issues.\n\nWould you like to retry uploading now?",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.retrySync(sessionId) },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isSyncing) "Syncing..." else "Retry Sync")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissSyncFailedDialog() },
                        enabled = !isSyncing
                    ) {
                        Text("Close")
                    }
                },
                containerColor = AppSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun StopItemCard(
    index: Int,
    stop: TrackingStopEntity
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeFormatted = remember(stop.arrivedAtUtc) {
        timeFormat.format(Date(stop.arrivedAtUtc))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing14),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppAccentTint)
                    .border(1.dp, AppAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = AppTypographyTokens.Caption.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppAccent,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.stopName ?: "Stop #$index",
                    style = AppTypographyTokens.BodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppPrimaryText
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Arrived: $timeFormatted • ${"%.4f".format(stop.latitude)}, ${"%.4f".format(stop.longitude)}",
                    style = AppTypographyTokens.Caption.copy(
                        color = AppSecondaryText,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TripHistoryCard(
    session: TrackingSessionEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.startTimeUtc) {
        dateFormat.format(Date(session.startTimeUtc))
    }

    val durationMins = remember(session.startTimeUtc, session.endTimeUtc) {
        val end = session.endTimeUtc ?: System.currentTimeMillis()
        Math.max(1, ((end - session.startTimeUtc) / 60000).toInt())
    }
    val hrs = durationMins / 60
    val remMins = durationMins % 60
    val durationText = if (hrs > 0) "${hrs}h ${remMins}m" else "${durationMins}m"

    val distanceKm = session.totalDistanceM / 1000f
    val distanceText = String.format(Locale.getDefault(), "%.2f km", distanceKm)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing14),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppAccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = AppAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    style = AppTypographyTokens.BodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppPrimaryText
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$distanceText • $durationText",
                    style = AppTypographyTokens.Caption.copy(
                        color = AppSecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "View Details",
                tint = AppSecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
