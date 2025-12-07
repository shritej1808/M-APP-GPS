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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var mapView: WebView
    private lateinit var eventText: TextView
    private lateinit var zoneText: TextView
    private lateinit var distanceText: TextView
    private lateinit var tollText: TextView
    private lateinit var vehicleText: TextView
    private lateinit var resetButton: Button
    private lateinit var btnHistory: Button

    // GPS
    private lateinit var fused: FusedLocationProviderClient
    private val client = OkHttpClient()

    // ---- FIXED: Now vehicle ID comes from SharedPreferences ----
    private val deviceId: String by lazy {
        getSharedPreferences("gpsapp", MODE_PRIVATE)
            .getString("vehicle_id", "UNKNOWN")!!
    }

    private val apiUrl = BuildConfig.API_BASE + "/update_location"
    private var trackingEnabled = false

    private var lastPostTime = 0L
    private val POST_INTERVAL_MS = 2500L
    private val ACCURACY_THRESHOLD = 40f

    private var tripStartTime: String? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startTrackingForeground()
                startLocalLocationUpdates()
            } else {
                eventText.text = "❌ Permission denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()

        // History button sends correct vehicle ID
        btnHistory.setOnClickListener {
            val i = Intent(this, TripHistoryActivity::class.java)
            i.putExtra("vehicle_id", deviceId)
            startActivity(i)
        }

        checkPermission()
    }

    // -------------------------------------------------------
    // INIT UI
    // -------------------------------------------------------
    private fun initUI() {
        mapView = findViewById(R.id.mapView)
        eventText = findViewById(R.id.eventText)
        zoneText = findViewById(R.id.zoneText)
        distanceText = findViewById(R.id.distanceText)
        tollText = findViewById(R.id.tollText)
        vehicleText = findViewById(R.id.vehicleText)
        resetButton = findViewById(R.id.resetButton)
        btnHistory = findViewById(R.id.btnHistory)

        vehicleText.text = "Vehicle: $deviceId"

        resetButton.setOnClickListener { resetDistance() }
    }

    // -------------------------------------------------------
    // PERMISSIONS
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // START BACKGROUND TRACKING
    // -------------------------------------------------------
    private fun startTrackingForeground() {
        trackingEnabled = true
        TrackerController.startForegroundTracking(this)
    }

    // -------------------------------------------------------
    // LOCATION UPDATE
    // -------------------------------------------------------
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
                for (loc in result.locations) handleLocation(loc)
            }
        }, mainLooper)
    }

    private fun handleLocation(loc: Location) {

        if (!loc.hasAccuracy() || loc.accuracy > ACCURACY_THRESHOLD) {
            eventText.text = "⚠️ Poor GPS (${loc.accuracy.toInt()}m)"
            return
        }

        updateMap(loc)

        if (trackingEnabled)
            sendToBackendThrottled(loc)
    }

    // -------------------------------------------------------
    // MAP DISPLAY
    // -------------------------------------------------------
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
            loadsImagesAutomatically = true
        }

        mapView.visibility = View.INVISIBLE

        mapView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                    duration = 1000
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
        }

        mapView.loadUrl(url)
    }

    // -------------------------------------------------------
    // SEND TO BACKEND
    // -------------------------------------------------------
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

                val body =
                    json.toString().toRequestBody("application/json".toMediaType())

                val req = Request.Builder().url(apiUrl).post(body).build()

                val resp = client.newCall(req).execute()

                val reply = resp.body?.string() ?: return@launch
                Log.d("API", "Response: $reply")

                val result = JSONObject(reply)

                val event = result.optString("event", "—")
                val nearest = result.optJSONObject("nearest_zone")
                val nearestName = nearest?.optString("name") ?: "—"
                val nearestDist = nearest?.optDouble("distance_m", 0.0) ?: 0.0
                val distMi = result.optDouble("total_distance_mi", 0.0)
                val toll = result.optDouble("toll_estimate", 0.0)

                withContext(Dispatchers.Main) {
                    eventText.text = event
                    zoneText.text = "Nearest: $nearestName (${nearestDist.toInt()}m)"
                    distanceText.text = "Distance: %.2f mi".format(distMi)
                    tollText.text = "Toll: ₹%.2f".format(toll)
                }

            } catch (e: Exception) {
                Log.e("API", "Error: ${e.localizedMessage}")
            }
        }
    }

    // -------------------------------------------------------
    // RESET
    // -------------------------------------------------------
    private fun resetDistance() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply { put("vehicle_id", deviceId) }
                val body =
                    json.toString().toRequestBody("application/json".toMediaType())

                val req = Request.Builder()
                    .url("${BuildConfig.API_BASE}/reset_distance")
                    .post(body)
                    .build()

                client.newCall(req).execute()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Reset done!", Toast.LENGTH_SHORT).show()
                    distanceText.text = "Distance: 0.00 mi"
                    tollText.text = "Toll: ₹0.00"
                }

            } catch (e: Exception) {
                Log.e("RESET", "Error: ${e.localizedMessage}")
            }
        }
    }
}
