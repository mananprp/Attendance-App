package com.company.attendance.feature.tracking.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.company.attendance.MainActivity
import com.company.attendance.R
import com.company.attendance.feature.tracking.util.TrackingActionActivity

class TrackingNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Ongoing Active Channel (Quiet, Persistent)
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_ID,
                "Site Tracking Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active field tracking duration and distance"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(activeChannel)

            // 2. Alert / Paused Channel (High Importance, Sound & Vibration for Heads-Up Banner)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Site Tracking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when location is turned off during tracking"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildActiveNotification(durationText: String, distanceText: String): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = TrackingActionActivity.createStopIntent(context)
        val stopPendingIntent = PendingIntent.getActivity(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ACTIVE_ID)
            .setContentTitle("Site Tracking Active")
            .setContentText("$durationText • $distanceText")
            .setSmallIcon(R.drawable.ic_findez_nav_tracker)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Open App", openAppPendingIntent)
            .addAction(0, "Stop Tracking", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildPausedAlertNotification(): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = TrackingActionActivity.createResumeIntent(context)
        val resumePendingIntent = PendingIntent.getActivity(
            context,
            2,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = TrackingActionActivity.createStopIntent(context)
        val stopPendingIntent = PendingIntent.getActivity(
            context,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setContentTitle("Tracking Paused")
            .setContentText("Location is off. Turn on location to resume.")
            .setSmallIcon(R.drawable.ic_findez_nav_tracker)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Resume Tracking", resumePendingIntent)
            .addAction(0, "Stop & Submit", stopPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    fun notifyPaused() {
        notificationManager.notify(NOTIFICATION_ID, buildPausedAlertNotification())
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ACTIVE_ID = "channel_site_tracking_active"
        const val CHANNEL_ALERT_ID = "channel_site_tracking_alert"
    }
}
