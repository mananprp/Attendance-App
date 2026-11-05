package com.company.attendance.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.company.attendance.R
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppOnAccent
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * Bottom navigation items (Findez 5-slot layout: Home, Calendar, Tracker, Settings).
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val outlineIconRes: Int,
    val activeIconRes: Int
) {
    HOME(
        route = "home",
        title = "Home",
        outlineIconRes = R.drawable.ic_findez_nav_home,
        activeIconRes = R.drawable.ic_findez_nav_home_active
    ),
    CALENDAR(
        route = "calendar",
        title = "Calendar",
        outlineIconRes = R.drawable.ic_findez_nav_schedule,
        activeIconRes = R.drawable.ic_findez_nav_schedule_active
    ),
    TRACKER(
        route = "tracker",
        title = "Tracker",
        outlineIconRes = R.drawable.ic_findez_nav_tracker,
        activeIconRes = R.drawable.ic_findez_nav_tracker_active
    ),
    SETTINGS(
        route = "profile_settings",
        title = "Settings",
        outlineIconRes = R.drawable.ic_findez_nav_settings,
        activeIconRes = R.drawable.ic_findez_nav_settings_active
    )
}

/**
 * State for the protruding center punch button.
 */
enum class CenterPunchState {
    PUNCH_IN,
    PUNCH_OUT,
    COMPLETE,
    BLOCKED
}

/**
 * AttendanceBottomNavBar: 5-slot Findez style navigation bar with raised center punch button.
 */
@Composable
fun AttendanceBottomNavBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    centerPunchState: CenterPunchState = CenterPunchState.PUNCH_IN,
    isCenterPunchLoading: Boolean = false,
    onCenterPunchClick: () -> Unit = { onNavigateToRoute("home") },
    isAdmin: Boolean = false // Maintained for signature compatibility
) {
    val navHeight = 64.dp
    val centerButtonSize = 56.dp
    val protrusion = 18.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navHeight + protrusion),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Base Navigation Bar surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(navHeight),
            color = AppSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1dp top divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.DividerThickness)
                        .background(AppBorder)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppDimensions.Spacing8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Home Item
                    NavBarItemView(
                        item = BottomNavItem.HOME,
                        isSelected = currentRoute == BottomNavItem.HOME.route,
                        onClick = { onNavigateToRoute(BottomNavItem.HOME.route) },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. Calendar Item
                    NavBarItemView(
                        item = BottomNavItem.CALENDAR,
                        isSelected = currentRoute == BottomNavItem.CALENDAR.route,
                        onClick = { onNavigateToRoute(BottomNavItem.CALENDAR.route) },
                        modifier = Modifier.weight(1f)
                    )

                    // Center Placeholder spacing for raised punch button
                    Spacer(modifier = Modifier.weight(1.1f))

                    // 3. Tracker Item
                    NavBarItemView(
                        item = BottomNavItem.TRACKER,
                        isSelected = currentRoute == BottomNavItem.TRACKER.route,
                        onClick = { onNavigateToRoute(BottomNavItem.TRACKER.route) },
                        modifier = Modifier.weight(1f)
                    )

                    // 4. Settings Item
                    NavBarItemView(
                        item = BottomNavItem.SETTINGS,
                        isSelected = currentRoute == BottomNavItem.SETTINGS.route || currentRoute == "admin_panel",
                        onClick = { onNavigateToRoute(BottomNavItem.SETTINGS.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Protruding Center Punch Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(centerButtonSize)
                .clip(CircleShape)
                .background(
                    when (centerPunchState) {
                        CenterPunchState.BLOCKED -> AppDisabled
                        CenterPunchState.COMPLETE -> AppSurface
                        else -> AppAccent
                    }
                )
                .then(
                    if (centerPunchState == CenterPunchState.COMPLETE) {
                        Modifier.border(2.dp, AppAccent, CircleShape)
                    } else if (centerPunchState == CenterPunchState.BLOCKED) {
                        Modifier.border(1.dp, AppBorder, CircleShape)
                    } else Modifier
                )
                .clickable(
                    enabled = centerPunchState != CenterPunchState.BLOCKED && !isCenterPunchLoading,
                    onClick = onCenterPunchClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCenterPunchLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppOnAccent,
                    strokeWidth = 2.5.dp
                )
            } else {
                when (centerPunchState) {
                    CenterPunchState.PUNCH_IN -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_findez_punch_in),
                            contentDescription = "Punch In",
                            tint = AppOnAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    CenterPunchState.PUNCH_OUT -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_findez_punch_out),
                            contentDescription = "Punch Out",
                            tint = AppOnAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    CenterPunchState.COMPLETE -> {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Punch Completed",
                            tint = AppAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    CenterPunchState.BLOCKED -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_findez_punch_in),
                            contentDescription = "Punch Disabled",
                            tint = AppDisabledText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * NavBarItemView: Renders a single bottom bar tab with top indicator pill when selected.
 */
@Composable
private fun NavBarItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mint indicator line above active item
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(if (isSelected) AppAccent else Color.Transparent)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Icon
        Icon(
            painter = painterResource(id = if (isSelected) item.activeIconRes else item.outlineIconRes),
            contentDescription = item.title,
            tint = if (isSelected) AppAccent else AppSecondaryText,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = item.title,
            style = AppTypographyTokens.Caption.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
            ),
            color = if (isSelected) AppAccent else AppSecondaryText
        )
    }
}
