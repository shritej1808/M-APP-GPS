package com.example.gpsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter : RecyclerView.Adapter<TripAdapter.TripViewHolder>(), Filterable {

    private val trips = mutableListOf<Trip>()
    private var filteredTrips: MutableList<Trip> = mutableListOf()
    private var originalTrips: List<Trip> = emptyList()

    init {
        filteredTrips = trips.toMutableList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(filteredTrips[position])
    }

    override fun getItemCount(): Int = filteredTrips.size

    fun addTrip(trip: Trip) {
        originalTrips = originalTrips + trip
        trips.add(0, trip)  // Add to top for newest first
        filteredTrips.add(0, trip)
        notifyItemInserted(0)
    }

    fun setTrips(newTrips: List<Trip>) {
        originalTrips = newTrips
        trips.clear()
        trips.addAll(newTrips)
        filteredTrips.clear()
        filteredTrips.addAll(newTrips)
        notifyDataSetChanged()
    }

    // New: Filter for search (call from TextWatcher)
    fun filterTrips(query: String) {
        if (query.isEmpty()) {
            filteredTrips = originalTrips.toMutableList()
        } else {
            filteredTrips = originalTrips.filter { trip ->
                (trip.startTime ?: "").contains(query, ignoreCase = true) ||
                        (trip.endTime ?: "").contains(query, ignoreCase = true) ||
                        trip.distance.toString().contains(query) ||
                        trip.toll.toString().contains(query)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // New: Get current filtered count for UI
    fun getFilteredCount(): Int = filteredTrips.size

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtDistance: TextView = itemView.findViewById(R.id.txtDistance)
        private val txtToll: TextView = itemView.findViewById(R.id.txtToll)
        private val txtStart: TextView = itemView.findViewById(R.id.txtStart)
        private val txtEnd: TextView = itemView.findViewById(R.id.txtEnd)

        fun bind(t: Trip) {
            txtDistance.text = "Distance: %.2f mi".format(t.distance)
            txtToll.text = "Toll: ₹%.2f".format(t.toll)
            txtStart.text = "Start: ${t.startTime ?: "--"}"
            txtEnd.text = "End: ${t.endTime ?: "--"}"
        }
    }

    // Filterable for built-in search support
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString() ?: ""
                filterTrips(query)
                return FilterResults().apply { values = filteredTrips }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}