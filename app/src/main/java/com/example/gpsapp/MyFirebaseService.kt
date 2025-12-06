package com.example.gpsapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    private val CHANNEL_ID = "gps_alerts_channel"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Send token to backend
        VehicleRegistrationHelper.saveTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: ""
        val title = message.notification?.title ?: "GPS Alert"
        val body = message.notification?.body ?: "You have a new alert."

        Log.d("FCM", "Received type = $type")

        // 🔥 Always show a notification (Android 12–14 need this)
        showNotification(title, body)

        // 🔵 Your old logic preserved:
        when (type) {

            "START_GPS", "TRIP_STARTED" -> {
                Log.d("FCM", "Starting GPS tracking (FCM trigger)")
                TrackerController.startForegroundTracking(applicationContext)
            }

            "OCR_DETECTED" -> Log.d("FCM", "OCR detected, waiting for toll entry...")

            "OFF_ROAD" -> Log.d("FCM", "Vehicle went off corridor!")

            "ON_ROAD" -> Log.d("FCM", "Vehicle back on toll route")

            "TRIP_ENDED" -> Log.d("FCM", "Trip ended, stopping tracking")

            else -> Log.d("FCM", "Unknown event")
        }
    }

    // ============================================
    //   CUSTOM NOTIFICATION CHANNEL + NOTIFICATION
    // ============================================
    private fun showNotification(title: String, body: String) {

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create custom channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableVibration(true)
            channel.description = "Notifications for OCR, GPS triggers, and toll events."

            manager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Change if needed
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Display notification
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
