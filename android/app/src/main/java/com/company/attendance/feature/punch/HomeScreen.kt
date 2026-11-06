package com.company.attendance.feature.punch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.R
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.util.PermissionManager
import com.company.attendance.design.components.AttendanceTextButton
import com.company.attendance.design.components.PermissionGuardDialog
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.components.StatCard
import com.company.attendance.feature.tracking.TrackingViewModel
import com.company.attendance.feature.tracking.dialogs.AddStopDialog
import com.company.attendance.feature.tracking.ui.FieldTrackerCard
import com.company.attendance.feature.tracking.ui.TripSummaryScreen
import com.company.attendance.feature.tracking.util.TrackingActionActivity
import com.company.attendance.design.components.SwipeToPunch
import com.company.attendance.design.components.SyncStatusBar
import com.company.attendance.design.components.WorkRing
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppHeaderBackground
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import kotlinx.coroutines.flow.collectLatest

/**
 * Data holder for mapping LocationCardState into StatCard visual elements.
 */
data class LocationTileData(
    val value: String,
    val label: String,
    val tint: Color,
    val isLoading: Boolean = false
)

fun mapLocationCardState(state: LocationCardState): LocationTileData {
    return when (state) {
        is LocationCardState.Locating -> LocationTileData(
            value = "Locating…",
            label = "Checking proximity",
            tint = AppSecondaryText,
            isLoading = true
        )
        is LocationCardState.InOffice -> LocationTileData(
            value = "In Office",
            label = "${state.distanceMeters.toInt()} m away",
            tint = AppSuccess
        )
        is LocationCardState.FieldVisit -> LocationTileData(
            value = if (state.isInside) "Field Site" else "Away",
            label = if (state.isInside) "${state.distanceMeters.toInt()} m · within radius" else "Return to spot",
            tint = if (state.isInside) AppSuccess else AppWarning
        )
        is LocationCardState.Outside -> LocationTileData(
            value = "Outside",
            label = "Allowed ${state.allowedRadiusMeters} m",
            tint = AppWarning
        )
        is LocationCardState.PermissionNeeded -> LocationTileData(
            value = "No permission",
            label = "Tap to allow",
            tint = AppWarning
        )
        is LocationCardState.ServicesDisabled -> LocationTileData(
            value = "GPS off",
            label = "Turn on location",
            tint = AppWarning
        )
        is LocationCardState.MockDetected -> LocationTileData(
            value = "Mock GPS",
            label = "Disable fake GPS",
            tint = AppError
        )
        is LocationCardState.Unavailable -> LocationTileData(
            value = "Weak signal",
            label = state.reason.take(20),
            tint = AppSecondaryText
        )
    }
}

