package com.company.attendance.core.interfaces

import com.company.attendance.core.model.TrustedTime
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Contract for acquiring trusted time, synchronizing with SNTP servers,
 * anchoring with monotonic device elapsed time, and converting to the office timezone.
 *
 * Owned by Agent 1 (Android Core). Used by Agent 4 (Punch Flow) and Agent 2 (UI Clock).
 */
interface TrustedTimeProvider {
    /**
     * Flow of the current estimated trusted time in the office timezone.
     * Updates periodically for UI clocks and date tracking.
     */
    val currentTimeFlow: StateFlow<TrustedTime>

    /**
     * Obtains the current trusted time synchronously / suspend function.
     * If SNTP is reachable or recently anchored, returns NTP-derived time.
     * If completely offline and unanchored, falls back to device clock with isUnverified = true.
     */
    suspend fun getTrustedTime(): TrustedTime

    /**
     * Forces an immediate NTP synchronization against configured fallback NTP servers.
     * @return true if synchronization was successful and anchor updated.
     */
    suspend fun syncNtp(): Boolean

    /**
     * Gets the current date string (YYYY-MM-DD) in the office timezone.
     */
    suspend fun getOfficeLocalDate(): LocalDate

    /**
     * Returns whether the time provider is currently anchored to an authoritative NTP source.
     */
    fun isNtpSynchronized(): Boolean
}
