package com.company.attendance.sync.util

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

object SyncOffsetCalculator {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Calculates the initial delay in milliseconds until the next occurrence of a configured
     * window (HH:mm) in the specified office timezone, plus a random jitter (0..jitterMinutes).
     *
     * @param windowStr Window time string, e.g. "16:00" or "00:00"
     * @param jitterMinutes Maximum jitter in minutes (e.g. 30)
     * @param timezoneId Office timezone ID string (e.g. "Asia/Kolkata")
     * @param nowInstant Optional current instant (defaults to Instant.now(), used for testing)
     * @param random Optional Random generator (used for deterministic testing)
     * @return Delay in milliseconds until scheduled trigger
     */
    fun calculateInitialDelayMs(
        windowStr: String,
        jitterMinutes: Int,
        timezoneId: String,
        nowInstant: Instant = Instant.now(),
        random: Random = Random()
    ): Long {
        val zoneId = try {
            ZoneId.of(timezoneId)
        } catch (_: Exception) {
            ZoneId.of("Asia/Kolkata")
        }

        val targetLocalTime = try {
            LocalTime.parse(windowStr.trim(), TIME_FORMATTER)
        } catch (_: Exception) {
            LocalTime.of(16, 0)
        }

        val nowZdt = nowInstant.atZone(zoneId)
        val todayTargetZdt = nowZdt
            .withHour(targetLocalTime.hour)
            .withMinute(targetLocalTime.minute)
            .withSecond(0)
            .withNano(0)

        val nextTargetZdt = if (todayTargetZdt.isAfter(nowZdt)) {
            todayTargetZdt
        } else {
            todayTargetZdt.plusDays(1)
        }

        val baseDelayMs = Duration.between(nowZdt, nextTargetZdt).toMillis().coerceAtLeast(0L)

        val jitterMs = if (jitterMinutes > 0) {
            val maxJitterMs = jitterMinutes * 60 * 1000L
            (random.nextDouble() * maxJitterMs).toLong().coerceIn(0L, maxJitterMs)
        } else {
            0L
        }

        return baseDelayMs + jitterMs
    }

    /**
     * Calculates next trigger epoch ms for a window given current time.
     */
    fun calculateNextTriggerEpochMs(
        windowStr: String,
        jitterMinutes: Int,
        timezoneId: String,
        nowInstant: Instant = Instant.now(),
        random: Random = Random()
    ): Long {
        return nowInstant.toEpochMilli() + calculateInitialDelayMs(
            windowStr = windowStr,
            jitterMinutes = jitterMinutes,
            timezoneId = timezoneId,
            nowInstant = nowInstant,
            random = random
        )
    }
}
