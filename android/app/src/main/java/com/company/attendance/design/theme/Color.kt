package com.company.attendance.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Attendance App Findez Dark Theme Color Palette (UI Revamp Plan Section 3.1)
 * Dark theme only. High contrast, clean, mint accent.
 */
val AppBackground = Color(0xFF070B0F)
val AppHeaderBackground = Color(0xFF14181D)
val AppSurface = Color(0xFF14181D)
val AppDisabled = Color(0xFF0C1015)
val AppIconCircle = Color(0xFF2A2E33)

val AppAccent = Color(0xFF4EFFCA)
val AppAccentTint = Color(0x244EFFCA) // ~14% alpha mint
val AppOnAccent = Color(0xFF04120D)

val AppPrimaryText = Color(0xFFFFFFFF)
val AppSecondaryText = Color(0xFFA3A9B0)
val AppDisabledText = Color(0xFF6B7178)
val AppPlaceholder = Color(0xFF6B7178)

val AppBorder = Color(0xFF2A2E33)
val AppBorderSubtle = Color(0xFF3A3F45)
val AppDivider = Color(0xFF2A2E33)

val AppSuccess = Color(0xFF4EFFCA)
val AppSuccessTint = Color(0x244EFFCA)

val AppWarning = Color(0xFFFBBF24)
val AppWarningTint = Color(0x29FBBF24) // 16% alpha

val AppError = Color(0xFFF87171)
val AppErrorTint = Color(0x29F87171) // 16% alpha

/**
 * Material 3 Dark Color Scheme mapped to Findez design tokens.
 */
val DarkColorScheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = AppOnAccent,
    primaryContainer = AppAccentTint,
    onPrimaryContainer = AppAccent,
    secondary = AppSecondaryText,
    onSecondary = AppPrimaryText,
    secondaryContainer = AppDisabled,
    onSecondaryContainer = AppPrimaryText,
    tertiary = AppWarning,
    onTertiary = AppOnAccent,
    tertiaryContainer = AppWarningTint,
    onTertiaryContainer = AppWarning,
    background = AppBackground,
    onBackground = AppPrimaryText,
    surface = AppSurface,
    onSurface = AppPrimaryText,
    surfaceVariant = AppHeaderBackground,
    onSurfaceVariant = AppSecondaryText,
    outline = AppBorder,
    outlineVariant = AppDivider,
    error = AppError,
    onError = AppOnAccent,
    errorContainer = AppErrorTint,
    onErrorContainer = AppError
)

/**
 * Repointed to DarkColorScheme as light theme is deferred.
 */
val LightColorScheme = DarkColorScheme
