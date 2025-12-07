package com.example.gpsapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TripService {

    @GET("trip_history/{vehicle_id}")
    fun getTripHistory(
        @Path("vehicle_id") vehicleId: String
    ): Call<TripHistoryResponse>
    @POST("create_order")
    fun createOrder(
        @Body request: CreateOrderRequest
    ): Call<CreateOrderResponse>

    @POST("verify_payment")
    fun verifyPayment(
        @Body request: VerifyPaymentRequest
    ): Call<VerifyPaymentResponse>

}