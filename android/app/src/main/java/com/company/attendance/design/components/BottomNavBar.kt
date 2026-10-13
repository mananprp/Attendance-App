package com.company.attendance.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * Bottom navigation bar items.
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val requiresAdmin: Boolean = false
) {
    HOME(
        route = "home",
        title = "Home",
        icon = Icons.Outlined.Home
    ),
    CALENDAR(
        route = "calendar",
        title = "Calendar",
        icon = Icons.Outlined.CalendarMonth
    ),
    ADMIN(
        route = "admin_panel",
        title = "Admin",
        icon = Icons.Outlined.AdminPanelSettings,
        requiresAdmin = true
    )
}

/**
 * AttendanceBottomNavBar: Clean line-icon navigation between Home, Calendar, and Admin.
 * White surface, 1dp top border, Accent indicator.
 */
@Composable
fun AttendanceBottomNavBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items = remember(isAdmin) {
        BottomNavItem.values().filter { !it.requiresAdmin || isAdmin }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.DividerThickness)
                .background(AppBorder)
        )
        NavigationBar(
            containerColor = AppSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            onNavigateToRoute(item.route)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = AppTypographyTokens.CaptionMedium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppAccent,
                        selectedTextColor = AppAccent,
                        indicatorColor = AppAccentTint,
                        unselectedIconColor = AppSecondaryText,
                        unselectedTextColor = AppSecondaryText
                    )
                )
            }
        }
    }
}
