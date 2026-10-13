package com.company.attendance.design.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Attendance App Design System Color Palette (PRD Section 8)
 * Light theme only. Clean, quiet, lots of white space, one accent color.
 */
val AppBackground = Color(0xFFFAFAFA)
val AppSurface = Color(0xFFFFFFFF)
val AppPrimaryText = Color(0xFF111827)
val AppSecondaryText = Color(0xFF6B7280)
val AppBorder = Color(0xFFE5E7EB)
val AppAccent = Color(0xFF2563EB)
val AppAccentTint = Color(0xFFEFF4FF)
val AppSuccess = Color(0xFF16A34A)
val AppSuccessTint = Color(0xFFDCFCE7)
val AppWarning = Color(0xFFD97706)
val AppWarningTint = Color(0xFFFEF3C7)
val AppError = Color(0xFFDC2626)
val AppErrorTint = Color(0xFFFEE2E2)

// Disabled and neutral states
val AppDisabled = Color(0xFFF3F4F6)
val AppDisabledText = Color(0xFF9CA3AF)
val AppPlaceholder = Color(0xFF9CA3AF)
val AppDivider = Color(0xFFE5E7EB)

/**
 * Material 3 Light Color Scheme mapped to the Attendance App design tokens.
 */
val LightColorScheme = lightColorScheme(
    primary = AppAccent,
    onPrimary = Color.White,
    primaryContainer = AppAccentTint,
    onPrimaryContainer = AppAccent,
    secondary = AppSecondaryText,
    onSecondary = Color.White,
    secondaryContainer = AppDisabled,
    onSecondaryContainer = AppPrimaryText,
    tertiary = AppWarning,
    onTertiary = Color.White,
    tertiaryContainer = AppWarningTint,
    onTertiaryContainer = AppWarning,
    background = AppBackground,
    onBackground = AppPrimaryText,
    surface = AppSurface,
    onSurface = AppPrimaryText,
    surfaceVariant = AppBackground,
    onSurfaceVariant = AppSecondaryText,
    outline = AppBorder,
    outlineVariant = AppDivider,
    error = AppError,
    onError = Color.White,
    errorContainer = AppErrorTint,
    onErrorContainer = AppError
)
