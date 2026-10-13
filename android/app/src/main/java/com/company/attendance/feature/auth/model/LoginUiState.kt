package com.company.attendance.feature.auth.model

sealed interface LoginNavigationEvent {
    data object NavigateToHome : LoginNavigationEvent
    data object NavigateToOnboarding : LoginNavigationEvent
}

data class LoginUiState(
    val userId: String = "",
    val password: String = "",
    val userIdError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null
) {
    val isFormValid: Boolean
        get() = userId.isNotBlank() && password.isNotBlank() && userIdError == null && passwordError == null
}
