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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.core.network.model.AdminSiteDto
import com.company.attendance.core.network.model.AdminUserDto
import com.company.attendance.design.components.AttendanceTextField
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.components.DangerButton
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppOnAccent
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

enum class AdminTab {
    EMPLOYEES,
    SITES
}

@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(AdminTab.EMPLOYEES) }
    var employeeSearchQuery by remember { mutableStateOf("") }
    var siteSearchQuery by remember { mutableStateOf("") }
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
                val screenTitle = if (currentTab == AdminTab.EMPLOYEES) "Employees" else "Office Sites"
                AttendanceTopAppBar(
                    title = screenTitle,
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    actions = {
                        if (currentTab == AdminTab.EMPLOYEES) {
                            IconButton(onClick = { viewModel.openCreateUserDialog(null) }) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Staff",
                                    tint = AppAccent
                                )
                            }
                        } else {
                            IconButton(onClick = viewModel::openCreateSiteDialog) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Site",
                                    tint = AppAccent
                                )
                            }
                        }
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
                        IconButton(onClick = { viewModel.openCreateUserDialog(selectedSite?.officeId) }) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Staff",
                                tint = AppAccent
                            )
                        }
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

                // Error Alert Banner (Clean Findez style)
                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing12),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        color = AppSurface,
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AppErrorTint,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = AppError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppPrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AppSecondaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Success Alert Banner (Clean Findez style)
                uiState.successMessage?.let { success ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing12),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        color = AppSurface,
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AppSuccessTint,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = AppSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = success,
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppPrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AppSecondaryText, modifier = Modifier.size(16.dp))
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
                } else if (selectedSite != null) {
                    // DRILL-DOWN: SPECIFIC SITE EMPLOYEES VIEW
                    SiteEmployeesView(
                        site = selectedSite!!,
                        users = uiState.users.filter { it.siteCode == selectedSite!!.officeId || it.assignedSites?.any { s -> s.siteCode == selectedSite!!.officeId } == true },
                        currentUserId = viewModel.getCurrentUserId(),
                        isAdmin = uiState.isAdmin,
                        onEditUserClick = { user -> viewModel.openEditUserDialog(user) },
                        onChangeSiteClick = { user -> viewModel.openSiteChangeDialog(user) },
                        onChangeRoleClick = { user -> viewModel.openRoleChangeDialog(user) },
                        onResetPasswordClick = { user -> viewModel.openResetPasswordDialog(user) },
                        onResetPhotoClick = { user -> viewModel.openResetPhotoDialog(user) },
                        onDeleteUserClick = { user -> viewModel.openDeleteUserDialog(user) }
                    )
                } else {
                    // MAIN TWO-TAB INTERFACE: [ EMPLOYEES ] | [ SITES ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing16),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isEmp = currentTab == AdminTab.EMPLOYEES
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { currentTab = AdminTab.EMPLOYEES },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEmp) AppAccent else AppSurface,
                            border = BorderStroke(1.dp, if (isEmp) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = if (isEmp) Color.Black else AppSecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Employees (${uiState.users.size})",
                                    style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isEmp) Color.Black else AppPrimaryText
                                )
                            }
                        }

                        val isSites = currentTab == AdminTab.SITES
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { currentTab = AdminTab.SITES },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSites) AppAccent else AppSurface,
                            border = BorderStroke(1.dp, if (isSites) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (isSites) Color.Black else AppSecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sites (${uiState.sites.size})",
                                    style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSites) Color.Black else AppPrimaryText
                                )
                            }
                        }
                    }

                    if (currentTab == AdminTab.EMPLOYEES) {
                        EmployeesMenuView(
                            users = uiState.users,
                            currentUserId = viewModel.getCurrentUserId(),
                            isAdmin = uiState.isAdmin,
                            searchQuery = employeeSearchQuery,
                            onSearchQueryChange = { employeeSearchQuery = it },
                            onAddUserClick = { viewModel.openCreateUserDialog(null) },
                            onEditUserClick = { user -> viewModel.openEditUserDialog(user) },
                            onChangeSiteClick = { user -> viewModel.openSiteChangeDialog(user) },
                            onChangeRoleClick = { user -> viewModel.openRoleChangeDialog(user) },
                            onResetPasswordClick = { user -> viewModel.openResetPasswordDialog(user) },
                            onResetPhotoClick = { user -> viewModel.openResetPhotoDialog(user) },
                            onDeleteUserClick = { user -> viewModel.openDeleteUserDialog(user) }
                        )
                    } else {
                        SitesMenuView(
                            sites = uiState.sites,
                            users = uiState.users,
                            searchQuery = siteSearchQuery,
                            onSearchQueryChange = { siteSearchQuery = it },
                            onSiteSelected = { site -> selectedSite = site },
                            onAddSiteClick = viewModel::openCreateSiteDialog,
                            onEditSiteClick = { site -> viewModel.openEditSiteDialog(site) },
                            onDeactivateSiteClick = { site -> viewModel.openDeactivateSiteDialog(site) },
                            onActivateSiteClick = { site -> viewModel.activateSite(site.officeId) }
                        )
                    }
                }
            }
        }
    }

    // Create Site Dialog
    if (uiState.isCreateSiteDialogOpen) {
        CreateSiteDialog(
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeCreateSiteDialog,
            onFetchLocation = { onCoords ->
                viewModel.fetchCurrentLocation(onCoords)
            },
            onConfirm = { officeId, name, lat, lng, radiusM ->
                viewModel.createSite(officeId, name, lat, lng, radiusM)
            }
        )
    }

    // Create User Dialog
    if (uiState.isCreateUserDialogOpen) {
        CreateUserDialog(
            sites = uiState.sites,
            users = uiState.users,
            targetSiteCode = uiState.targetSiteForNewUser ?: selectedSite?.officeId,
            isLoading = uiState.isActionLoading,
            isAdmin = uiState.isAdmin,
            onDismiss = viewModel::closeCreateUserDialog,
            onConfirm = { userId, name, password, siteCode, role ->
                viewModel.createUser(userId, name, password, siteCode, role)
            }
        )
    }

    // Manage Sites Dialog (Multi-site Assignment)
    uiState.selectedUserForSiteChange?.let { targetUser ->
        ManageSitesDialog(
            user = targetUser,
            sites = uiState.sites,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeSiteChangeDialog,
            onConfirm = { newSiteCodes ->
                viewModel.updateUserSites(targetUser.userId, newSiteCodes)
            }
        )
    }

    // Change Role Dialog
    uiState.selectedUserForRoleChange?.let { targetUser ->
        ChangeRoleDialog(
            user = targetUser,
            isAdmin = uiState.isAdmin,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeRoleChangeDialog,
            onConfirm = { newRole ->
                viewModel.updateUserRole(targetUser.userId, newRole)
            }
        )
    }

    // Reset Password Dialog
    uiState.selectedUserForPasswordReset?.let { targetUser ->
        AdminResetPasswordDialog(
            user = targetUser,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeResetPasswordDialog,
            onConfirm = { newPassword ->
                viewModel.resetUserPassword(targetUser.userId, newPassword)
            }
        )
    }

    // Reset Photo Confirmation Dialog
    uiState.selectedUserForPhotoReset?.let { targetUser ->
        AdminResetPhotoDialog(
            user = targetUser,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeResetPhotoDialog,
            onConfirm = {
                viewModel.resetUserPhoto(targetUser.userId)
            }
        )
    }

    // Delete User Confirmation Dialog
    uiState.selectedUserForDelete?.let { targetUser ->
        AdminDeleteUserDialog(
            user = targetUser,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeDeleteUserDialog,
            onConfirm = {
                viewModel.deleteUser(targetUser.userId)
            }
        )
    }

    // Edit Site Dialog
    uiState.selectedSiteForEdit?.let { targetSite ->
        EditSiteDialog(
            site = targetSite,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeEditSiteDialog,
            onFetchLocation = { onCoords ->
                viewModel.fetchCurrentLocation(onCoords)
            },
            onConfirm = { name, lat, lng, radiusM ->
                viewModel.updateSite(targetSite.officeId, name, lat, lng, radiusM)
            }
        )
    }

    // Deactivate Site Dialog
    uiState.selectedSiteForDeactivate?.let { targetSite ->
        DeactivateSiteDialog(
            site = targetSite,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeDeactivateSiteDialog,
            onConfirm = {
                viewModel.deactivateSite(targetSite.officeId)
            }
        )
    }

    // Edit User Dialog
    uiState.selectedUserForEdit?.let { targetUser ->
        EditUserDialog(
            user = targetUser,
            sites = uiState.sites,
            isAdmin = uiState.isAdmin,
            isLoading = uiState.isActionLoading,
            onDismiss = viewModel::closeEditUserDialog,
            onConfirm = { name, role, siteCode, siteCodes ->
                viewModel.updateUser(targetUser.userId, name, role, siteCode, siteCodes)
            }
        )
    }
}