/**
 * Findez-styled Home Attendance Screen.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    trackingViewModel: TrackingViewModel = hiltViewModel(),
    onNavigateToPunchCapture: (PunchType, String, Double, Double, Float) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activeTrackingSession by trackingViewModel.activeSession.collectAsState()
    val completedTripSummary by trackingViewModel.completedTripSummary.collectAsState()
    val showAddStopDialog by trackingViewModel.showAddStopDialog.collectAsState()
    val isStoppingTracking by trackingViewModel.isStopping.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(PermissionManager.hasCameraPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(PermissionManager.hasLocationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasCameraPermission = PermissionManager.hasCameraPermission(context)
        hasLocationPermission = PermissionManager.hasLocationPermission(context)
        if (hasLocationPermission) {
            viewModel.onScreenStarted()
            viewModel.onRefreshLocation()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    hasCameraPermission = PermissionManager.hasCameraPermission(context)
                    hasLocationPermission = PermissionManager.hasLocationPermission(context)
                    if (hasLocationPermission) {
                        viewModel.onScreenStarted()
                    }
                }
                Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Proactively request missing permissions on first landing
    LaunchedEffect(Unit) {
        if (!PermissionManager.hasMandatoryPermissions(context)) {
            val missing = PermissionManager.getMissingPermissions(context)
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToPunchCapture -> {
                    onNavigateToPunchCapture(
                        event.punchType,
                        event.locationType,
                        event.latitude,
                        event.longitude,
                        event.distanceMeters
                    )
                }
                is HomeNavigationEvent.NavigateToProfile -> {
                    onNavigateToProfile()
                }
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppHeaderBackground),
        containerColor = AppHeaderBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Header Zone (Header background #14181D)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar
                    if (uiState.profilePhotoBitmap != null) {
                        Image(
                            bitmap = uiState.profilePhotoBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Settings",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AppAccent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AppAccent, CircleShape),
                            color = AppAccentTint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = uiState.displayName.take(2).uppercase().ifBlank { "EM" },
                                    style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

                    // Name + Subtitle (Employee ID or Date)
                    Column(modifier = Modifier.weight(1f)) {
                        val greetingTitle = if (uiState.displayName.isNotBlank()) {
                            uiState.displayName
                        } else {
                            uiState.greeting
                        }

                        Text(
                            text = greetingTitle,
                            style = AppTypographyTokens.Title,
                            color = AppPrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.userId.isNotBlank()) "ID: ${uiState.userId}" else uiState.officeDateFormatted,
                            style = AppTypographyTokens.Caption,
                            color = AppSecondaryText
                        )
                    }
                }

                // 2. Rounded Body Panel (#070B0F with 32dp top corners)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(
                        topStart = AppDimensions.BodyPanelTopCornerRadius,
                        topEnd = AppDimensions.BodyPanelTopCornerRadius
                    ),
                    color = AppBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = AppDimensions.ScreenPadding)
                            .padding(top = AppDimensions.Spacing20, bottom = AppDimensions.Spacing32),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)
                    ) {
                        // 2x2 Stat Grid
                        val locData = mapLocationCardState(uiState.locationCardState)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                        ) {
                            // Tile 1: Checked In
                            StatCard(
                                label = "Checked In",
                                value = uiState.inTimeFormatted ?: "--:--",
                                iconPainter = painterResource(id = R.drawable.ic_findez_check_in),
                                iconTint = AppAccent,
                                modifier = Modifier.weight(1f)
                            )
                            // Tile 2: Checked Out
                            StatCard(
                                label = "Checked Out",
                                value = uiState.outTimeFormatted ?: "--:--",
                                iconPainter = painterResource(id = R.drawable.ic_findez_check_out),
                                iconTint = AppAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                        ) {
                            // Tile 3: Location Status
                            StatCard(
                                label = "Location",
                                value = locData.value,
                                iconVector = Icons.Outlined.LocationOn,
                                iconTint = locData.tint,
                                isLoading = locData.isLoading,
                                onClick = {
                                    if (uiState.locationCardState is LocationCardState.PermissionNeeded || !hasLocationPermission) {
                                        val missing = PermissionManager.getMissingPermissions(context)
                                        if (missing.isNotEmpty()) {
                                            permissionLauncher.launch(missing)
                                        }
                                    } else {
                                        viewModel.onRefreshLocation()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // Tile 4: Total Attended
                            StatCard(
                                label = "Total Attended",
                                value = "${uiState.totalAttendedDays} days",
                                iconPainter = painterResource(id = R.drawable.ic_findez_calendar_dots),
                                iconTint = AppAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Work-Hours Ring Card
                        WorkRing(
                            ringState = uiState.ringState,
                            workedTimeFormatted = uiState.workedDurationFormatted,
                            progress = uiState.ringProgress,
                            officeTimeFormatted = uiState.officeTimeFormatted,
                            shiftDisplayName = uiState.shiftDisplayName ?: "General Shift (09:00 - 18:00)",
                            isTimeUnverified = uiState.isTimeUnverified
                        )

                        // Field & Site Tracking Card
                        FieldTrackerCard(
                            session = activeTrackingSession,
                            isStopping = isStoppingTracking,
                            onStartClick = {
                                if (!hasLocationPermission) {
                                    val missing = PermissionManager.getMissingPermissions(context)
                                    if (missing.isNotEmpty()) {
                                        permissionLauncher.launch(missing)
                                    }
                                } else {
                                    trackingViewModel.startTracking()
                                }
                            },
                            onAddStopClick = { trackingViewModel.openAddStopDialog() },
                            onStopClick = { trackingViewModel.stopTracking() },
                            onResumeClick = {
                                val resumeIntent = TrackingActionActivity.createResumeIntent(context)
                                context.startActivity(resumeIntent)
                            }
                        )

                        // Field visit warning text
                        if (uiState.isNewLocationDay && uiState.isPunchOutEnabled) {
                            Text(
                                text = "Field visit active: Check-out must be within 100m of check-in spot.",
                                style = AppTypographyTokens.Caption,
                                color = AppWarning,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Punch In at New Location pill (when Punch In is available)
                        if (uiState.isPunchInEnabled) {
                            SecondaryButton(
                                text = "Punch In at New Location",
                                onClick = { viewModel.onPunchInNewLocationClicked() },
                                enabled = !uiState.isCheckingGeofence,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Swipe-To-Punch Slider
                        val swipeLabel = when {
                            uiState.isPunchInEnabled -> "Swipe to Check In"
                            uiState.isPunchOutEnabled -> "Swipe to Check Out"
                            uiState.inTimeFormatted != null && uiState.outTimeFormatted != null -> "Attendance complete"
                            else -> uiState.punchInButtonText
                        }
                        val isSwipeEnabled = (uiState.isPunchInEnabled && !uiState.isPunchInBlocked) ||
                                (uiState.isPunchOutEnabled && !uiState.isPunchOutBlocked)

                        SwipeToPunch(
                            text = swipeLabel,
                            enabled = isSwipeEnabled,
                            isLoading = uiState.isCheckingGeofence,
                            onSwipeTriggered = {
                                if (uiState.isPunchInEnabled) {
                                    viewModel.onPunchInClicked()
                                } else if (uiState.isPunchOutEnabled) {
                                    viewModel.onPunchOutClicked()
                                }
                            }
                        )

                        // Sync Status Bar
                        SyncStatusBar(
                            pendingCount = uiState.pendingCount,
                            lastSyncedText = uiState.lastSyncedText,
                            isSyncing = uiState.isSyncing,
                            onSyncNowClick = { viewModel.onSyncNowClicked() }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    when (val dialog = uiState.geofenceDialogState) {
        is GeofenceDialogState.OutsideRadius -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeofenceDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = AppWarning,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = dialog.dialogTitle,
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = dialog.formattedMessage,
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    PrimaryButton(
                        text = "OK",
                        onClick = { viewModel.dismissGeofenceDialog() },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = AppSurface
            )
        }
        is GeofenceDialogState.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeofenceDialog() },
                title = {
                    Text(
                        text = "Permission Required",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = dialog.formattedMessage,
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    PrimaryButton(
                        text = "Grant Permission",
                        onClick = {
                            viewModel.dismissGeofenceDialog()
                            val missing = PermissionManager.getMissingPermissions(context)
                            if (missing.isNotEmpty()) {
                                permissionLauncher.launch(missing)
                            }
                        },
                        modifier = Modifier.width(160.dp)
                    )
                },
                containerColor = AppSurface
            )
        }
        is GeofenceDialogState.LocationServicesDisabled -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeofenceDialog() },
                title = {
                    Text(
                        text = "Location Services Disabled",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = dialog.formattedMessage,
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    PrimaryButton(
                        text = "OK",
                        onClick = { viewModel.dismissGeofenceDialog() },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = AppSurface
            )
        }
        is GeofenceDialogState.MockLocationDetected -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeofenceDialog() },
                title = {
                    Text(
                        text = "Mock Location Detected",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = dialog.formattedMessage,
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    PrimaryButton(
                        text = "OK",
                        onClick = { viewModel.dismissGeofenceDialog() },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = AppSurface
            )
        }
        is GeofenceDialogState.LocationUnavailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeofenceDialog() },
                title = {
                    Text(
                        text = "Location Unavailable",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                },
                text = {
                    Text(
                        text = dialog.formattedMessage,
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText
                    )
                },
                confirmButton = {
                    PrimaryButton(
                        text = "OK",
                        onClick = { viewModel.dismissGeofenceDialog() },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = AppSurface
            )
        }
        null -> {}
    }

    // Mandatory Permission Guard Dialog (Blocks all actions until Camera and Location are granted)
    // Add Stop Dialog
    if (showAddStopDialog) {
        AddStopDialog(
            onDismiss = { trackingViewModel.dismissAddStopDialog() },
            onConfirm = { stopName -> trackingViewModel.confirmAddStop(stopName) }
        )
    }

    // Trip Summary Screen Overlay
    completedTripSummary?.let { summary ->
        TripSummaryScreen(
            summary = summary,
            onDoneClick = { trackingViewModel.clearCompletedTrip() }
        )
    }

    if (!hasCameraPermission || !hasLocationPermission) {
        PermissionGuardDialog(
            hasCameraPermission = hasCameraPermission,
            hasLocationPermission = hasLocationPermission,
            onRequestPermissions = {
                val missing = PermissionManager.getMissingPermissions(context)
                if (missing.isNotEmpty()) {
                    permissionLauncher.launch(missing)
                }
            },
            onOpenSettings = {
                PermissionManager.openAppSettings(context)
            }
        )
    }
}
