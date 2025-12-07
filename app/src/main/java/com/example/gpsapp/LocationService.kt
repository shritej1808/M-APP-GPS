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
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val client = OkHttpClient()

    // ❗Vehicle ID no longer hard-coded
    private var deviceId: String = ""

    private val apiUrl = BuildConfig.API_BASE + "/update_location"

    private var lastPostTime = 0L
    private val POST_INTERVAL_MS = 2500L

    override fun onCreate() {
        super.onCreate()

        loadVehicleId()   // Load real saved vehicle ID

        Log.d("SERVICE", "LocationService created. Using vehicle = $deviceId")

        startForegroundServiceSafe()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------
    //  LOAD VEHICLE ID FROM SHARED PREFS
    // ---------------------------------------------------------------------
    private fun loadVehicleId() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        deviceId = prefs.getString("vehicle_id", "") ?: ""

        if (deviceId.isBlank()) {
            Log.e("SERVICE", "❌ vehicle_id NOT SET in SharedPreferences!")
        }
    }

    // ---------------------------------------------------------------------
    //  FOREGROUND SERVICE
    // ---------------------------------------------------------------------
    private fun startForegroundServiceSafe() {

        val channelId = "gps_channel"

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
            .setContentText("Sending live location for $deviceId…")
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
    //  GPS UPDATES
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
    //  SEND TO BACKEND
    // ---------------------------------------------------------------------
    private fun handleLocation(loc: Location) {

        if (deviceId.isBlank()) {
            Log.e("SERVICE", "❌ Cannot send location → vehicleId empty")
            return
        }

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

                if (raw.startsWith("<html")) {
                    Log.e("SERVICE", "❌ Backend returned HTML → ngrok dead?")
                    return@use
                }

                if (!response.isSuccessful) {
                    Log.e("SERVICE", "❌ Server error: ${response.code}")
                    return@use
                }

                Log.d("SERVICE", "✔ Sent location for $deviceId @ ${loc.latitude}, ${loc.longitude}")
            }

        } catch (e: Exception) {
            Log.e("SERVICE", "🔥 Error sending location: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(object : LocationCallback() {})
        Log.d("SERVICE", "LocationService destroyed")
    }
}