@Composable
private fun AdminSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = AppTypographyTokens.BodyMedium, color = AppSecondaryText) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = AppSecondaryText, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppSecondaryText, modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppSurface,
            unfocusedContainerColor = AppSurface,
            focusedBorderColor = AppAccent,
            unfocusedBorderColor = AppBorder,
            focusedTextColor = AppPrimaryText,
            unfocusedTextColor = AppPrimaryText
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun EmployeesMenuView(
    users: List<AdminUserDto>,
    currentUserId: String?,
    isAdmin: Boolean = true,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddUserClick: () -> Unit,
    onEditUserClick: (AdminUserDto) -> Unit,
    onChangeSiteClick: (AdminUserDto) -> Unit,
    onChangeRoleClick: (AdminUserDto) -> Unit,
    onResetPasswordClick: (AdminUserDto) -> Unit,
    onResetPhotoClick: (AdminUserDto) -> Unit,
    onDeleteUserClick: (AdminUserDto) -> Unit
) {
    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) {
            users
        } else {
            val q = searchQuery.trim().lowercase()
            users.filter {
                it.name.lowercase().contains(q) || it.userId.lowercase().contains(q) || it.role.lowercase().contains(q) || it.siteName.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search by name, ID or role..."
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Staff Directory (${filteredUsers.size})",
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddUserClick),
                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppPrimaryText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Staff", style = AppTypographyTokens.CaptionMedium, color = AppPrimaryText)
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No employees found." else "No staff found matching '$searchQuery'.",
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
                items(filteredUsers, key = { it.userId }) { user ->
                    UserItemCard(
                        user = user,
                        isSelf = currentUserId?.equals(user.userId, ignoreCase = true) == true,
                        isAdmin = isAdmin,
                        onEditUserClick = { onEditUserClick(user) },
                        onChangeSiteClick = { onChangeSiteClick(user) },
                        onChangeRoleClick = { onChangeRoleClick(user) },
                        onResetPasswordClick = { onResetPasswordClick(user) },
                        onResetPhotoClick = { onResetPhotoClick(user) },
                        onDeleteUserClick = { onDeleteUserClick(user) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Menu of sites view: lists all sites with employee count and location specs.
 */
@Composable
private fun SitesMenuView(
    sites: List<AdminSiteDto>,
    users: List<AdminUserDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSiteSelected: (AdminSiteDto) -> Unit,
    onAddSiteClick: () -> Unit,
    onEditSiteClick: (AdminSiteDto) -> Unit,
    onDeactivateSiteClick: (AdminSiteDto) -> Unit,
    onActivateSiteClick: (AdminSiteDto) -> Unit
) {
    val filteredSites = remember(sites, searchQuery) {
        if (searchQuery.isBlank()) {
            sites
        } else {
            val q = searchQuery.trim().lowercase()
            sites.filter {
                it.name.lowercase().contains(q) || it.officeId.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search sites by name or ID..."
        )

        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Locations (${filteredSites.size})",
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddSiteClick),
                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppPrimaryText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Site", style = AppTypographyTokens.CaptionMedium, color = AppPrimaryText)
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

        if (filteredSites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isBlank()) "No sites configured." else "No sites found matching '$searchQuery'.",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText
                    )
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))
                        PrimaryButton(
                            text = "Create First Site",
                            onClick = onAddSiteClick,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
            ) {
                items(filteredSites, key = { it.officeId }) { site ->
                    val siteEmployeeCount = users.count {
                        it.siteCode == site.officeId || it.assignedSites?.any { s -> s.siteCode == site.officeId } == true
                    }
                    SiteCardItem(
                        site = site,
                        employeeCount = siteEmployeeCount,
                        onClick = { onSiteSelected(site) },
                        onEditClick = { onEditSiteClick(site) },
                        onDeactivateClick = { onDeactivateSiteClick(site) },
                        onActivateClick = { onActivateSiteClick(site) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SiteCardItem(
    site: AdminSiteDto,
    employeeCount: Int,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = site.isActive

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
                color = if (isActive) AppAccentTint else AppBackground,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = if (isActive) AppAccent else AppSecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimensions.Spacing16))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = site.name,
                        style = AppTypographyTokens.SectionHeading,
                        color = if (isActive) AppPrimaryText else AppSecondaryText
                    )
                    if (!isActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppBackground,
                            border = BorderStroke(1.dp, AppBorder)
                        ) {
                            Text(
                                text = "DEACTIVATED",
                                style = AppTypographyTokens.Caption,
                                color = AppSecondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${site.officeId} • ${employeeCount} active staff • ${site.radiusM}m geofence",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        tint = AppSecondaryText
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AppSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Site", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccent) },
                        onClick = {
                            menuExpanded = false
                            onEditClick()
                        }
                    )
                    if (isActive) {
                        DropdownMenuItem(
                            text = { Text("Deactivate Site", style = AppTypographyTokens.BodyMedium, color = AppError) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppError) },
                            onClick = {
                                menuExpanded = false
                                onDeactivateClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Activate Site", style = AppTypographyTokens.BodyMedium, color = AppSuccess) },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = AppSuccess) },
                            onClick = {
                                menuExpanded = false
                                onActivateClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteEmployeesView(
    site: AdminSiteDto,
    users: List<AdminUserDto>,
    currentUserId: String?,
    isAdmin: Boolean = true,
    onEditUserClick: (AdminUserDto) -> Unit,
    onChangeSiteClick: (AdminUserDto) -> Unit,
    onChangeRoleClick: (AdminUserDto) -> Unit,
    onResetPasswordClick: (AdminUserDto) -> Unit,
    onResetPhotoClick: (AdminUserDto) -> Unit,
    onDeleteUserClick: (AdminUserDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                        isSelf = currentUserId?.equals(user.userId, ignoreCase = true) == true,
                        isAdmin = isAdmin,
                        onEditUserClick = { onEditUserClick(user) },
                        onChangeSiteClick = { onChangeSiteClick(user) },
                        onChangeRoleClick = { onChangeRoleClick(user) },
                        onResetPasswordClick = { onResetPasswordClick(user) },
                        onResetPhotoClick = { onResetPhotoClick(user) },
                        onDeleteUserClick = { onDeleteUserClick(user) }
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
    isSelf: Boolean,
    isAdmin: Boolean = true,
    onEditUserClick: () -> Unit,
    onChangeSiteClick: () -> Unit,
    onChangeRoleClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onResetPhotoClick: () -> Unit,
    onDeleteUserClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isTargetAdmin = user.role.equals("ADMIN", ignoreCase = true)
    val canManageUser = isAdmin || !isTargetAdmin

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = AppSurface,
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppAccentTint,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppAccent
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
                            color = AppBackground,
                            border = BorderStroke(1.dp, AppBorder)
                        ) {
                            Text(
                                text = "ADMIN",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppSecondaryText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (user.role.equals("SUPERVISOR", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = AppAccentTint,
                            border = BorderStroke(1.dp, AppAccent)
                        ) {
                            Text(
                                text = "SUPERVISOR",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val sitesText = if (!user.assignedSites.isNullOrEmpty()) {
                    user.assignedSites.joinToString(", ") { it.siteName }
                } else {
                    user.siteName.ifBlank { user.siteCode }
                }

                Text(
                    text = "${user.userId} • $sitesText",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canManageUser) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = AppSecondaryText
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(AppSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccent) },
                            onClick = {
                                menuExpanded = false
                                onEditUserClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Sites", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = AppAccent) },
                            onClick = {
                                menuExpanded = false
                                onChangeSiteClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Role", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppAccent) },
                            onClick = {
                                menuExpanded = false
                                onChangeRoleClick()
                            }
                        )
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Reset Password", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AppAccent) },
                                onClick = {
                                    menuExpanded = false
                                    onResetPasswordClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Reset Photo", style = AppTypographyTokens.BodyMedium, color = AppPrimaryText) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = AppAccent) },
                            onClick = {
                                menuExpanded = false
                                onResetPhotoClick()
                            }
                        )
                        if (isAdmin && !isSelf) {
                            DropdownMenuItem(
                                text = { Text("Delete User", style = AppTypographyTokens.BodyMedium, color = AppError) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppError) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteUserClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    isAdmin: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, name: String, password: String, siteCode: String, role: String) -> Unit
) {
    val effectiveSites = if (sites.isEmpty()) {
        listOf(AdminSiteDto("DEFAULT_OFFICE", "Headquarters", 28.490293, 77.060544, 100, "Asia/Kolkata", 0.46f))
    } else sites

    var selectedRole by remember { mutableStateOf("EMPLOYEE") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    val availableRoles = remember(isAdmin) {
        if (isAdmin) listOf("EMPLOYEE", "SUPERVISOR", "ADMIN") else listOf("EMPLOYEE", "SUPERVISOR")
    }

    var selectedSiteCode by remember {
        mutableStateOf(
            targetSiteCode ?: effectiveSites.firstOrNull()?.officeId ?: "DEFAULT_OFFICE"
        )
    }
    var siteDropdownExpanded by remember { mutableStateOf(false) }

    var userId by remember(selectedSiteCode) {
        mutableStateOf(generateNextEmployeeId(selectedSiteCode, users, effectiveSites))
    }

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("Password@123") }

    val selectedSiteName = remember(selectedSiteCode, effectiveSites) {
        effectiveSites.find { it.officeId == selectedSiteCode }?.name ?: selectedSiteCode
    }

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

                // 1. FULL NAME
                AttendanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name *",
                    placeholder = "e.g. Rahul Sharma"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 2. EMPLOYEE ID (auto-generated)
                AttendanceTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = "Employee ID (Auto-generated)",
                    placeholder = "e.g. EMP1004"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 3. PASSWORD
                AttendanceTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Default Password",
                    placeholder = "Password@123",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 4. ROLE DROPDOWN
                Text(
                    text = "Role",
                    style = AppTypographyTokens.CaptionMedium,
                    color = AppSecondaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing4)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roleDropdownExpanded = true },
                        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                        color = AppBackground,
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedRole,
                                style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AppPrimaryText
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Role",
                                tint = AppSecondaryText
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                        modifier = Modifier.background(AppSurface)
                    ) {
                        availableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = role,
                                        style = AppTypographyTokens.BodyMedium.copy(
                                            fontWeight = if (role == selectedRole) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (role == selectedRole) AppAccent else AppPrimaryText
                                    )
                                },
                                onClick = {
                                    selectedRole = role
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 5. ASSIGNED OFFICE SITE DROPDOWN (below role!)
                Text(
                    text = "Assigned Office Site",
                    style = AppTypographyTokens.CaptionMedium,
                    color = AppSecondaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing4)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { siteDropdownExpanded = true },
                        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                        color = AppBackground,
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSiteName,
                                style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AppPrimaryText
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Site",
                                tint = AppSecondaryText
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = siteDropdownExpanded,
                        onDismissRequest = { siteDropdownExpanded = false },
                        modifier = Modifier.background(AppSurface)
                    ) {
                        effectiveSites.forEach { site ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = site.name,
                                        style = AppTypographyTokens.BodyMedium.copy(
                                            fontWeight = if (site.officeId == selectedSiteCode) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (site.officeId == selectedSiteCode) AppAccent else AppPrimaryText
                                    )
                                },
                                onClick = {
                                    selectedSiteCode = site.officeId
                                    siteDropdownExpanded = false
                                }
                            )
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
private fun ManageSitesDialog(
    user: AdminUserDto,
    sites: List<AdminSiteDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newSiteCodes: List<String>) -> Unit
) {
    val initialCodes = remember(user) {
        if (!user.assignedSites.isNullOrEmpty()) {
            user.assignedSites.map { it.siteCode }.toSet()
        } else {
            setOf(user.siteCode)
        }
    }
    var selectedSiteCodes by remember(user) { mutableStateOf(initialCodes) }

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
                        text = "Manage Sites",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Text(
                    text = "Assign ${user.name} (${user.userId}) to one or more office locations:",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                    val effectiveSites = if (sites.isEmpty()) {
                        listOf(AdminSiteDto("DEFAULT_OFFICE", "Headquarters", 28.490293, 77.060544, 100, "Asia/Kolkata", 0.46f))
                    } else sites

                    effectiveSites.forEach { site ->
                        val isChecked = selectedSiteCodes.contains(site.officeId)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSiteCodes = if (isChecked) {
                                        if (selectedSiteCodes.size > 1) selectedSiteCodes - site.officeId else selectedSiteCodes
                                    } else {
                                        selectedSiteCodes + site.officeId
                                    }
                                },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isChecked) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isChecked) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSiteCodes = if (checked) {
                                            selectedSiteCodes + site.officeId
                                        } else {
                                            if (selectedSiteCodes.size > 1) selectedSiteCodes - site.officeId else selectedSiteCodes
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AppAccent,
                                        uncheckedColor = AppSecondaryText
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = site.name,
                                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isChecked) AppAccent else AppPrimaryText
                                    )
                                    Text(
                                        text = "${site.officeId} • ${site.radiusM}m geofence",
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                val hasChanges = selectedSiteCodes != initialCodes
                PrimaryButton(
                    text = if (isLoading) "Saving Sites..." else "Save Assigned Sites (${selectedSiteCodes.size})",
                    onClick = { onConfirm(selectedSiteCodes.toList()) },
                    enabled = !isLoading && selectedSiteCodes.isNotEmpty() && hasChanges
                )
            }
        }
    }
}

@Composable
private fun ChangeRoleDialog(
    user: AdminUserDto,
    isAdmin: Boolean = true,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newRole: String) -> Unit
) {
    val allRoles = listOf(
        Triple("EMPLOYEE", "Regular Staff", "Attendance check-in/out and field tracking"),
        Triple("SUPERVISOR", "Site Supervisor", "Staff oversight, view attendance, manage site ops"),
        Triple("ADMIN", "System Administrator", "Full system access, manage all users and sites")
    )

    val roles = remember(isAdmin) {
        if (!isAdmin) allRoles.filter { it.first != "ADMIN" } else allRoles
    }

    var selectedRole by remember(user, isAdmin) {
        val initial = user.role.uppercase()
        mutableStateOf(if (!isAdmin && initial == "ADMIN") "EMPLOYEE" else initial)
    }

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
                        text = "Change User Role",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Text(
                    text = "Select access role for ${user.name} (${user.userId}):",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppSecondaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                    roles.forEach { (roleKey, roleTitle, roleDesc) ->
                        val isSelected = selectedRole.equals(roleKey, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = roleKey },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isSelected) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$roleKey • $roleTitle",
                                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                        color = if (isSelected) AppAccent else AppPrimaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = roleDesc,
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                PrimaryButton(
                    text = if (isLoading) "Updating..." else "Confirm Role Change",
                    onClick = { onConfirm(selectedRole) },
                    enabled = !isLoading && !selectedRole.equals(user.role, ignoreCase = true)
                )
            }
        }
    }
}

/**
 * Dialog to create a new office site directly from the app.
 * - Enter Site Name (auto-suggests Site Code / Office ID, e.g. "SITE_NOIDA")
 * - Site Code is editable
 * - GPS Latitude and Longitude with one-tap "Use Current GPS" button
 * - Geofence Radius (prefilled 100 meters)
 * - Automatic global defaults (Threshold 0.46, Timezone Asia/Kolkata)
 */
@Composable
private fun CreateSiteDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onFetchLocation: ((Double, Double) -> Unit) -> Unit,
    onConfirm: (officeId: String, name: String, lat: Double, lng: Double, radiusM: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var officeId by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("100") }
    var isFetchingGps by remember { mutableStateOf(false) }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Office Site",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // 1. Site Name
                AttendanceTextField(
                    value = name,
                    onValueChange = { newName ->
                        name = newName
                        val slug = newName.trim().uppercase()
                            .replace("[^A-Z0-9]+".toRegex(), "_")
                            .trim('_')
                        officeId = if (slug.isNotEmpty()) "SITE_$slug" else ""
                    },
                    label = "Site Name *",
                    placeholder = "e.g. Noida Branch"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 2. Site Code (Auto-generated, non-editable)
                AttendanceTextField(
                    value = officeId,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = "Site Code (Auto-generated)",
                    placeholder = "Auto-generated from name"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 3. Coordinates Section with GPS Fetch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS Coordinates *",
                        style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppPrimaryText
                    )
                    Surface(
                        modifier = Modifier.clickable(enabled = !isFetchingGps) {
                            isFetchingGps = true
                            onFetchLocation { lat, lng ->
                                latitudeText = "%.6f".format(Locale.US, lat)
                                longitudeText = "%.6f".format(Locale.US, lng)
                                isFetchingGps = false
                            }
                        },
                        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                        color = AppAccentTint,
                        border = BorderStroke(1.dp, AppAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = AppAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFetchingGps) "Getting GPS..." else "Use Current GPS",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                ) {
                    AttendanceTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = "Latitude",
                        placeholder = "28.535516",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = "Longitude",
                        placeholder = "77.391026",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // 4. Geofence Radius
                AttendanceTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it.filter { ch -> ch.isDigit() } },
                    label = "Geofence Radius (meters)",
                    placeholder = "100",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                val parsedLat = latitudeText.toDoubleOrNull()
                val parsedLng = longitudeText.toDoubleOrNull()
                val parsedRadius = radiusText.toIntOrNull() ?: 100
                val isFormValid = name.isNotBlank() &&
                        officeId.isNotBlank() &&
                        parsedLat != null &&
                        parsedLng != null &&
                        parsedRadius in 10..10000

                PrimaryButton(
                    text = if (isLoading) "Creating Site..." else "Create Site",
                    onClick = {
                        if (parsedLat != null && parsedLng != null) {
                            onConfirm(officeId.trim(), name.trim(), parsedLat, parsedLng, parsedRadius)
                        }
                    },
                    enabled = !isLoading && isFormValid
                )
            }
        }
    }
}

/**
 * Dialog to edit an existing office site.
 * - Site Code / Office ID is displayed as non-editable
 * - Site Name is editable
 * - GPS Latitude and Longitude editable with "Use Current GPS"
 * - Geofence Radius editable
 */
@Composable
private fun EditSiteDialog(
    site: AdminSiteDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onFetchLocation: ((Double, Double) -> Unit) -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double, radiusM: Int) -> Unit
) {
    var name by remember { mutableStateOf(site.name) }
    var latitudeText by remember { mutableStateOf("%.6f".format(Locale.US, site.latitude)) }
    var longitudeText by remember { mutableStateOf("%.6f".format(Locale.US, site.longitude)) }
    var radiusText by remember { mutableStateOf(site.radiusM.toString()) }
    var isFetchingGps by remember { mutableStateOf(false) }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Office Site",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Site Code (Non-editable)
                AttendanceTextField(
                    value = site.officeId,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = "Site Code (Non-editable)",
                    placeholder = site.officeId
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // Site Name
                AttendanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Site Name *",
                    placeholder = "e.g. Noida Branch"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // Coordinates Section with GPS Fetch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS Coordinates *",
                        style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppPrimaryText
                    )
                    Surface(
                        modifier = Modifier.clickable(enabled = !isFetchingGps) {
                            isFetchingGps = true
                            onFetchLocation { lat, lng ->
                                latitudeText = "%.6f".format(Locale.US, lat)
                                longitudeText = "%.6f".format(Locale.US, lng)
                                isFetchingGps = false
                            }
                        },
                        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                        color = AppAccentTint,
                        border = BorderStroke(1.dp, AppAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = AppAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFetchingGps) "Getting GPS..." else "Use Current GPS",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)
                ) {
                    AttendanceTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = "Latitude",
                        placeholder = "28.535516",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = "Longitude",
                        placeholder = "77.391026",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // Geofence Radius
                AttendanceTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it.filter { ch -> ch.isDigit() } },
                    label = "Geofence Radius (meters)",
                    placeholder = "100",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                val parsedLat = latitudeText.toDoubleOrNull()
                val parsedLng = longitudeText.toDoubleOrNull()
                val parsedRadius = radiusText.toIntOrNull() ?: 100
                val isFormValid = name.isNotBlank() &&
                        parsedLat != null &&
                        parsedLng != null &&
                        parsedRadius in 10..10000

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = if (isLoading) "Saving..." else "Save Changes",
                        onClick = {
                            if (parsedLat != null && parsedLng != null) {
                                onConfirm(name.trim(), parsedLat, parsedLng, parsedRadius)
                            }
                        },
                        enabled = !isLoading && isFormValid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Confirmation dialog to deactivate a site.
 * Warns that if employees are assigned to it, deactivation will be rejected.
 */
@Composable
private fun DeactivateSiteDialog(
    site: AdminSiteDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.ScreenPadding)
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing20)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deactivate Site",
                        style = AppTypographyTokens.SectionHeading,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                Text(
                    text = "Are you sure you want to deactivate \"${site.name}\" (${site.officeId})?",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                Surface(
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppErrorTint,
                    border = BorderStroke(1.dp, AppError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppDimensions.Spacing12)) {
                        Text(
                            text = "Warning: If any employees are currently assigned to this site as their primary or secondary site, deactivation will be blocked until their site assignments are changed.",
                            style = AppTypographyTokens.Caption,
                            color = AppError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    DangerButton(
                        text = if (isLoading) "Deactivating..." else "Deactivate",
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Dialog to edit employee details (Full Name, Role, Assigned Sites).
 * Employee ID is non-editable.
 */
@Composable
private fun EditUserDialog(
    user: AdminUserDto,
    sites: List<AdminSiteDto>,
    isAdmin: Boolean = true,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, siteCode: String, siteCodes: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var selectedRole by remember(user, isAdmin) {
        val initial = user.role.uppercase()
        mutableStateOf(if (!isAdmin && initial == "ADMIN") "EMPLOYEE" else initial)
    }

    val allRoles = listOf(
        Triple("EMPLOYEE", "Regular Staff", "Attendance check-in/out and field tracking"),
        Triple("SUPERVISOR", "Site Supervisor", "Staff oversight, view attendance, manage site ops"),
        Triple("ADMIN", "System Administrator", "Full system access, manage all users and sites")
    )
    val availableRoles = remember(isAdmin) {
        if (!isAdmin) allRoles.filter { it.first != "ADMIN" } else allRoles
    }

    var selectedSiteCodes by remember(user) {
        val assigned = user.assignedSites?.map { it.siteCode } ?: emptyList()
        val currentSites = (listOf(user.siteCode) + assigned).filter { it.isNotBlank() }.distinct()
        mutableStateOf<List<String>>(currentSites.ifEmpty { listOf(sites.firstOrNull()?.officeId ?: "DEFAULT_OFFICE") })
    }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Employee Details",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Employee ID (read-only)
                AttendanceTextField(
                    value = user.userId,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = "Employee ID (Non-editable)",
                    placeholder = user.userId
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                // Full Name
                AttendanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name *",
                    placeholder = "e.g. John Doe"
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Role Selection
                Text(
                    text = "Role *",
                    style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPrimaryText
                )
                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                    availableRoles.forEach { (roleKey, roleTitle, roleDesc) ->
                        val isSelected = selectedRole.equals(roleKey, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = roleKey },
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
                                        text = "$roleKey • $roleTitle",
                                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) AppAccent else AppPrimaryText
                                    )
                                    Text(
                                        text = roleDesc,
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Assigned Sites (Multi-select)
                Text(
                    text = "Assigned Sites *",
                    style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPrimaryText
                )
                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                val effectiveSites = sites.filter { it.isActive }
                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                    effectiveSites.forEach { site ->
                        val isChecked = selectedSiteCodes.contains(site.officeId)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSiteCodes = if (isChecked) {
                                        if (selectedSiteCodes.size > 1) selectedSiteCodes - site.officeId else selectedSiteCodes
                                    } else {
                                        selectedSiteCodes + site.officeId
                                    }
                                },
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = if (isChecked) AppAccentTint else AppBackground,
                            border = BorderStroke(1.dp, if (isChecked) AppAccent else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSiteCodes = if (checked) {
                                            selectedSiteCodes + site.officeId
                                        } else {
                                            if (selectedSiteCodes.size > 1) selectedSiteCodes - site.officeId else selectedSiteCodes
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AppAccent,
                                        uncheckedColor = AppSecondaryText
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = site.name,
                                        style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isChecked) AppAccent else AppPrimaryText
                                    )
                                    Text(
                                        text = "${site.officeId} • ${site.radiusM}m geofence",
                                        style = AppTypographyTokens.Caption,
                                        color = AppSecondaryText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                val isFormValid = name.isNotBlank() && selectedSiteCodes.isNotEmpty()
                val primarySite = if (selectedSiteCodes.contains(user.siteCode)) user.siteCode else selectedSiteCodes.first()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = if (isLoading) "Saving..." else "Save Changes",
                        onClick = {
                            onConfirm(name.trim(), selectedRole, primarySite, selectedSiteCodes)
                        },
                        enabled = !isLoading && isFormValid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminResetPasswordDialog(
    user: AdminUserDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.ScreenPadding)
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing20)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset Password",
                        style = AppTypographyTokens.SectionHeading,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Text(
                    text = "Set a new password for ${user.name} (${user.userId}). Minimum 6 characters required.",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                AttendanceTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password",
                    placeholder = "Enter new password",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = AppSecondaryText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = if (isLoading) "Saving..." else "Update",
                        onClick = { onConfirm(newPassword) },
                        enabled = !isLoading && newPassword.length >= 6,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDeleteUserDialog(
    user: AdminUserDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.ScreenPadding)
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing20)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Delete Employee",
                        style = AppTypographyTokens.SectionHeading,
                        color = AppError
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                Text(
                    text = "Are you sure you want to permanently delete ${user.name} (${user.userId})?",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Surface(
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppErrorTint,
                    border = BorderStroke(1.dp, AppError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Warning: This user account and their records will be removed. This action cannot be undone.",
                        style = AppTypographyTokens.Caption,
                        color = AppError,
                        modifier = Modifier.padding(AppDimensions.Spacing12)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppError,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isLoading) "Deleting..." else "Delete",
                            style = AppTypographyTokens.ButtonText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AdminResetPhotoDialog(
    user: AdminUserDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.ScreenPadding)
        ) {
            Column(modifier = Modifier.padding(AppDimensions.Spacing20)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset Profile Photo",
                        style = AppTypographyTokens.SectionHeading,
                        color = AppPrimaryText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                Text(
                    text = "Are you sure you want to reset the profile photo for ${user.name} (${user.userId})?",
                    style = AppTypographyTokens.BodyMedium,
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                Surface(
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppAccentTint,
                    border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "The user will be able to re-enrol their profile photo from their profile screen. Attendance and punch records are not affected.",
                        style = AppTypographyTokens.Caption,
                        color = AppAccent,
                        modifier = Modifier.padding(AppDimensions.Spacing12)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing12)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = if (isLoading) "Resetting..." else "Reset Photo",
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
