package com.company.attendance.feature.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.core.network.model.AdminSiteDto
import com.company.attendance.core.network.model.AdminUserDto
import com.company.attendance.design.components.AttendanceTextField
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSite by remember { mutableStateOf<AdminSiteDto?>(null) }

    BackHandler(enabled = selectedSite != null) {
        selectedSite = null
    }

    // Sync selectedSite if sites list updates
    LaunchedEffect(uiState.sites) {
        if (selectedSite != null) {
            selectedSite = uiState.sites.find { it.officeId == selectedSite?.officeId }
        }
    }

    Scaffold(
        topBar = {
            if (selectedSite == null) {
                AttendanceTopAppBar(
                    title = "Office Sites",
                    canNavigateBack = false,
                    actions = {
                        IconButton(onClick = viewModel::loadData) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AppPrimaryText
                            )
                        }
                    }
                )
            } else {
                AttendanceTopAppBar(
                    title = selectedSite!!.name,
                    canNavigateBack = true,
                    onNavigateBack = { selectedSite = null },
                    actions = {
                        IconButton(onClick = viewModel::loadData) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AppPrimaryText
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val target = selectedSite?.officeId
                    viewModel.openCreateUserDialog(target)
                },
                containerColor = AppAccent,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
            }
        },
        containerColor = AppBackground,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimensions.ScreenPadding)
            ) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Error Banner
                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing12),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        color = AppErrorTint,
                        border = BorderStroke(1.dp, AppError)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppDimensions.Spacing12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = AppTypographyTokens.BodyMedium,
                                color = AppError,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AppError)
                            }
                        }
                    }
                }

                // Success Banner
                uiState.successMessage?.let { success ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing12),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        color = AppSuccessTint,
                        border = BorderStroke(1.dp, AppSuccess)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppDimensions.Spacing12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = success,
                                style = AppTypographyTokens.BodyMedium,
                                color = AppSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AppSuccess)
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppAccent)
                    }
                } else if (selectedSite == null) {
                    // SITES MENU VIEW
                    SitesMenuView(
                        sites = uiState.sites,
                        users = uiState.users,
                        onSiteSelected = { site -> selectedSite = site }
                    )
                } else {
                    // SITE EMPLOYEES VIEW
                    SiteEmployeesView(
                        site = selectedSite!!,
                        users = uiState.users.filter { it.siteCode == selectedSite!!.officeId },
                        onChangeSiteClick = { user -> viewModel.openSiteChangeDialog(user) }
                    )
                }
            }
        }
    }

    // Create User Dialog
    if (uiState.isCreateUserDialogOpen) {
        CreateUserDialog(
            sites = uiState.sites,
            users = uiState.users,
            targetSiteCode = uiState.targetSiteForNewUser ?: selectedSite?.officeId,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeCreateUserDialog,
            onConfirm = { userId, name, password, siteCode, role ->
                viewModel.createUser(userId, name, password, siteCode, role)
            }
        )
    }

    // Change Site Dialog
    uiState.selectedUserForSiteChange?.let { targetUser ->
        ChangeSiteDialog(
            user = targetUser,
            sites = uiState.sites,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeSiteChangeDialog,
            onConfirm = { newSiteCode ->
                viewModel.updateUserSite(targetUser.userId, newSiteCode)
            }
        )
    }
}

/**
 * Menu of sites view: lists all sites with employee count and location specs.
 */
