package com.company.attendance.core.time

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface ClockWrapper {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

@Singleton
class SystemClockWrapper @Inject constructor() : ClockWrapper {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
