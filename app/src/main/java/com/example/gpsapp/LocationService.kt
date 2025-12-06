package com.example.gpsapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val client = OkHttpClient()

    private val deviceId = "UP70GT1215"
    private val apiUrl = BuildConfig.API_BASE + "/update_location"

    private var lastPostTime = 0L
    private val POST_INTERVAL_MS = 2500L

    override fun onCreate() {
        super.onCreate()

        Log.d("SERVICE", "LocationService created")

        startForegroundServiceSafe()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------
    //  FOREGROUND SERVICE SETUP
    // ---------------------------------------------------------------------
    private fun startForegroundServiceSafe() {

        val channelId = "gps_channel"

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Sending live location to server…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(1, notification)
        }
    }

    // ---------------------------------------------------------------------
    //  START GPS UPDATES
    // ---------------------------------------------------------------------
    private fun startLocationUpdates() {

        fused = LocationServices.getFusedLocationProviderClient(this)

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .build()

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SERVICE", "NO LOCATION PERMISSION")
            stopSelf()
            return
        }

        fused.requestLocationUpdates(req, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    handleLocation(loc)
                }
            }
        }, mainLooper)
    }

    // ---------------------------------------------------------------------
    //  SEND LOCATION TO BACKEND
    // ---------------------------------------------------------------------
    private fun handleLocation(loc: Location) {

        val now = System.currentTimeMillis()
        if (now - lastPostTime < POST_INTERVAL_MS) return
        lastPostTime = now

        try {
            val acc = if (loc.hasAccuracy()) loc.accuracy else 0f
            val spd = if (loc.hasSpeed()) loc.speed else 0f

            val json = JSONObject().apply {
                put("vehicle_id", deviceId)
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
                put("accuracy", acc)
                put("speed", spd)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url(apiUrl)
                .post(body)
                .build()

            client.newCall(req).execute().use { response ->

                val raw = response.body?.string() ?: ""

                // 🚨 Detect ngrok / HTML failure early
                if (raw.startsWith("<!DOCTYPE") || raw.startsWith("<html")) {
                    Log.e("SERVICE", "❌ Backend returned HTML instead of JSON → ngrok dead or wrong URL")
                    return@use
                }

                // 🚨 Detect server-side error pages (500, 404, etc.)
                if (!response.isSuccessful) {
                    Log.e("SERVICE", "❌ Server error: ${response.code} → ${raw.take(100)}")
                    return@use
                }

                Log.d(
                    "SERVICE",
                    "✔ Sent location → ${loc.latitude}, ${loc.longitude}"
                )
            }

        } catch (e: Exception) {
            Log.e("SERVICE", "🔥 Exception while sending location: ${e.message}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(object : LocationCallback() {})  // Clean up
        Log.d("SERVICE", "LocationService destroyed")
    }
}