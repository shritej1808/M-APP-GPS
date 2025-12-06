package com.example.gpsapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Send token to backend
        VehicleRegistrationHelper.saveTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        Log.d("FCM", "Received type = $type")

        when (type) {

            "START_GPS", "TRIP_STARTED" -> {
                Log.d("FCM", "Starting GPS tracking (FCM trigger)")
                TrackerController.startForegroundTracking(applicationContext)
            }

            "OCR_DETECTED" -> {
                Log.d("FCM", "OCR detected, waiting for toll entry...")
            }

            "OFF_ROAD" -> {
                Log.d("FCM", "Vehicle went off corridor!")
            }

            "ON_ROAD" -> {
                Log.d("FCM", "Vehicle back on toll route")
            }

            "TRIP_ENDED" -> {
                Log.d("FCM", "Trip ended, stopping tracking")
            }

            else -> Log.d("FCM", "Unknown event")
        }
    }
}