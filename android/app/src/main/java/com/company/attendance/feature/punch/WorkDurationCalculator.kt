package com.company.attendance.feature.punch

import com.company.attendance.design.components.WorkRingState
import kotlin.math.max
import kotlin.math.min

/**
 * Result data class for calculated work duration, progress and ring state.
 */
data class WorkDurationResult(
    val workedSeconds: Long,
    val formattedDuration: String,
    val progress: Float,
    val ringState: WorkRingState
)

/**
 * Pure calculator for work duration, ring progress and state transitions (Section 6.1).
 */
object WorkDurationCalculator {

    const val DEFAULT_TARGET_WORK_HOURS: Float = 9.0f

    fun calculate(
        punchInEpochSeconds: Long?,
        punchOutEpochSeconds: Long?,
        currentEpochSeconds: Long,
        isSessionExpired: Boolean = false,
        targetWorkHours: Float = DEFAULT_TARGET_WORK_HOURS
    ): WorkDurationResult {
        // Case 1: No punch in
        if (punchInEpochSeconds == null) {
            return WorkDurationResult(
                workedSeconds = 0L,
                formattedDuration = "0:00",
                progress = 0.0f,
                ringState = WorkRingState.NotStarted
            )
        }

        // Case 2: Session expired / missed punch cutoff passed without checkout
        if (punchOutEpochSeconds == null && isSessionExpired) {
            return WorkDurationResult(
                workedSeconds = 0L,
                formattedDuration = "--:--",
                progress = 0.0f,
                ringState = WorkRingState.MissedPunch
            )
        }

        // Case 3: In only (live elapsed)
        if (punchOutEpochSeconds == null) {
            val elapsedSeconds = max(0L, currentEpochSeconds - punchInEpochSeconds)
            val targetSeconds = max(1f, targetWorkHours * 3600f)
            val progress = min(1.0f, elapsedSeconds.toFloat() / targetSeconds)
            return WorkDurationResult(
                workedSeconds = elapsedSeconds,
                formattedDuration = formatHoursMinutes(elapsedSeconds),
                progress = progress,
                ringState = WorkRingState.InProgress
            )
        }

        // Case 4: Guard - Out earlier than in
        if (punchOutEpochSeconds < punchInEpochSeconds) {
            return WorkDurationResult(
                workedSeconds = 0L,
                formattedDuration = "0:00",
                progress = 0.0f,
                ringState = WorkRingState.Complete
            )
        }

        // Case 5: In + Out completed
        val workedSeconds = punchOutEpochSeconds - punchInEpochSeconds
        val targetSeconds = max(1f, targetWorkHours * 3600f)
        val progress = min(1.0f, workedSeconds.toFloat() / targetSeconds)
        return WorkDurationResult(
            workedSeconds = workedSeconds,
            formattedDuration = formatHoursMinutes(workedSeconds),
            progress = progress,
            ringState = WorkRingState.Complete
        )
    }

    fun formatHoursMinutes(seconds: Long): String {
        val totalMinutes = seconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "%d:%02d".format(hours, minutes)
    }
}