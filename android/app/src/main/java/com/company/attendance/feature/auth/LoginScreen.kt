package com.company.attendance.feature.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.R
import com.company.attendance.design.components.AttendanceTextField
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.feature.auth.model.LoginNavigationEvent
import com.company.attendance.feature.auth.model.LoginUiState

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                LoginNavigationEvent.NavigateToHome -> onNavigateToHome()
                LoginNavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onUserIdChanged = viewModel::onUserIdChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::login,
        modifier = modifier
    )
}

@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onUserIdChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.ScreenPadding, vertical = AppDimensions.Spacing32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Findez Iconic Brand Emblem (Clean, bold, modern)
            Surface(
                shape = CircleShape,
                color = AppAccent,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_findez_punch_in),
                        contentDescription = "Findez Logo",
                        tint = AppBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            // Findez Bold Hero Headline
            Text(
                text = "Welcome to Findez",
                style = AppTypographyTokens.HeroTitle.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppPrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

            Text(
                text = "Sign in with your employee credentials",
                style = AppTypographyTokens.Body.copy(
                    fontSize = 14.sp,
                    color = AppSecondaryText
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

            // General error banner
            if (!uiState.generalError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimensions.Spacing16),
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
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
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = AppError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.generalError,
                            style = AppTypographyTokens.CaptionMedium,
                            color = AppPrimaryText
                        )
                    }
                }
            }

            // User ID Input
            AttendanceTextField(
                value = uiState.userId,
                onValueChange = onUserIdChanged,
                label = "User ID / Employee ID",
                placeholder = "e.g. EMP2001",
                isError = uiState.userIdError != null,
                errorMessage = uiState.userIdError,
                enabled = !uiState.isLoading,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AppSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

            // Password Input
            AttendanceTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                label = "Password",
                placeholder = "Enter your password",
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                enabled = !uiState.isLoading,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AppSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = AppSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                )
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            // Sign In Action Button
            PrimaryButton(
                text = "Sign In",
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            // Sleek system footer
            Text(
                text = "Accounts are provisioned by your system administrator.",
                style = AppTypographyTokens.Caption.copy(
                    fontSize = 12.sp,
                    color = AppSecondaryText.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
        }
    }
}
