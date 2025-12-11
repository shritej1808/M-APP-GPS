package com.example.gpsapp

import android.Manifest
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val client = OkHttpClient()

    private var vehicleId: String = ""
    private var lastPostTime = 0L

    private val POST_INTERVAL_MS = 2500L

    // Build API URL safely (avoids "//update_location")
    private val apiUrl: String
        get() = BuildConfig.API_BASE.trimEnd('/') + "/update_location"

    // Location callback
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { handleLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        loadVehicleId()

        if (vehicleId.isBlank()) {
            Log.e("SERVICE", "❌ No vehicle_id in SharedPrefs → stopping service")
            stopSelf()
            return
        }

        Log.d("SERVICE", "LocationService started for $vehicleId")
        Log.d("SERVICE", "API endpoint = $apiUrl")

        startForegroundServiceSafe()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadVehicleId()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadVehicleId() {
        val prefs = getSharedPreferences("gps_prefs", MODE_PRIVATE)
        vehicleId = prefs.getString("vehicle_id", "") ?: ""
        Log.d("SERVICE", "Loaded vehicleId = $vehicleId")
    }

    // ---------------- FOREGROUND SERVICE ----------------
    private fun startForegroundServiceSafe() {
        val channelId = "gps_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Vehicle: $vehicleId")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notif)
        }
    }

    // ---------------- LOCATION UPDATES ----------------
    private fun startLocationUpdates() {
        fused = LocationServices.getFusedLocationProviderClient(this)

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SERVICE", "❌ Missing location permission → stopping")
            stopSelf()
            return
        }

        fused.requestLocationUpdates(req, locationCallback, mainLooper)
    }

    // ---------------- SEND LOCATION ----------------
    private fun handleLocation(loc: Location) {

        if (vehicleId.isBlank()) {
            Log.e("SERVICE", "❌ No vehicle ID → ignoring GPS")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastPostTime < POST_INTERVAL_MS) return
        lastPostTime = now

        Log.d("SERVICE", "Sending GPS → lat=${loc.latitude}, lon=${loc.longitude}")

        val json = JSONObject().apply {
            put("vehicle_id", vehicleId)
            put("latitude", loc.latitude)
            put("longitude", loc.longitude)
            put("accuracy", loc.accuracy)
            put("speed", loc.speed)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(apiUrl).post(body).build()

        // Use async enqueue so app never freezes
        client.newCall(req).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("SERVICE", "❌ GPS POST failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("SERVICE", "❌ Server error ${it.code}")
                    } else {
                        Log.d("SERVICE", "✔ GPS sent successfully")
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(locationCallback)
        Log.d("SERVICE", "LocationService stopped")
    }
}
