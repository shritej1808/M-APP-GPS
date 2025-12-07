package com.example.gpsapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentService {

    @POST("create_order")
    fun createOrder(
        @Body body: CreateOrderRequest
    ): Call<CreateOrderResponse>

    @POST("verify_payment")
    fun verifyPayment(
        @Body body: VerifyPaymentRequest
    ): Call<VerifyPaymentResponse>
}
