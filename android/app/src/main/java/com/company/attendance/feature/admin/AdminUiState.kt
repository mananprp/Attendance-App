package com.company.attendance.feature.admin

import com.company.attendance.core.network.model.AdminSiteDto
import com.company.attendance.core.network.model.AdminUserDto

data class AdminUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val sites: List<AdminSiteDto> = emptyList(),
    val users: List<AdminUserDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isCreateUserDialogOpen: Boolean = false,
    val isCreateSiteDialogOpen: Boolean = false,
    val targetSiteForNewUser: String? = null,
    val selectedUserForSiteChange: AdminUserDto? = null,
    val selectedUserForPasswordReset: AdminUserDto? = null,
    val selectedUserForDelete: AdminUserDto? = null
)
