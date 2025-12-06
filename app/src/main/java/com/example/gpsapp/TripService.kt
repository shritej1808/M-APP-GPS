package com.example.gpsapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface TripService {

    @GET("trip_history/{vehicle_id}")
    fun getTripHistory(
        @Path("vehicle_id") vehicleId: String
    ): Call<TripHistoryResponse>
}