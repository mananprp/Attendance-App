package com.company.attendance.feature.punch

import com.company.attendance.design.components.WorkRingState
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkDurationCalculatorTest {

    @Test
    fun `no punch-in returns NotStarted with 0 duration and 0 progress`() {
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = null,
            punchOutEpochSeconds = null,
            currentEpochSeconds = 1700000000L
        )
        assertEquals(WorkRingState.NotStarted, result.ringState)
        assertEquals("0:00", result.formattedDuration)
        assertEquals(0.0f, result.progress, 0.001f)
    }

    @Test
    fun `punched in without checkout computes live elapsed duration and progress`() {
        val punchIn = 1700000000L
        val current = punchIn + (4 * 3600L + 30 * 60L) // 4 hours 30 mins later
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = punchIn,
            punchOutEpochSeconds = null,
            currentEpochSeconds = current,
            targetWorkHours = 9.0f
        )
        assertEquals(WorkRingState.InProgress, result.ringState)
        assertEquals("4:30", result.formattedDuration)
        assertEquals(0.5f, result.progress, 0.001f) // 4.5 / 9 = 0.5
    }

    @Test
    fun `punched in and punched out computes exact fixed duration`() {
        val punchIn = 1700000000L
        val punchOut = punchIn + (8 * 3600L + 15 * 60L) // 8 hours 15 mins later
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = punchIn,
            punchOutEpochSeconds = punchOut,
            currentEpochSeconds = punchIn + 100000L,
            targetWorkHours = 9.0f
        )
        assertEquals(WorkRingState.Complete, result.ringState)
        assertEquals("8:15", result.formattedDuration)
        assertEquals(8.25f / 9.0f, result.progress, 0.001f)
    }

    @Test
    fun `overtime progress clamps at 100 percent`() {
        val punchIn = 1700000000L
        val punchOut = punchIn + (10 * 3600L) // 10 hours on a 9-hour target
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = punchIn,
            punchOutEpochSeconds = punchOut,
            currentEpochSeconds = punchIn + 100000L,
            targetWorkHours = 9.0f
        )
        assertEquals(WorkRingState.Complete, result.ringState)
        assertEquals("10:00", result.formattedDuration)
        assertEquals(1.0f, result.progress, 0.001f)
    }

    @Test
    fun `expired session marks MissedPunch with dashes`() {
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = 1700000000L,
            punchOutEpochSeconds = null,
            currentEpochSeconds = 1700100000L,
            isSessionExpired = true
        )
        assertEquals(WorkRingState.MissedPunch, result.ringState)
        assertEquals("--:--", result.formattedDuration)
        assertEquals(0.0f, result.progress, 0.001f)
    }

    @Test
    fun `guard against punchOut earlier than punchIn`() {
        val result = WorkDurationCalculator.calculate(
            punchInEpochSeconds = 1700001000L,
            punchOutEpochSeconds = 1700000000L,
            currentEpochSeconds = 1700002000L
        )
        assertEquals(WorkRingState.Complete, result.ringState)
        assertEquals("0:00", result.formattedDuration)
        assertEquals(0.0f, result.progress, 0.001f)
    }
}