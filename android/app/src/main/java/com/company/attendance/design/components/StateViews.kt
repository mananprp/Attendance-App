package com.company.attendance.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * Clean Loading State with circular progress spinner and optional message.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = "Loading..."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimensions.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = AppAccent,
            strokeWidth = 3.dp
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
            Text(
                text = message,
                style = AppTypographyTokens.Body,
                color = AppSecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Clean Empty State with minimalist icon, title, description, and optional action button.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimensions.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = AppSecondaryText
        )
        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
        Text(
            text = title,
            style = AppTypographyTokens.SectionHeading,
            color = AppPrimaryText,
            textAlign = TextAlign.Center
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
            Text(
                text = message,
                style = AppTypographyTokens.Body,
                color = AppSecondaryText,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
            PrimaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.padding(horizontal = AppDimensions.Spacing24)
            )
        }
    }
}

/**
 * Error state with title, message, and Retry PrimaryButton.
 */
@Composable
fun ErrorStateWithRetry(
    title: String = "Something went wrong",
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryButtonText: String = "Try Again",
    icon: ImageVector = Icons.Outlined.ErrorOutline
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimensions.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = AppError
        )
        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
        Text(
            text = title,
            style = AppTypographyTokens.SectionHeading,
            color = AppPrimaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
        Text(
            text = message,
            style = AppTypographyTokens.Body,
            color = AppSecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
        PrimaryButton(
            text = retryButtonText,
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = AppDimensions.Spacing24)
        )
    }
}
