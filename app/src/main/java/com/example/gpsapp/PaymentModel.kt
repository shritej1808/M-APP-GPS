package com.example.gpsapp

data class CreateOrderRequest(
    val vehicle_id: String,
    val amount: Double,
    val trip_id: String?
)

data class CreateOrderResponse(
    val order_id: String,
    val amount: Int,
    val currency: String,
    val key_id: String
)

data class VerifyPaymentRequest(
    val order_id: String,
    val payment_id: String,
    val signature: String,
    val vehicle_id: String,
    val amount: Double,
    val trip_id: String?
)

data class VerifyPaymentResponse(
    val status: String,
    val message: String,
    val order_id: String,
    val payment_id: String
)
