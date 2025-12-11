package com.example.gpsapp

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: WebView
    private lateinit var eventText: TextView
    private lateinit var zoneText: TextView
    private lateinit var distanceText: TextView
    private lateinit var tollText: TextView
    private lateinit var vehicleText: TextView
    private lateinit var btnHistory: Button

    private lateinit var fused: FusedLocationProviderClient
    private val client = OkHttpClient()

    private val prefs by lazy { getSharedPreferences("gps_prefs", MODE_PRIVATE) }

    private val deviceId: String by lazy {
        prefs.getString("vehicle_id", "") ?: ""
    }

    private val apiUrl = BuildConfig.API_BASE + "/update_location"
    private var trackingEnabled = false

    private var lastPostTime = 0L
    private val POST_INTERVAL_MS = 2500L
    private val ACCURACY_THRESHOLD = 40f

    @RequiresApi(Build.VERSION_CODES.O)
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startTrackingForeground()
                startLocalLocationUpdates()
            } else eventText.text = "❌ Permission denied"
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // FORCE LOGIN IF LOGGED OUT
        if (deviceId.isBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initUI()

        btnHistory.setOnClickListener {
            val i = Intent(this, TripHistoryActivity::class.java)
            i.putExtra("vehicle_id", deviceId)
            startActivity(i)
        }

        checkPermission()
    }

    private fun initUI() {
        mapView = findViewById(R.id.mapView)
        eventText = findViewById(R.id.eventText)
        zoneText = findViewById(R.id.zoneText)
        distanceText = findViewById(R.id.distanceText)
        tollText = findViewById(R.id.tollText)
        vehicleText = findViewById(R.id.vehicleText)
        btnHistory = findViewById(R.id.btnHistory)

        val logoutButton = findViewById<Button>(R.id.btnLogout)

        vehicleText.text = "Vehicle: $deviceId"

        logoutButton.setOnClickListener {
            prefs.edit().clear().apply()

            TrackerController.stopForegroundTracking(this)

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startTrackingForeground()
            startLocalLocationUpdates()
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startTrackingForeground() {
        trackingEnabled = true
        TrackerController.startForegroundTracking(this)
    }

    private fun startLocalLocationUpdates() {
        fused = LocationServices.getFusedLocationProviderClient(this)

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fused.requestLocationUpdates(req, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { handleLocation(it) }
            }
        }, mainLooper)
    }

    private fun handleLocation(loc: Location) {
        if (!loc.hasAccuracy() || loc.accuracy > ACCURACY_THRESHOLD) {
            eventText.text = "⚠ Poor GPS (${loc.accuracy.toInt()}m)"
            return
        }

        updateMap(loc)
        if (trackingEnabled) sendToBackendThrottled(loc)
    }

    private fun updateMap(loc: Location) {
        val isNight = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 6..18
        val style = if (isNight) "dark-matter-yellow-roads" else "osm-bright-smooth"

        val url =
            "https://maps.geoapify.com/v1/staticmap" +
                    "?style=$style" +
                    "&width=800&height=600" +
                    "&center=lonlat:${loc.longitude},${loc.latitude}" +
                    "&zoom=16" +
                    "&marker=lonlat:${loc.longitude},${loc.latitude};color:red;size:large" +
                    "&apiKey=${BuildConfig.GEOAPIFY_KEY}"

        mapView.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        mapView.visibility = View.INVISIBLE

        mapView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                    duration = 800
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
        }

        mapView.loadUrl(url)
    }

    private fun sendToBackendThrottled(loc: Location) {
        val now = System.currentTimeMillis()
        if (now - lastPostTime < POST_INTERVAL_MS) return
        lastPostTime = now

        sendToBackend(loc)
    }

    private fun sendToBackend(loc: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("vehicle_id", deviceId)
                    put("latitude", loc.latitude)
                    put("longitude", loc.longitude)
                    put("accuracy", loc.accuracy)
                    put("speed", loc.speed)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(apiUrl).post(body).build()
                val resp = client.newCall(req).execute()
                val reply = resp.body?.string() ?: return@launch

                val result = JSONObject(reply)
                val event = result.optString("event", "—")
                val nearest = result.optJSONObject("nearest_zone")
                val distMi = result.optDouble("total_distance_mi", 0.0)
                val toll = result.optDouble("toll_estimate", 0.0)

                withContext(Dispatchers.Main) {
                    eventText.text = event
                    zoneText.text = "Nearest: ${nearest?.optString("name")} (${nearest?.optDouble("distance_m")?.toInt()}m)"
                    distanceText.text = "Distance: %.2f mi".format(distMi)
                    tollText.text = "Toll: ₹%.2f".format(toll)
                }

            } catch (e: Exception) {
                Log.e("API", "Error: ${e.localizedMessage}")
            }
        }
    }
}
