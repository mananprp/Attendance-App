package com.company.attendance.feature.punch

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.PunchType
import com.company.attendance.design.components.AttendanceTextButton
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.components.SyncStatusBar
import com.company.attendance.design.components.TimeDisplayCard
import com.company.attendance.design.components.TimeUnverifiedBadge
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppTypographyTokens
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.company.attendance.core.util.PermissionManager
import com.company.attendance.design.components.PermissionGuardDialog

/**
 * HomeScreen: Main attendance dashboard displaying greeting, trusted clock,
 * today's punch times card, state-machine punch in/out buttons, sync bar, and geofence alerts.
 */
@Composable
fun HomeScreen(
    onNavigateToPunchCapture: (PunchType, String, Double, Double, Float) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

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
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = PermissionManager.hasCameraPermission(context)
                hasLocationPermission = PermissionManager.hasLocationPermission(context)
                if (hasLocationPermission) {
                    viewModel.onScreenStarted()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onScreenStopped()
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
            .background(AppBackground),
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppDimensions.ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Top Header: Greeting, Date & Profile Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val greetingTitle = if (uiState.displayName.isNotBlank()) {
                            "${uiState.greeting}, ${uiState.displayName}"
                        } else {
                            uiState.greeting
                        }

                        Text(
                            text = greetingTitle,
                            style = AppTypographyTokens.Title,
                            color = AppPrimaryText
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing4))
                        Text(
                            text = uiState.officeDateFormatted.ifEmpty { "Office Time" },
                            style = AppTypographyTokens.BodyMedium,
                            color = AppSecondaryText
                        )
                    }

                    if (uiState.profilePhotoBitmap != null) {
                        Image(
                            bitmap = uiState.profilePhotoBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Settings",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AppAccent, CircleShape)
                                .clickable { viewModel.onProfileClicked() },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.onProfileClicked() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profile Settings",
                                tint = AppSecondaryText,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                // Large Trusted Clock Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.officeTimeFormatted,
                        style = AppTypographyTokens.ClockTime,
                        color = AppPrimaryText,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.isTimeUnverified) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                        TimeUnverifiedBadge(
                            text = "Time unverified (offline clock)"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Real-Time Location Proximity Card
                LocationStatusCard(
                    state = uiState.locationCardState,
                    isRefreshing = uiState.isCheckingGeofence,
                    onRefreshClick = { viewModel.onRefreshLocation() }
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Today's Attendance Summary Card (Punch In / Punch Out times)
                uiState.shiftDisplayName?.let { shiftName ->
                    Text(
                        text = "⏱️ $shiftName",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp, start = 4.dp)
                    )
                }
                TimeDisplayCard(
                    inTime = uiState.inTimeFormatted,
                    outTime = uiState.outTimeFormatted
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                // Punch Action Buttons (State Machine)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    // Punch In Buttons
                    if (uiState.isPunchInEnabled) {
                        PrimaryButton(
                            text = uiState.punchInButtonText,
                            onClick = { viewModel.onPunchInClicked() },
                            isLoading = uiState.isCheckingGeofence,
                            enabled = !uiState.isCheckingGeofence,
                            leadingIcon = Icons.Outlined.LocationOn
                        )
                        SecondaryButton(
                            text = "Punch In at New Location",
                            onClick = { viewModel.onPunchInNewLocationClicked() },
                            isLoading = uiState.isCheckingGeofence,
                            enabled = !uiState.isCheckingGeofence,
                            leadingIcon = Icons.Outlined.NearMe
                        )
                    } else {
                        SecondaryButton(
                            text = uiState.punchInButtonText,
                            onClick = {},
                            enabled = false
                        )
                    }

                    // Punch Out Button
                    if (uiState.isPunchOutEnabled) {
                        PrimaryButton(
                            text = uiState.punchOutButtonText,
                            onClick = { viewModel.onPunchOutClicked() },
                            isLoading = uiState.isCheckingGeofence,
                            enabled = !uiState.isCheckingGeofence,
                            leadingIcon = Icons.Outlined.NearMe
                        )
                        if (uiState.isNewLocationDay) {
                            Text(
                                text = "📍 Field visit: You must punch out within 100m of your check-in location.",
                                style = AppTypographyTokens.Caption,
                                color = AppWarning,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        SecondaryButton(
                            text = uiState.punchOutButtonText,
                            onClick = {},
                            enabled = false
                        )
                    }
                }
            }

            // Bottom Section: Sync Status Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimensions.Spacing16)
            ) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
                SyncStatusBar(
                    pendingCount = uiState.pendingCount,
                    lastSyncedText = uiState.lastSyncedText,
                    isSyncing = uiState.isSyncing,
                    onSyncNowClick = { viewModel.onSyncNowClicked() }
                )
            }
        }
    }

    // Geofence & Location Error Dialog
    uiState.geofenceDialogState?.let { dialogState ->
        GeofenceErrorAlertDialog(
            dialogState = dialogState,
            onDismiss = { viewModel.dismissGeofenceDialog() }
        )
    }

    // Mandatory Permission Guard Dialog (Blocks all actions until Camera and Location are granted)
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

