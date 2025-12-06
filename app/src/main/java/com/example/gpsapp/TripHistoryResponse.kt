package com.example.gpsapp

data class TripHistoryResponse(
    val vehicle_id: String,
    val trips: List<Trip>
)