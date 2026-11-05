package com.company.attendance.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppOnAccent
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AttendanceTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pure swipe threshold check function (UI Revamp Plan Section 5.1 & 7).
 */
fun isSwipeThresholdReached(dragOffset: Float, maxDrag: Float, thresholdRatio: Float = 0.85f): Boolean {
    if (maxDrag <= 0f) return false
    return (dragOffset / maxDrag) >= thresholdRatio
}

/**
 * SwipeToPunch: Pinned slider replacing static buttons with a smooth swipe gesture.
 */
@Composable
fun SwipeToPunch(
    text: String,
    onSwipeTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    thresholdRatio: Float = 0.85f
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    val trackHeight = 64.dp
    val thumbSize = 52.dp
    val thumbPadding = 6.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .semantics {
                if (enabled && !isLoading) {
                    customActions = listOf(
                        CustomAccessibilityAction(label = text) {
                            onSwipeTriggered()
                            true
                        }
                    )
                }
            }
    ) {
        val density = LocalDensity.current
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val thumbPaddingPx = with(density) { thumbPadding.toPx() }
        val maxDragPx = (totalWidthPx - thumbSizePx - (thumbPaddingPx * 2)).coerceAtLeast(0f)

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = if (enabled) AppSurface else AppDisabled,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Secondary text label in center
                Text(
                    text = text,
                    style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) AppSecondaryText else AppDisabledText
                )

                // Draggable Thumb
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = thumbPadding)
                        .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(if (enabled) AppAccent else AppBorder)
                        .draggable(
                            state = rememberDraggableState { delta ->
                                if (enabled && !isLoading) {
                                    coroutineScope.launch {
                                        val newOffset = (dragOffset.value + delta).coerceIn(0f, maxDragPx)
                                        dragOffset.snapTo(newOffset)
                                    }
                                }
                            },
                            orientation = Orientation.Horizontal,
                            enabled = enabled && !isLoading,
                            onDragStopped = {
                                val reached = isSwipeThresholdReached(
                                    dragOffset = dragOffset.value,
                                    maxDrag = maxDragPx,
                                    thresholdRatio = thresholdRatio
                                )
                                if (reached) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeTriggered()
                                }
                                coroutineScope.launch {
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AppOnAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Swipe Action",
                            tint = if (enabled) AppOnAccent else AppDisabledText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "SwipeToPunch - Idle")
@Composable
private fun PreviewSwipeToPunchIdle() {
    AttendanceTheme {
        SwipeToPunch(
            text = "Swipe to Check In",
            onSwipeTriggered = {}
        )
    }
}

@Preview(name = "SwipeToPunch - Disabled")
@Composable
private fun PreviewSwipeToPunchDisabled() {
    AttendanceTheme {
        SwipeToPunch(
            text = "Attendance complete",
            onSwipeTriggered = {},
            enabled = false
        )
    }
}