package com.example.gpsapp

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFirebaseService : FirebaseMessagingService() {

    private val CHANNEL_ID = "gps_alerts_channel"

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val prefs = getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
        val vid = prefs.getString("vehicle_id", null)

        if (vid != null) {
            // FIX: Only pass context + token
            VehicleRegistrationHelper.saveTokenToBackend(this, token)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received: ${remoteMessage.data}")

        val type = remoteMessage.data["type"] ?: ""
        val vehicleId = remoteMessage.data["vehicle_id"] ?: ""

        val prefs = getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
        val loggedVid = prefs.getString("vehicle_id", "") ?: ""

        // Security: ensure message matches logged-in vehicle
        if (vehicleId.isNotBlank() && vehicleId != loggedVid) {
            Log.w("FCM", "Vehicle mismatch. Msg:$vehicleId Logged:$loggedVid. Ignored.")
            return
        }

        when (type) {

            "START_GPS" -> {
                Log.d("FCM", "FCM → Start GPS")

                val intent = Intent(this, LocationService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }

                showNotification("Start Tracking", "OCR detected. Begin sending GPS updates.")
            }

            "TRIP_ENDED", "STOP" -> {
                Log.d("FCM", "FCM → Stop GPS")
                stopService(Intent(this, LocationService::class.java))
                showNotification("Trip Ended", "Tracking stopped.")
            }

            "OCR_DETECTED" -> {
                showNotification("Authorized Plate", "Trip will start when entering toll gate.")
            }

            "TRIP_STARTED" -> {
                val direction = remoteMessage.data["direction"] ?: "unknown"
                showNotification("Trip Started", "Vehicle entered toll road ($direction).")
            }

            "OFF_ROAD" -> showNotification("Off Route!", "Vehicle left the toll road.")
            "ON_ROAD"  -> showNotification("Back On Route", "Vehicle returned to toll road.")

            else -> {
                Log.d("FCM", "Unhandled message type: $type")
                remoteMessage.notification?.let {
                    showNotification(it.title ?: "Alert", it.body ?: "")
                }
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(Random().nextInt(10000), notification)
    }

    private fun <T> isServiceRunning(serviceClass: Class<T>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }
}
