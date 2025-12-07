package com.example.gpsapp

import android.content.Intent
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

    private val PAYMENT_REQUEST = 2001   // <--- match adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        vehicleId = intent.getStringExtra("vehicle_id") ?: "UNKNOWN"

        recycler = findViewById(R.id.historyRecycler)
        adapter = TripAdapter(vehicleId)   // <-- FIXED: pass vehicleId here
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        tripCountText = findViewById(R.id.tripCountText)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        searchEditText = findViewById(R.id.searchEditText)

        findViewById<TextView>(R.id.vehicleHeaderText)?.text = "Vehicle: $vehicleId"

        findViewById<Button>(R.id.btnBack)?.setOnClickListener { finish() }

        // Search bar
        searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                adapter.filterTrips(query)
                tripCountText?.text = "${adapter.getFilteredCount()} trips"
            }
        })

        loadHistory()
    }

    //-----------------------------------------------------------------------
    // 🔥 REFRESH AFTER PAYMENT
    //-----------------------------------------------------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PAYMENT_REQUEST && resultCode == RESULT_OK) {
            loadHistory()
        }
    }

    //-----------------------------------------------------------------------
    // LOAD TRIP HISTORY
    //-----------------------------------------------------------------------
    private fun loadHistory() {
        RetroFitClient.api.getTripHistory(vehicleId)
            .enqueue(object : Callback<TripHistoryResponse> {

                override fun onResponse(call: Call<TripHistoryResponse>, response: Response<TripHistoryResponse>) {
                    val body = response.body()

                    if (body == null) {
                        showEmpty("Failed to load")
                        return
                    }

                    if (body.trips.isEmpty()) {
                        showEmpty("No trips")
                        return
                    }

                    adapter.setTrips(body.trips)
                    tripCountText?.text = "${body.trips.size} trips loaded"
                    recycler.visibility = View.VISIBLE
                    emptyStateLayout?.visibility = View.GONE
                }

                override fun onFailure(call: Call<TripHistoryResponse>, t: Throwable) {
                    Log.e("TripHistoryActivity", "Load failed", t)
                    showEmpty("Load error")
                    Toast.makeText(this@TripHistoryActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEmpty(msg: String) {
        tripCountText?.text = msg
        recycler.visibility = View.GONE
        emptyStateLayout?.visibility = View.VISIBLE
    }
}
