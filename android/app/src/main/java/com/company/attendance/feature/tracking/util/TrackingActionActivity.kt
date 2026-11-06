package com.company.attendance.feature.tracking.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.company.attendance.feature.tracking.service.LocationTrackerService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class TrackingActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            ACTION_RESUME_GPS -> {
                promptEnableGps()
            }
            ACTION_STOP_TRACKING -> {
                LocationTrackerService.stopTracking(this)
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    private fun promptEnableGps() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000L).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            LocationTrackerService.resumeTracking(this)
            finish()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (_: Exception) {
                    finish()
                }
            } else {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                LocationTrackerService.resumeTracking(this)
            }
            finish()
        }
    }

    companion object {
        const val ACTION_RESUME_GPS = "com.company.attendance.action.RESUME_GPS"
        const val ACTION_STOP_TRACKING = "com.company.attendance.action.STOP_TRACKING"
        private const val REQUEST_CHECK_SETTINGS = 1001

        fun createResumeIntent(context: Context): Intent {
            return Intent(context, TrackingActionActivity::class.java).apply {
                action = ACTION_RESUME_GPS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, TrackingActionActivity::class.java).apply {
                action = ACTION_STOP_TRACKING
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }
}
