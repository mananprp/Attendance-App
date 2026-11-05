package com.company.attendance.design.gallery

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.company.attendance.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.design.components.AttendanceBottomNavBar
import com.company.attendance.design.components.AttendanceTextField
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.components.CalendarDayCell
import com.company.attendance.design.components.DangerButton
import com.company.attendance.design.components.DayDetailSheetContent
import com.company.attendance.design.components.DayStatusChip
import com.company.attendance.design.components.EmptyState
import com.company.attendance.design.components.ErrorStateWithRetry
import com.company.attendance.design.components.LoadingState
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.components.StatusChip
import com.company.attendance.design.components.SyncStatusBar
import com.company.attendance.design.components.SyncStatusChip
import com.company.attendance.design.components.TimeDisplayCard
import com.company.attendance.design.components.TimeUnverifiedBadge
import com.company.attendance.design.components.StatCard
import com.company.attendance.design.components.WorkRing
import com.company.attendance.design.components.WorkRingState
import com.company.attendance.design.components.SwipeToPunch
import com.company.attendance.design.components.WeekStrip
import com.company.attendance.design.components.WeekDayItem
import com.company.attendance.design.components.IconCircleButton
import com.company.attendance.design.components.SectionHeader
import java.time.LocalDate
import java.time.LocalTime

import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AttendanceTheme
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint

/**
 * ComponentGalleryScreen: Comprehensive design system gallery preview
 * showcasing all theme tokens, typography, shapes, and custom UI components in interactive states.
 */
