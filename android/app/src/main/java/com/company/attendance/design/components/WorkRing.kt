package com.company.attendance.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AttendanceTheme

/**
 * States for the Work Ring.
 */
enum class WorkRingState {
    NotStarted,
    InProgress,
    Complete,
    MissedPunch
}

/**
 * WorkRing: The centerpiece 200dp ring card displaying worked hours and live office time.
 */
@Composable
fun WorkRing(
    ringState: WorkRingState,
    workedTimeFormatted: String,
    progress: Float, // 0.0f to 1.0f
    officeTimeFormatted: String = "",
    shiftDisplayName: String = "",
    modifier: Modifier = Modifier,
    titleText: String? = null,
    isTimeUnverified: Boolean = false
) {
    val title = titleText ?: when (ringState) {
        WorkRingState.NotStarted -> "Check in your presence"
        WorkRingState.InProgress -> "You're on the clock"
        WorkRingState.Complete -> "Great work today"
        WorkRingState.MissedPunch -> "Session expired"
    }

    val sublabel = when (ringState) {
        WorkRingState.NotStarted -> "not checked in"
        WorkRingState.InProgress -> "worked today"
        WorkRingState.Complete -> "completed"
        WorkRingState.MissedPunch -> "missed punch"
    }

    val arcColor = when (ringState) {
        WorkRingState.MissedPunch -> AppWarning
        else -> AppAccent
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "WorkRingProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        color = AppSurface,
        border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header
            Text(
                text = title,
                style = AppTypographyTokens.SectionHeading.copy(fontWeight = FontWeight.SemiBold),
                color = AppPrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

            // 200dp Ring
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidthPx = 14.dp
                Canvas(modifier = Modifier.size(200.dp)) {
                    val stroke = Stroke(width = strokeWidthPx.toPx(), cap = StrokeCap.Round)
                    val diameter = size.minDimension - strokeWidthPx.toPx()
                    val topLeft = Offset(strokeWidthPx.toPx() / 2f, strokeWidthPx.toPx() / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Track
                    drawArc(
                        color = AppDisabled,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )

                    // Active Arc (from top = -90 deg)
                    if (animatedProgress > 0.005f) {
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }

                // Center Elapsed Time + Sublabel
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = workedTimeFormatted,
                        style = AppTypographyTokens.ClockTime,
                        color = if (ringState == WorkRingState.MissedPunch) AppWarning else AppPrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sublabel,
                        style = AppTypographyTokens.RingCaption,
                        color = AppSecondaryText
                    )
                }
            }

            if (isTimeUnverified) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
                TimeUnverifiedBadge()
            }
        }
    }
}

@Preview(name = "WorkRing - In Progress")
@Composable
private fun PreviewWorkRingInProgress() {
    AttendanceTheme {
        WorkRing(
            ringState = WorkRingState.InProgress,
            workedTimeFormatted = "04:32",
            progress = 0.5f,
            officeTimeFormatted = "13:32:15",
            shiftDisplayName = "General Shift (09:00 - 18:00)"
        )
    }
}

@Preview(name = "WorkRing - Complete")
@Composable
private fun PreviewWorkRingComplete() {
    AttendanceTheme {
        WorkRing(
            ringState = WorkRingState.Complete,
            workedTimeFormatted = "09:00",
            progress = 1.0f,
            officeTimeFormatted = "18:00:00",
            shiftDisplayName = "General Shift (09:00 - 18:00)"
        )
    }
}