package com.example.gpsapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitClient {

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE)
        .addConverterFactory(GsonConverterFactory.create())

        .build()

    val api: TripService = retrofit.create(TripService::class.java)
}
