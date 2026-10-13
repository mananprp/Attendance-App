package com.company.attendance.face.util

import android.os.SystemClock
import android.util.Log
import com.company.attendance.BuildConfig

import java.util.Locale

/**
 * Lightweight debug timing logger for face verification pipeline stages.
 */
object StageTimer {
    const val TAG = "FaceTiming"

    inline fun <T> time(stage: String, block: () -> T): T {
        val startNs = SystemClock.elapsedRealtimeNanos()
        try {
            return block()
        } finally {
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[${Thread.currentThread().name}] $stage: ${String.format(Locale.US, "%.2f", elapsedMs)} ms")
            }
        }
    }

    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
