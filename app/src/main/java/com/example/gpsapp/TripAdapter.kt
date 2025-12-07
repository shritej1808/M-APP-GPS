package com.example.gpsapp

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(
    private val vehicleId: String
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>(), Filterable {

    private val trips = mutableListOf<Trip>()
    private var filteredTrips = mutableListOf<Trip>()
    private var originalTrips: List<Trip> = emptyList()

    // ------------------------------------------------------------------
    // SET TRIPS
    // ------------------------------------------------------------------
    fun setTrips(newTrips: List<Trip>) {
        originalTrips = newTrips
        trips.clear()
        trips.addAll(newTrips)

        filteredTrips.clear()
        filteredTrips.addAll(newTrips)

        notifyDataSetChanged()
    }

    // ------------------------------------------------------------------
    // FILTER FUNCTION (Missing earlier)
    // ------------------------------------------------------------------
    fun filterTrips(query: String) {
        filteredTrips =
            if (query.isEmpty()) {
                originalTrips.toMutableList()
            } else {
                originalTrips.filter { trip ->
                    (trip.startTime ?: "").contains(query, ignoreCase = true) ||
                            (trip.endTime ?: "").contains(query, ignoreCase = true) ||
                            trip.distance.toString().contains(query) ||
                            trip.toll.toString().contains(query) ||
                            (trip.entryToll ?: "").contains(query, ignoreCase = true) ||
                            (trip.exitToll ?: "").contains(query, ignoreCase = true)
                }.toMutableList()
            }

        notifyDataSetChanged()
    }

    fun getFilteredCount(): Int = filteredTrips.size

    // ------------------------------------------------------------------
    // VIEW HOLDER
    // ------------------------------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(filteredTrips[position])
    }

    override fun getItemCount(): Int = filteredTrips.size

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtDistance: TextView = itemView.findViewById(R.id.txtDistance)
        private val txtToll: TextView = itemView.findViewById(R.id.txtToll)
        private val txtStart: TextView = itemView.findViewById(R.id.txtStart)
        private val txtEnd: TextView = itemView.findViewById(R.id.txtEnd)
        private val txtEntryToll: TextView = itemView.findViewById(R.id.txtEntryToll)
        private val txtExitToll: TextView = itemView.findViewById(R.id.txtExitToll)

        private val btnPay: Button = itemView.findViewById(R.id.btnPay)

        fun bind(t: Trip) {

            txtDistance.text = "Distance: %.2f mi".format(t.distance)
            txtToll.text = "Toll: ₹%.2f".format(t.toll)
            txtStart.text = "Start: ${t.startTime ?: "--"}"
            txtEnd.text = "End: ${t.endTime ?: "--"}"
            txtEntryToll.text = "Entry Toll: ${t.entryToll ?: "--"}"
            txtExitToll.text = "Exit Toll: ${t.exitToll ?: "--"}"

            if (t.is_paid) {
                // PAID STYLE
                btnPay.apply {
                    text = "PAID ✔"
                    setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    isEnabled = false
                }
            } else {
                // UNPAID STYLE
                btnPay.apply {
                    text = "PAY NOW"
                    setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    isEnabled = true
                }

                btnPay.setOnClickListener {
                    val ctx = itemView.context
                    val intent = Intent(ctx, PaymentActivity::class.java)

                    intent.putExtra("vehicle_id", vehicleId)
                    intent.putExtra("amount", t.toll)
                    intent.putExtra("trip_id", t.trip_id ?: "")

                    if (ctx is Activity) {
                        ctx.startActivityForResult(intent, 2001)
                    } else {
                        ctx.startActivity(intent)
                    }
                }
            }
        }
    }


    // ------------------------------------------------------------------
    // ANDROID FILTER IMPLEMENTATION
    // ------------------------------------------------------------------
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val q = constraint?.toString()?.trim() ?: ""

                filterTrips(q)

                return FilterResults().apply {
                    values = filteredTrips
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}
