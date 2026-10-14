package com.company.attendance.core.util

import com.company.attendance.core.model.ShiftType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Utility for evaluating shift windows, cutoffs, and work date assignments
 * based on enterprise shift specifications:
 * - General Shift: 09:00 to 18:00 (cutoff at 03:00 AM next day)
 * - Evening Shift: 14:00 to 23:00 (cutoff at 06:00 AM next day)
 * - Night Shift:   21:00 to 06:00 (+1 day) (cutoff at 12:00 PM noon next day)
 */
object ShiftEvaluator {

    /**
     * Determines the assigned ShiftType and work date based on the local timestamp of Punch In.
     */
    fun determineShift(dateTime: LocalDateTime): Pair<ShiftType, LocalDate> {
        val hour = dateTime.hour
        return when {
            hour in 6..12 -> {
                // 06:00 AM - 12:59 PM: General Day Shift
                Pair(ShiftType.GENERAL, dateTime.toLocalDate())
            }
            hour in 13..18 -> {
                // 01:00 PM - 06:59 PM: Evening Shift
                Pair(ShiftType.EVENING, dateTime.toLocalDate())
            }
            hour >= 19 -> {
                // 07:00 PM - 11:59 PM: Night Shift starting today
                Pair(ShiftType.NIGHT, dateTime.toLocalDate())
            }
            else -> {
                // 00:00 AM - 05:59 AM: Post-midnight portion of previous night's shift
                Pair(ShiftType.NIGHT, dateTime.toLocalDate().minusDays(1))
            }
        }
    }

    /**
     * Calculates the exact rollover / cutoff deadline for a given shift and work date.
     * If the employee hasn't punched out by this deadline, the session is finalized as MISSED_PUNCH.
     */
    fun getCutoffDateTime(shiftType: ShiftType, workDate: LocalDate): LocalDateTime {
        return when (shiftType) {
            ShiftType.GENERAL -> {
                // 03:00 AM the following morning
                workDate.plusDays(1).atTime(LocalTime.of(3, 0, 0))
            }
            ShiftType.EVENING -> {
                // 06:00 AM the following morning
                workDate.plusDays(1).atTime(LocalTime.of(6, 0, 0))
            }
            ShiftType.NIGHT -> {
                // 12:00 PM (noon) the following day
                workDate.plusDays(1).atTime(LocalTime.of(12, 0, 0))
            }
        }
    }

    /**
     * Checks if an open session (punched in but not out) has expired past its shift cutoff.
     */
    fun isSessionExpired(
        shiftType: ShiftType,
        workDate: LocalDate,
        currentDateTime: LocalDateTime
    ): Boolean {
        val cutoff = getCutoffDateTime(shiftType, workDate)
        return currentDateTime.isAfter(cutoff)
    }

    /**
     * Returns user-friendly shift description.
     */
    fun getShiftDisplayName(shiftType: ShiftType): String {
        return when (shiftType) {
            ShiftType.GENERAL -> "General Shift (09:00 - 18:00)"
            ShiftType.EVENING -> "Evening Shift (14:00 - 23:00)"
            ShiftType.NIGHT -> "Night Shift (21:00 - 06:00)"
        }
    }
}
