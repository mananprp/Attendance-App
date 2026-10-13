package com.company.attendance.sync

import com.company.attendance.sync.util.SyncOffsetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Random

class SyncOffsetCalculatorTest {

    private val kolkataZone = ZoneId.of("Asia/Kolkata")

    @Test
    fun calculateInitialDelayMs_sameDayFutureWindow_calculatesAccurateOffset() {
        // Current time: 2026-09-30 10:00:00 IST
        val ldt = LocalDateTime.of(2026, 9, 30, 10, 0, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()

        // Target window: 16:00
        val delayMs = SyncOffsetCalculator.calculateInitialDelayMs(
            windowStr = "16:00",
            jitterMinutes = 0,
            timezoneId = "Asia/Kolkata",
            nowInstant = nowInstant
        )

        // Expected delay: 6 hours = 6 * 3600 * 1000 = 21,600,000 ms
        val expectedMs = 6 * 3600 * 1000L
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun calculateInitialDelayMs_pastWindowToday_schedulesForNextDay() {
        // Current time: 2026-09-30 17:00:00 IST (after 16:00 window)
        val ldt = LocalDateTime.of(2026, 9, 30, 17, 0, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()

        // Target window: 16:00 (next day)
        val delayMs = SyncOffsetCalculator.calculateInitialDelayMs(
            windowStr = "16:00",
            jitterMinutes = 0,
            timezoneId = "Asia/Kolkata",
            nowInstant = nowInstant
        )

        // Expected delay: (24 - 17 + 16) hours = 23 hours = 23 * 3600 * 1000 = 82,800,000 ms
        val expectedMs = 23 * 3600 * 1000L
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun calculateInitialDelayMs_midnightWindow_schedulesCorrectly() {
        // Current time: 2026-09-30 20:30:00 IST
        val ldt = LocalDateTime.of(2026, 9, 30, 20, 30, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()

        // Target window: 00:00 (midnight next day)
        val delayMs = SyncOffsetCalculator.calculateInitialDelayMs(
            windowStr = "00:00",
            jitterMinutes = 0,
            timezoneId = "Asia/Kolkata",
            nowInstant = nowInstant
        )

        // Expected delay: 3.5 hours = 3.5 * 3600 * 1000 = 12,600,000 ms
        val expectedMs = (3 * 3600 + 30 * 60) * 1000L
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun calculateInitialDelayMs_withJitter_addsBoundedRandomDelay() {
        val ldt = LocalDateTime.of(2026, 9, 30, 10, 0, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()

        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5 // 50% of max jitter
        }

        val delayMs = SyncOffsetCalculator.calculateInitialDelayMs(
            windowStr = "16:00",
            jitterMinutes = 30,
            timezoneId = "Asia/Kolkata",
            nowInstant = nowInstant,
            random = deterministicRandom
        )

        val baseDelayMs = 6 * 3600 * 1000L
        val expectedJitterMs = 15 * 60 * 1000L // 50% of 30m
        assertEquals(baseDelayMs + expectedJitterMs, delayMs)
    }

    @Test
    fun calculateInitialDelayMs_randomJitterDistribution_isWithinBounds() {
        val ldt = LocalDateTime.of(2026, 9, 30, 12, 0, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()
        val jitterMinutes = 30
        val maxJitterMs = jitterMinutes * 60 * 1000L
        val baseDelayMs = 4 * 3600 * 1000L // 12:00 to 16:00

        for (seed in 1..20) {
            val random = Random(seed.toLong())
            val delayMs = SyncOffsetCalculator.calculateInitialDelayMs(
                windowStr = "16:00",
                jitterMinutes = jitterMinutes,
                timezoneId = "Asia/Kolkata",
                nowInstant = nowInstant,
                random = random
            )
            val jitterPart = delayMs - baseDelayMs
            assertTrue("Jitter should be >= 0", jitterPart >= 0)
            assertTrue("Jitter should be <= maxJitterMs", jitterPart <= maxJitterMs)
        }
    }

    @Test
    fun calculateNextTriggerEpochMs_producesAccurateEpochTimestamp() {
        val ldt = LocalDateTime.of(2026, 9, 30, 10, 0, 0)
        val nowInstant = ldt.atZone(kolkataZone).toInstant()

        val nextEpoch = SyncOffsetCalculator.calculateNextTriggerEpochMs(
            windowStr = "16:00",
            jitterMinutes = 0,
            timezoneId = "Asia/Kolkata",
            nowInstant = nowInstant
        )

        val expectedEpoch = LocalDateTime.of(2026, 9, 30, 16, 0, 0)
            .atZone(kolkataZone).toInstant().toEpochMilli()
        assertEquals(expectedEpoch, nextEpoch)
    }
}