@Composable
private fun SitesMenuView(
    sites: List<AdminSiteDto>,
    users: List<AdminUserDto>,
    onSiteSelected: (AdminSiteDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sites & Locations (${sites.size})",
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )
            Text(
                text = "Total Users: ${users.size}",
                style = AppTypographyTokens.CaptionMedium,
                color = AppSecondaryText
            )
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing4))

        Text(
            text = "Click a site to view its employees and assign staff",
            style = AppTypographyTokens.Caption,
            color = AppSecondaryText
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

        if (sites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sites configured.",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
            ) {
                items(sites, key = { it.officeId }) { site ->
                    val siteEmployeeCount = users.count { it.siteCode == site.officeId }
                    SiteCardItem(
                        site = site,
                        employeeCount = siteEmployeeCount,
                        onClick = { onSiteSelected(site) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SiteCardItem(
    site: AdminSiteDto,
    employeeCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = AppSurface,
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppAccentTint,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = AppAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing16))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = AppTypographyTokens.SectionHeading,
                    color = AppPrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${site.officeId} • $employeeCount Employees",
                    style = AppTypographyTokens.CaptionMedium,
                    color = AppAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Geofence: ${site.radiusM}m • Threshold: ${site.matchThreshold}",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open Site",
                tint = AppSecondaryText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Site Employees view: lists users assigned to a specific site.
 */
@Composable
private fun SiteEmployeesView(
    site: AdminSiteDto,
    users: List<AdminUserDto>,
    onChangeSiteClick: (AdminUserDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Site details header card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppAccentTint,
            border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing12)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = site.name,
                        style = AppTypographyTokens.SectionHeading,
                        color = AppAccent
                    )
                    Surface(
                        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                        color = AppAccent
                    ) {
                        Text(
                            text = site.officeId,
                            style = AppTypographyTokens.CaptionMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Geofence: ${site.radiusM}m • Accuracy Threshold: ${site.matchThreshold}",
                    style = AppTypographyTokens.Caption,
                    color = AppPrimaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Assigned Employees (${users.size})",
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No employees assigned to ${site.name} yet.\nTap + below to create a user for this site.",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
            ) {
                items(users, key = { it.userId }) { user ->
                    UserItemCard(
                        user = user,
                        onChangeSiteClick = { onChangeSiteClick(user) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun UserItemCard(
    user: AdminUserDto,
    onChangeSiteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = AppSurface,
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Column(modifier = Modifier.padding(AppDimensions.Spacing16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppAccentTint,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                    ) {
                        Text(
                            text = user.name,
                            style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppPrimaryText
                        )

                        if (user.role.equals("ADMIN", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                                color = AppAccentTint
                            ) {
                                Text(
                                    text = "ADMIN",
                                    style = AppTypographyTokens.CaptionMedium,
                                    color = AppAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "ID: ${user.userId}",
                        style = AppTypographyTokens.Caption,
                        color = AppSecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = AppSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing4))
                    Text(
                        text = "${user.siteName} (${user.siteCode})",
                        style = AppTypographyTokens.CaptionMedium,
                        color = AppPrimaryText
                    )
                }

                SecondaryButton(
                    text = "Change Site",
                    onClick = onChangeSiteClick,
                    modifier = Modifier.width(130.dp)
                )
            }
        }
    }
}

/**
 * Calculates next sequential employee ID for a site based on current users.
 * E.g. EMP1001, EMP1002, EMP1003 -> next is EMP1004.
 */
fun generateNextEmployeeId(
    siteCode: String,
    allUsers: List<AdminUserDto>,
    allSites: List<AdminSiteDto>
): String {
    val regex = Regex("""^([A-Za-z_-]*)(\d+)$""")
    val siteUsers = allUsers.filter { it.siteCode.equals(siteCode, ignoreCase = true) }

    val siteCandidates = siteUsers.mapNotNull { user ->
        regex.find(user.userId.trim())?.let { match ->
            val prefix = match.groupValues[1]
            val numStr = match.groupValues[2]
            val num = numStr.toLongOrNull()
            if (num != null) Triple(prefix, num, numStr.length) else null
        }
    }

    if (siteCandidates.isNotEmpty()) {
        val bestPrefix = siteCandidates.groupBy { it.first }.maxByOrNull { it.value.size }?.key ?: "EMP"
        val matching = siteCandidates.filter { it.first == bestPrefix }
        val maxNum = matching.maxOf { it.second }
        val digitsLen = matching.maxOf { it.third }
        val nextNum = maxNum + 1
        val formattedNum = nextNum.toString().padStart(digitsLen, '0')
        return "$bestPrefix$formattedNum"
    }

    // If no users in this site yet, derive base from site index
    val siteIndex = allSites.indexOfFirst { it.officeId.equals(siteCode, ignoreCase = true) }.coerceAtLeast(0)
    val base = (siteIndex + 1) * 1000 + 1
    var candidate = base
    val existingIds = allUsers.map { it.userId.trim().uppercase() }.toSet()
    while (existingIds.contains("EMP$candidate")) {
        candidate++
    }
    return "EMP$candidate"
}

/**
 * Create User Dialog:
 * - Prompts site selection if >1 site and no pre-selection, or allows site picking.
 * - Auto-fills sequential Employee ID according to site serial.
 * - Auto-fills default password (Password@123).
 * - Admin only types Name and clicks Create (minimal work!).
 */
@Composable
private fun CreateUserDialog(
    sites: List<AdminSiteDto>,
    users: List<AdminUserDto>,
    targetSiteCode: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, name: String, password: String, siteCode: String, role: String) -> Unit
) {
    val effectiveSites = if (sites.isEmpty()) {
        listOf(AdminSiteDto("DEFAULT_OFFICE", "Headquarters", 28.490293, 77.060544, 100, "Asia/Kolkata", 0.46f))
    } else sites

    var selectedSiteCode by remember {
        mutableStateOf(
            targetSiteCode ?: effectiveSites.firstOrNull()?.officeId ?: "DEFAULT_OFFICE"
        )
    }

    var userId by remember(selectedSiteCode) {
        mutableStateOf(generateNextEmployeeId(selectedSiteCode, users, effectiveSites))
    }

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("Password@123") }
    var selectedRole by remember { mutableStateOf("EMPLOYEE") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(AppDimensions.Spacing20)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New User",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // 1. SITE PICKER (required if >1 site)
                Text(
                    text = "1. Assigned Office Site",
                    style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPrimaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing4)
                )

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing6)) {
                    effectiveSites.forEach { site ->
                        val isSelected = selectedSiteCode == site.officeId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSiteCode = site.officeId
                                },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isSelected) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = site.name,
                                        style = AppTypographyTokens.BodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) AppAccent else AppPrimaryText
                                    )
                                    Text(
                                        text = "${site.officeId} • ${site.radiusM}m geofence",
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // 2. FULL NAME (primary input needed)
                Text(
                    text = "2. Employee Details",
                    style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPrimaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing4)
                )

                AttendanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name *",
                    placeholder = "e.g. Rahul Sharma"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 3. EMPLOYEE ID (auto-generated)
                AttendanceTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = "Employee ID (Auto-generated)",
                    placeholder = "e.g. EMP1004"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 4. PASSWORD (prefilled default)
                AttendanceTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Default Password",
                    placeholder = "Password@123",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 5. ROLE
                Text(
                    text = "Role",
                    style = AppTypographyTokens.CaptionMedium,
                    color = AppSecondaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing4)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                ) {
                    listOf("EMPLOYEE", "ADMIN").forEach { role ->
                        val isSelected = selectedRole == role
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = role },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isSelected) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) AppAccent else AppBorder)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = role,
                                    style = AppTypographyTokens.CaptionMedium,
                                    color = if (isSelected) AppAccent else AppPrimaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                PrimaryButton(
                    text = if (isLoading) "Creating..." else "Create User",
                    onClick = {
                        onConfirm(userId, name, password, selectedSiteCode, selectedRole)
                    },
                    enabled = !isLoading && userId.isNotBlank() && name.isNotBlank() && password.length >= 6
                )
            }
        }
    }
}

@Composable
private fun ChangeSiteDialog(
    user: AdminUserDto,
    sites: List<AdminSiteDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newSiteCode: String) -> Unit
) {
    var selectedSiteCode by remember { mutableStateOf(user.siteCode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing20)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Site Assignment",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Text(
                    text = "Reassign ${user.name} (${user.userId}) to an office location:",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                    val effectiveSites = if (sites.isEmpty()) {
                        listOf(AdminSiteDto("DEFAULT_OFFICE", "Headquarters", 28.490293, 77.060544, 100, "Asia/Kolkata", 0.46f))
                    } else sites

                    effectiveSites.forEach { site ->
                        val isSelected = selectedSiteCode == site.officeId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSiteCode = site.officeId },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isSelected) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${site.name} (${site.officeId})",
                                    style = AppTypographyTokens.BodyMedium,
                                    color = if (isSelected) AppAccent else AppPrimaryText,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AppAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                PrimaryButton(
                    text = if (isLoading) "Updating..." else "Confirm Change",
                    onClick = { onConfirm(selectedSiteCode) },
                    enabled = !isLoading && selectedSiteCode != user.siteCode
                )
            }
        }
    }
}