@Composable
fun ComponentGalleryScreen(
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AttendanceTopAppBar(
                title = "Design System Gallery",
                canNavigateBack = onNavigateBack != null,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimensions.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing24)
        ) {
            item { Spacer(modifier = Modifier.height(AppDimensions.Spacing8)) }

            // Section 1: Color Palette
            item {
                GallerySection(title = "1. Color Palette (Findez Dark Theme)") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                        ) {
                            ColorSwatchItem("Background\n#FAFAFA", AppBackground, AppPrimaryText, Modifier.weight(1f), hasBorder = true)
                            ColorSwatchItem("Surface\n#FFFFFF", AppSurface, AppPrimaryText, Modifier.weight(1f), hasBorder = true)
                            ColorSwatchItem("Accent\n#2563EB", AppAccent, Color.White, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                        ) {
                            ColorSwatchItem("Success\n#16A34A", AppSuccess, Color.White, Modifier.weight(1f))
                            ColorSwatchItem("Warning\n#D97706", AppWarning, Color.White, Modifier.weight(1f))
                            ColorSwatchItem("Error\n#DC2626", AppError, Color.White, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                        ) {
                            ColorSwatchItem("Primary Text\n#111827", AppPrimaryText, Color.White, Modifier.weight(1f))
                            ColorSwatchItem("Secondary\n#6B7280", AppSecondaryText, Color.White, Modifier.weight(1f))
                            ColorSwatchItem("Border\n#E5E7EB", AppBorder, AppPrimaryText, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Section 2: Typography Tokens
            item {
                GallerySection(title = "2. Typography Scale (Poppins)") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)) {
                        Text(text = "09:41 AM", style = AppTypographyTokens.ClockTime)
                        Text(text = "Clock Time (40sp Medium)", style = AppTypographyTokens.Caption)
                        Divider(color = AppBorder, thickness = 1.dp)

                        Text(text = "Good morning, Alex", style = AppTypographyTokens.Title)
                        Text(text = "Title (22sp Semibold)", style = AppTypographyTokens.Caption)
                        Divider(color = AppBorder, thickness = 1.dp)

                        Text(text = "Today's Attendance", style = AppTypographyTokens.SectionHeading)
                        Text(text = "Section Heading (16sp Medium)", style = AppTypographyTokens.Caption)
                        Divider(color = AppBorder, thickness = 1.dp)

                        Text(text = "Punched in at 09:15 AM via Office NTP server.", style = AppTypographyTokens.Body)
                        Text(text = "Body (14sp Regular)", style = AppTypographyTokens.Caption)
                        Divider(color = AppBorder, thickness = 1.dp)

                        Text(text = "Within 100m geofence radius", style = AppTypographyTokens.Caption)
                        Text(text = "Caption (12sp Regular)", style = AppTypographyTokens.Caption)
                    }
                }
            }

            // Section 3: Buttons
            item {
                GallerySection(title = "3. Buttons (52dp Height, 26dp Pill Radius)") {
                    var isLoadingPrimary by remember { mutableStateOf(false) }
                    var isLoadingSecondary by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)) {
                        PrimaryButton(
                            text = if (isLoadingPrimary) "Punching In..." else "Primary Button (Punch In)",
                            onClick = { isLoadingPrimary = !isLoadingPrimary },
                            isLoading = isLoadingPrimary
                        )

                        PrimaryButton(
                            text = "Primary Button Disabled",
                            onClick = {},
                            enabled = false
                        )

                        SecondaryButton(
                            text = if (isLoadingSecondary) "Processing..." else "Secondary Button (Punch Out)",
                            onClick = { isLoadingSecondary = !isLoadingSecondary },
                            isLoading = isLoadingSecondary
                        )

                        SecondaryButton(
                            text = "Secondary Button Disabled",
                            onClick = {},
                            enabled = false
                        )

                        DangerButton(
                            text = "Danger Button (Sign Out)",
                            onClick = {}
                        )
                    }
                }
            }

            // Section 4: Status Chips & Badges
            item {
                GallerySection(title = "4. Status Chips & Badges") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)) {
                        Text(text = "Attendance Day Statuses:", style = AppTypographyTokens.CaptionMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DayStatusChip(status = DayStatus.COMPLETE)
                            DayStatusChip(status = DayStatus.MISSED_PUNCH)
                            DayStatusChip(status = DayStatus.NO_RECORD)
                        }

                        Text(text = "Sync Statuses:", style = AppTypographyTokens.CaptionMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SyncStatusChip(status = SyncStatus.SYNCED)
                            SyncStatusChip(status = SyncStatus.PENDING)
                            SyncStatusChip(status = SyncStatus.FAILED)
                        }

                        Text(text = "Offline Unverified Badge:", style = AppTypographyTokens.CaptionMedium)
                        TimeUnverifiedBadge()
                    }
                }
            }

            // Section 5: TimeDisplayCard
            item {
                GallerySection(title = "5. Time Display Card (Home Component)") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)) {
                        Text(text = "Empty State (Before Punch In):", style = AppTypographyTokens.CaptionMedium)
                        TimeDisplayCard(inTime = null, outTime = null)

                        Text(text = "Active Day (Punched In Only):", style = AppTypographyTokens.CaptionMedium)
                        TimeDisplayCard(inTime = "09:15 AM", outTime = null)

                        Text(text = "Completed Day (Both Times Recorded):", style = AppTypographyTokens.CaptionMedium)
                        TimeDisplayCard(inTime = "09:15 AM", outTime = "06:30 PM")
                    }
                }
            }

            // Section 6: Text Fields
            item {
                GallerySection(title = "6. Text Fields (12dp Radius, 1dp Border)") {
                    var userIdText by remember { mutableStateOf("") }
                    var passwordText by remember { mutableStateOf("secret123") }
                    var passwordVisible by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)) {
                        AttendanceTextField(
                            value = userIdText,
                            onValueChange = { userIdText = it },
                            label = "User ID",
                            placeholder = "Enter your employee ID (e.g. EMP1001)",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = AppSecondaryText
                                )
                            }
                        )

                        AttendanceTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            label = "Password",
                            placeholder = "Enter password",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = AppSecondaryText
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = AppSecondaryText
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )

                        AttendanceTextField(
                            value = "invalid_user",
                            onValueChange = {},
                            label = "User ID with Error",
                            isError = true,
                            errorMessage = "Invalid credentials. Please check your user ID."
                        )
                    }
                }
            }

            // Section 7: Calendar Day Cells
            item {
                GallerySection(title = "7. Calendar Day Cells (42dp Grid Cells)") {
                    var selectedIndex by remember { mutableIntStateOf(3) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Regular day
                        CalendarDayCell(
                            dayNumber = 12,
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = selectedIndex == 0,
                            status = null,
                            onClick = { selectedIndex = 0 },
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )

                        // 2. Today outlined
                        CalendarDayCell(
                            dayNumber = 15,
                            isCurrentMonth = true,
                            isToday = true,
                            isSelected = selectedIndex == 1,
                            status = null,
                            onClick = { selectedIndex = 1 },
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )

                        // 3. Complete (green dot)
                        CalendarDayCell(
                            dayNumber = 16,
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = selectedIndex == 2,
                            status = DayStatus.COMPLETE,
                            onClick = { selectedIndex = 2 },
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )

                        // 4. Selected (Accent filled)
                        CalendarDayCell(
                            dayNumber = 17,
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = selectedIndex == 3,
                            status = DayStatus.COMPLETE,
                            onClick = { selectedIndex = 3 },
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )

                        // 5. Missed Punch (Amber dot)
                        CalendarDayCell(
                            dayNumber = 18,
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = selectedIndex == 4,
                            status = DayStatus.MISSED_PUNCH,
                            onClick = { selectedIndex = 4 },
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )

                        // 6. Other month
                        CalendarDayCell(
                            dayNumber = 30,
                            isCurrentMonth = false,
                            isToday = false,
                            isSelected = false,
                            status = null,
                            onClick = {},
                            modifier = Modifier.size(AppDimensions.CalendarCellSize)
                        )
                    }
                }
            }

            // Section 8: Sync Status Bar
            item {
                GallerySection(title = "8. Sync Status Bar") {
                    var isSyncing by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)) {
                        Text(text = "Synced State:", style = AppTypographyTokens.CaptionMedium)
                        SyncStatusBar(
                            pendingCount = 0,
                            lastSyncedText = "Today at 09:30 AM",
                            isSyncing = false,
                            onSyncNowClick = {}
                        )

                        Text(text = "Pending Uploads State:", style = AppTypographyTokens.CaptionMedium)
                        SyncStatusBar(
                            pendingCount = 2,
                            lastSyncedText = "Yesterday at 06:30 PM",
                            isSyncing = isSyncing,
                            onSyncNowClick = { isSyncing = !isSyncing }
                        )
                    }
                }
            }

            // Section 9: State Views
            item {
                GallerySection(title = "9. Loading, Empty & Error State Views") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing20)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                            border = androidx.compose.foundation.BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
                            colors = CardDefaults.cardColors(containerColor = AppSurface)
                        ) {
                            LoadingState(message = "Downloading face detection model (17 MB)...")
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                            border = androidx.compose.foundation.BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
                            colors = CardDefaults.cardColors(containerColor = AppSurface)
                        ) {
                            EmptyState(
                                title = "No Attendance Records",
                                message = "You have not recorded any punches for this month.",
                                actionText = "Refresh",
                                onActionClick = {}
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                            border = androidx.compose.foundation.BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
                            colors = CardDefaults.cardColors(containerColor = AppSurface)
                        ) {
                            ErrorStateWithRetry(
                                title = "Outside Office Geofence",
                                message = "You are currently 250m away from the office. Punch is allowed within 100m only.",
                                onRetry = {}
                            )
                        }
                    }
                }
            }

            // Section 10: Day Detail Sheet Mockup
            item {
                GallerySection(title = "10. Day Detail Sheet Content Mockup") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        border = androidx.compose.foundation.BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
                        colors = CardDefaults.cardColors(containerColor = AppSurface)
                    ) {
                        DayDetailSheetContent(
                            dateTitle = "Wednesday, Sep 30, 2026",
                            inTime = "09:14 AM (NTP)",
                            outTime = "06:32 PM (NTP)",
                            status = DayStatus.COMPLETE,
                            syncStatus = SyncStatus.SYNCED,
                            timeUnverified = false,
                            onCloseClick = {}
                        )
                    }
                }
            }


            // Section 12: Findez New Components
            item {
                GallerySection(title = "12. Findez New Components") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing16)) {
                        SectionHeader(
                            title = "Section Header Sample",
                            actionText = "See more",
                            onActionClick = {}
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                label = "Checked In",
                                value = "09:15 AM",
                                iconPainter = painterResource(R.drawable.ic_findez_check_in),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Total Attended",
                                value = "21 days",
                                iconPainter = painterResource(R.drawable.ic_findez_calendar_dots),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Work Ring
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            WorkRing(
                                ringState = WorkRingState.InProgress,
                                workedTimeFormatted = "4:32",
                                progress = 0.5f,
                                officeTimeFormatted = "Office time 13:47:20",
                                shiftDisplayName = "General Shift (09:00 - 18:00)"
                            )
                        }

                        // Swipe To Punch
                        SwipeToPunch(
                            text = "Swipe to Punch In",
                            onSwipeTriggered = {}
                        )

                        // Week Strip
                        val today = LocalDate.now()
                        val sampleWeekDays = (0..6).map { idx ->
                            val d = today.minusDays(today.dayOfWeek.value.toLong()).plusDays(idx.toLong())
                            WeekDayItem(
                                date = d,
                                dayNumber = d.dayOfMonth,
                                dayOfWeekText = d.dayOfWeek.name.take(1),
                                isToday = d == today,
                                isSelected = d == today,
                                status = if (idx < 3) DayStatus.COMPLETE else DayStatus.NO_RECORD
                            )
                        }
                        WeekStrip(
                            days = sampleWeekDays,
                            onDateSelected = {}
                        )
                    }
                }
            }

            // Section 13: Findez Vector Drawables
            item {
                GallerySection(title = "13. Hand-Traced Findez Vector Drawables") {
                    val icons = listOf(
                        Pair(R.drawable.ic_findez_check_in, "check_in"),
                        Pair(R.drawable.ic_findez_check_out, "check_out"),
                        Pair(R.drawable.ic_findez_checked_box, "checked_box"),
                        Pair(R.drawable.ic_findez_calendar_dots, "calendar_dots"),
                        Pair(R.drawable.ic_findez_calendar_8, "calendar_8"),
                        Pair(R.drawable.ic_findez_menu_four_dots, "menu_four_dots"),
                        Pair(R.drawable.ic_findez_nav_home, "nav_home"),
                        Pair(R.drawable.ic_findez_nav_home_active, "nav_home_act"),
                        Pair(R.drawable.ic_findez_nav_schedule, "nav_schedule"),
                        Pair(R.drawable.ic_findez_nav_schedule_active, "nav_sched_act"),
                        Pair(R.drawable.ic_findez_nav_settings, "nav_settings"),
                        Pair(R.drawable.ic_findez_nav_settings_active, "nav_sett_act"),
                        Pair(R.drawable.ic_findez_punch_in, "punch_in"),
                        Pair(R.drawable.ic_findez_punch_out, "punch_out")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        icons.chunked(4).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowIcons.forEach { (resId, label) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        IconCircleButton(
                                            onClick = {},
                                            painter = painterResource(resId),
                                            contentDescription = label,
                                            tint = AppAccent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            style = AppTypographyTokens.Caption,
                                            color = AppSecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 11: Bottom Navigation Bar
            item {
                GallerySection(title = "11. Bottom Navigation Bar") {
                    var currentTab by remember { mutableStateOf("home") }
                    AttendanceBottomNavBar(
                        currentRoute = currentTab,
                        onNavigateToRoute = { currentTab = it }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(AppDimensions.Spacing48)) }
        }
    }
}

@Composable
private fun GallerySection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        border = androidx.compose.foundation.BorderStroke(AppDimensions.CardBorderWidth, AppBorder),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.CardPadding)
        ) {
            Text(
                text = title,
                style = AppTypographyTokens.SectionHeading.copy(fontWeight = FontWeight.SemiBold),
                color = AppPrimaryText
            )
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
            content()
        }
    }
}

@Composable
private fun ColorSwatchItem(
    label: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    hasBorder: Boolean = false
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(color, RoundedCornerShape(AppDimensions.ChipCornerRadius))
            .then(
                if (hasBorder) Modifier.border(1.dp, AppBorder, RoundedCornerShape(AppDimensions.ChipCornerRadius))
                else Modifier
            )
            .padding(AppDimensions.Spacing8),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTypographyTokens.Caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
fun ComponentGalleryScreenPreview() {
    AttendanceTheme {
        ComponentGalleryScreen()
    }
}
