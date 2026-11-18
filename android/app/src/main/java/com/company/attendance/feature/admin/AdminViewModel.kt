package com.company.attendance.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.network.model.AdminUserDto
import com.company.attendance.core.network.model.CreateSiteRequestDto
import com.company.attendance.core.network.model.CreateUserRequestDto
import com.company.attendance.core.network.model.UpdateUserSiteRequestDto
import com.company.attendance.core.network.model.UpdateUserRoleRequestDto
import com.company.attendance.core.network.model.UpdateUserPasswordRequestDto
import com.company.attendance.core.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: AttendanceApiService,
    private val locationChecker: LocationChecker,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isAdmin = tokenStorage.isAdmin(),
                currentRole = tokenStorage.getRole()
            )
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val sitesResponse = apiService.getAdminSites()
                val sitesResult = NetworkHelper.parseResponse(sitesResponse)

                val usersResponse = apiService.getAdminUsers()
                val usersResult = NetworkHelper.parseResponse(usersResponse)

                if (sitesResult.isSuccess && usersResult.isSuccess) {
                    val usersList = usersResult.getOrDefault(emptyList())
                    val currentUserId = tokenStorage.getUserId()
                    val myRecord = usersList.find { it.userId.equals(currentUserId, ignoreCase = true) }
                    if (myRecord != null && !myRecord.role.equals(tokenStorage.getRole(), ignoreCase = true)) {
                        tokenStorage.updateRole(myRecord.role)
                    }
                    val effectiveRole = myRecord?.role ?: tokenStorage.getRole()
                    val effectiveIsAdmin = effectiveRole.equals("ADMIN", ignoreCase = true)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sites = sitesResult.getOrDefault(emptyList()),
                            users = usersList,
                            isAdmin = effectiveIsAdmin,
                            currentRole = effectiveRole
                        )
                    }
                } else {
                    val error = sitesResult.exceptionOrNull()?.message
                        ?: usersResult.exceptionOrNull()?.message
                        ?: "Failed to load admin data."
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to connect to server.")
                }
            }
        }
    }

    fun openCreateUserDialog(targetSiteCode: String? = null) {
        _uiState.update {
            it.copy(
                isCreateUserDialogOpen = true,
                targetSiteForNewUser = targetSiteCode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun closeCreateUserDialog() {
        _uiState.update { it.copy(isCreateUserDialogOpen = false, targetSiteForNewUser = null) }
    }

    fun openCreateSiteDialog() {
        _uiState.update {
            it.copy(
                isCreateSiteDialogOpen = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun closeCreateSiteDialog() {
        _uiState.update { it.copy(isCreateSiteDialogOpen = false) }
    }

    fun fetchCurrentLocation(onLocationFetched: (Double, Double) -> Unit) {
        viewModelScope.launch {
            try {
                val coords = locationChecker.getCurrentCoordinates()
                if (coords != null) {
                    onLocationFetched(coords.first, coords.second)
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Could not fetch GPS coordinates. Please ensure GPS is enabled or enter manually.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to get location: ${e.message}")
                }
            }
        }
    }

    fun createSite(
        officeId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        radiusM: Int = 100
    ) {
        val trimmedId = officeId.trim().uppercase()
        val trimmedName = name.trim()

        if (trimmedId.isBlank() || trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter site name and site code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val request = CreateSiteRequestDto(
                    officeId = trimmedId,
                    name = trimmedName,
                    latitude = latitude,
                    longitude = longitude,
                    radiusM = radiusM,
                    timezone = "Asia/Kolkata",
                    matchThreshold = 0.46f
                )
                val response = apiService.createAdminSite(request)
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { createdSite ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            isCreateSiteDialogOpen = false,
                            sites = current.sites + createdSite,
                            successMessage = "Site '${createdSite.name}' ($trimmedId) created successfully!"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to create site.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to create site.")
                }
            }
        }
    }

    fun openSiteChangeDialog(user: AdminUserDto) {
        _uiState.update { it.copy(selectedUserForSiteChange = user, errorMessage = null, successMessage = null) }
    }

    fun closeSiteChangeDialog() {
        _uiState.update { it.copy(selectedUserForSiteChange = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun createUser(
        userId: String,
        name: String,
        password: String,
        siteCode: String,
        role: String
    ) {
        val trimmedId = userId.trim()
        val trimmedName = name.trim()

        if (trimmedId.isBlank() || trimmedName.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        if (!_uiState.value.isAdmin && role.equals("ADMIN", ignoreCase = true)) {
            _uiState.update { it.copy(errorMessage = "Supervisors cannot create Admin users.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val request = CreateUserRequestDto(
                    userId = trimmedId,
                    name = trimmedName,
                    password = password,
                    siteCode = siteCode,
                    role = role
                )
                val response = apiService.createAdminUser(request)
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { createdUser ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            isCreateUserDialogOpen = false,
                            users = listOf(createdUser) + current.users.filter { it.userId != createdUser.userId },
                            successMessage = "User '${createdUser.userId}' created successfully!"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to create user.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to create user.")
                }
            }
        }
    }

    fun updateUserSite(userId: String, newSiteCode: String) {
        updateUserSites(userId, listOf(newSiteCode))
    }

    fun updateUserSites(userId: String, siteCodes: List<String>) {
        if (siteCodes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "At least one office site must be selected.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = apiService.updateAdminUserSite(
                    userId = userId,
                    request = UpdateUserSiteRequestDto(siteCode = siteCodes.first(), siteCodes = siteCodes)
                )
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { updatedUser ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            selectedUserForSiteChange = null,
                            users = current.users.map { if (it.userId == updatedUser.userId) updatedUser else it },
                            successMessage = "Updated site assignments for '${updatedUser.userId}'."
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to update sites.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to update sites.")
                }
            }
        }
    }

    fun openRoleChangeDialog(user: AdminUserDto) {
        _uiState.update { it.copy(selectedUserForRoleChange = user, errorMessage = null, successMessage = null) }
    }

    fun closeRoleChangeDialog() {
        _uiState.update { it.copy(selectedUserForRoleChange = null) }
    }

    fun updateUserRole(userId: String, newRole: String) {
        if (!_uiState.value.isAdmin) {
            if (newRole.equals("ADMIN", ignoreCase = true)) {
                _uiState.update { it.copy(errorMessage = "Supervisors cannot assign the Admin role.") }
                return
            }
            val targetUser = _uiState.value.users.find { it.userId == userId }
            if (targetUser?.role.equals("ADMIN", ignoreCase = true)) {
                _uiState.update { it.copy(errorMessage = "Supervisors cannot modify Admin users.") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = apiService.updateAdminUserRole(
                    userId = userId,
                    request = UpdateUserRoleRequestDto(role = newRole)
                )
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { updatedUser ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            selectedUserForRoleChange = null,
                            users = current.users.map { if (it.userId == updatedUser.userId) updatedUser else it },
                            successMessage = "Role for '${updatedUser.userId}' updated to ${updatedUser.role}."
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to update user role.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to update user role.")
                }
            }
        }
    }

    fun getCurrentUserId(): String? = tokenStorage.getUserId()

    fun openResetPasswordDialog(user: AdminUserDto) {
        _uiState.update { it.copy(selectedUserForPasswordReset = user, errorMessage = null, successMessage = null) }
    }

    fun closeResetPasswordDialog() {
        _uiState.update { it.copy(selectedUserForPasswordReset = null) }
    }

    fun resetUserPassword(userId: String, newPassword: String) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Supervisors are not permitted to reset passwords.") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = apiService.updateAdminUserPassword(
                    userId = userId,
                    request = UpdateUserPasswordRequestDto(password = newPassword)
                )
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { user ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            selectedUserForPasswordReset = null,
                            successMessage = "Password for '${user.name}' (${user.userId}) updated successfully."
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to reset password.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to reset password.")
                }
            }
        }
    }

    fun openResetPhotoDialog(user: AdminUserDto) {
        _uiState.update { it.copy(selectedUserForPhotoReset = user, errorMessage = null, successMessage = null) }
    }

    fun closeResetPhotoDialog() {
        _uiState.update { it.copy(selectedUserForPhotoReset = null) }
    }

    fun resetUserPhoto(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = apiService.resetAdminUserPhoto(userId = userId)
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { actionResp ->
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            selectedUserForPhotoReset = null,
                            users = current.users.map { u ->
                                if (u.userId == userId) u.copy(photoChangeUsed = false) else u
                            },
                            successMessage = actionResp.message.ifBlank { "Profile photo for user '$userId' reset successfully." }
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to reset photo.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to reset photo.")
                }
            }
        }
    }

    fun openDeleteUserDialog(user: AdminUserDto) {
        _uiState.update { it.copy(selectedUserForDelete = user, errorMessage = null, successMessage = null) }
    }

    fun closeDeleteUserDialog() {
        _uiState.update { it.copy(selectedUserForDelete = null) }
    }

    fun deleteUser(userId: String) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Supervisors are not permitted to delete users.") }
            return
        }
        val currentUserId = tokenStorage.getUserId()
        if (currentUserId?.equals(userId, ignoreCase = true) == true) {
            _uiState.update { it.copy(errorMessage = "You cannot delete your own account.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = apiService.deleteAdminUser(userId = userId)
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isActionLoading = false,
                            selectedUserForDelete = null,
                            users = current.users.filter { it.userId != userId },
                            successMessage = "User '$userId' deleted successfully."
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = err.message ?: "Failed to delete user.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = e.message ?: "Failed to delete user.")
                }
            }
        }
    }
}