/**
 * Clean Material 3 alert dialog informing employee when outside allowed geofence radius
 * or when location permissions/services are missing.
 */
@Composable
fun GeofenceErrorAlertDialog(
    dialogState: GeofenceDialogState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.LocationOff,
                contentDescription = null,
                tint = AppError,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = when (dialogState) {
                    is GeofenceDialogState.OutsideRadius -> dialogState.dialogTitle
                    is GeofenceDialogState.PermissionRequired -> "Location Permission Required"
                    is GeofenceDialogState.LocationServicesDisabled -> "Location Services Disabled"
                    is GeofenceDialogState.MockLocationDetected -> "Mock Location Detected"
                    is GeofenceDialogState.LocationUnavailable -> "Location Unavailable"
                },
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )
        },
        text = {
            Text(
                text = when (dialogState) {
                    is GeofenceDialogState.OutsideRadius -> dialogState.formattedMessage
                    is GeofenceDialogState.PermissionRequired -> dialogState.formattedMessage
                    is GeofenceDialogState.LocationServicesDisabled -> dialogState.formattedMessage
                    is GeofenceDialogState.MockLocationDetected -> dialogState.formattedMessage
                    is GeofenceDialogState.LocationUnavailable -> dialogState.formattedMessage
                },
                style = AppTypographyTokens.BodyMedium,
                color = AppSecondaryText
            )
        },
        confirmButton = {
            AttendanceTextButton(
                text = "OK",
                onClick = onDismiss
            )
        },
        containerColor = com.company.attendance.design.theme.AppSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimensions.CardCornerRadius)
    )
}

/**
 * Visual card showing the user's real-time proximity status to the office geofence.
 */
@Composable
fun LocationStatusCard(
    state: LocationCardState,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, iconColor) = when (state) {
        is LocationCardState.InOffice -> Triple(
            com.company.attendance.design.theme.AppSuccessTint,
            AppSuccess.copy(alpha = 0.5f),
            AppSuccess
        )
        is LocationCardState.FieldVisit -> if (state.isInside) {
            Triple(
                com.company.attendance.design.theme.AppSuccessTint,
                AppSuccess.copy(alpha = 0.5f),
                AppSuccess
            )
        } else {
            Triple(
                com.company.attendance.design.theme.AppWarningTint,
                AppWarning.copy(alpha = 0.5f),
                AppWarning
            )
        }
        is LocationCardState.Outside -> Triple(
            com.company.attendance.design.theme.AppWarningTint,
            AppWarning.copy(alpha = 0.5f),
            AppWarning
        )
        is LocationCardState.MockDetected -> Triple(
            com.company.attendance.design.theme.AppErrorTint,
            AppError.copy(alpha = 0.5f),
            AppError
        )
        else -> Triple(
            AppSurface,
            AppBorder,
            AppSecondaryText
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, androidx.compose.foundation.shape.RoundedCornerShape(AppDimensions.CardCornerRadius))
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(AppDimensions.CardCornerRadius))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state) {
                    is LocationCardState.Locating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppAccent
                        )
                    }
                    is LocationCardState.InOffice -> {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "In Office",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is LocationCardState.FieldVisit -> {
                        Icon(
                            imageVector = if (state.isInside) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                            contentDescription = "Field Visit",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is LocationCardState.Outside,
                    is LocationCardState.PermissionNeeded,
                    is LocationCardState.ServicesDisabled,
                    is LocationCardState.MockDetected -> {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Location Warning",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is LocationCardState.Unavailable -> {
                        Icon(
                            imageVector = Icons.Outlined.LocationOff,
                            contentDescription = "Location Unavailable",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    val (title, subtitle) = when (state) {
                        is LocationCardState.Locating -> Pair(
                            "Acquiring Location...",
                            "Checking proximity"
                        )
                        is LocationCardState.InOffice -> Pair(
                            "In Office (${state.distanceMeters.toInt()}m)",
                            "Verified proximity • ${state.updatedAgoSeconds}s ago"
                        )
                        is LocationCardState.FieldVisit -> Pair(
                            if (state.isInside) "At Field Site (${state.distanceMeters.toInt()}m)" else "Away from Field Site (${state.distanceMeters.toInt()}m)",
                            if (state.isInside) "Within check-in radius (100m) • ${state.updatedAgoSeconds}s ago" else "Must return to check-in location to punch out"
                        )
                        is LocationCardState.Outside -> Pair(
                            "Outside Office (${state.distanceMeters.toInt()}m)",
                            "Allowed radius: ${state.allowedRadiusMeters}m"
                        )
                        is LocationCardState.PermissionNeeded -> Pair(
                            "Location Permission Needed",
                            "Grant permission to verify proximity"
                        )
                        is LocationCardState.ServicesDisabled -> Pair(
                            "GPS Services Disabled",
                            "Turn on location services in device settings"
                        )
                        is LocationCardState.MockDetected -> Pair(
                            "Mock Location Detected",
                            "Disable fake GPS apps to punch"
                        )
                        is LocationCardState.Unavailable -> Pair(
                            "Location Signal Weak",
                            state.reason
                        )
                    }

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
            }

            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier.size(32.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Location",
                        tint = AppSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
