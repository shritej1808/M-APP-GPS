package com.example.gpsapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TripHistoryActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var vehicleId: String
    private var tripCountText: TextView? = null
    private var emptyStateLayout: LinearLayout? = null
    private var searchEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        vehicleId = intent.getStringExtra("vehicle_id") ?: "UNKNOWN"
        Log.d("TripHistoryActivity", "Loading history for vehicle: $vehicleId")

        // Find and setup RecyclerView (core - always needed)
        recycler = findViewById(R.id.historyRecycler)
        if (recycler == null) {
            Log.e("TripHistoryActivity", "RecyclerView (historyRecycler) not found! Check layout XML.")
            Toast.makeText(this, "Layout error: RecyclerView missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup adapter and layout manager
        adapter = TripAdapter()
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        // Optional UI elements (with null safety)
        try {
            val vehicleHeaderText = findViewById<TextView>(R.id.vehicleHeaderText)
            vehicleHeaderText?.text = "Vehicle: $vehicleId"

            tripCountText = findViewById(R.id.tripCountText)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)
            searchEditText = findViewById(R.id.searchEditText)

            // Back button
            val btnBack = findViewById<Button>(R.id.btnBack)
            btnBack?.setOnClickListener { finish() }

            // Search functionality (if EditText exists)
            searchEditText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    adapter.filterTrips(query)  // Uses enhanced filterTrips
                    tripCountText?.text = "${adapter.getFilteredCount()} trips"  // Update count
                }
            })
        } catch (e: Exception) {
            Log.w("TripHistoryActivity", "Optional UI setup failed: ${e.message}")
            // Continue without optional elements
        }

        // Load data
        loadHistory()
    }

    private fun loadHistory() {
        RetroFitClient.api.getTripHistory(vehicleId)
            .enqueue(object : Callback<TripHistoryResponse> {
                override fun onResponse(
                    call: Call<TripHistoryResponse>,
                    response: Response<TripHistoryResponse>
                ) {
                    val body = response.body()
                    if (body != null) {
                        if (body.trips.isNotEmpty()) {
                            adapter.setTrips(body.trips)
                            tripCountText?.text = "${body.trips.size} trips loaded"
                            recycler.visibility = View.VISIBLE
                            emptyStateLayout?.visibility = View.GONE
                            Toast.makeText(this@TripHistoryActivity, "Loaded ${body.trips.size} trips", Toast.LENGTH_SHORT).show()
                        } else {
                            tripCountText?.text = "No trips available"
                            recycler.visibility = View.GONE
                            emptyStateLayout?.visibility = View.VISIBLE
                            Toast.makeText(this@TripHistoryActivity, "No trip history found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        tripCountText?.text = "Failed to load"
                        recycler.visibility = View.GONE
                        emptyStateLayout?.visibility = View.VISIBLE
                        Toast.makeText(this@TripHistoryActivity, "No trip history found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TripHistoryResponse>, t: Throwable) {
                    Log.e("TripHistoryActivity", "Load failed", t)
                    tripCountText?.text = "Load error"
                    recycler.visibility = View.GONE
                    emptyStateLayout?.visibility = View.VISIBLE
                    Toast.makeText(this@TripHistoryActivity, "Failed to load history: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}