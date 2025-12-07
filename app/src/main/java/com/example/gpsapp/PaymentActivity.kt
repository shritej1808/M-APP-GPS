package com.example.gpsapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    private var vehicleId = ""
    private var amount = 0.0
    private var tripId: String? = ""
    private var razorpayOrderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        Checkout.preload(applicationContext)

        vehicleId = intent.getStringExtra("vehicle_id") ?: ""
        amount = intent.getDoubleExtra("amount", 0.0)
        tripId = intent.getStringExtra("trip_id")

        findViewById<TextView>(R.id.txtPayVehicle).text = "Vehicle: $vehicleId"
        findViewById<TextView>(R.id.txtPayAmount).text = "Pay: ₹%.2f".format(amount)

        findViewById<Button>(R.id.btnStartPayment).setOnClickListener {
            createOrder()
        }
    }

    private fun createOrder() {
        val req = CreateOrderRequest(vehicleId, amount, tripId)

        RetroFitClient.api.createOrder(req)
            .enqueue(object : Callback<CreateOrderResponse> {
                override fun onResponse(call: Call<CreateOrderResponse>, res: Response<CreateOrderResponse>) {
                    val body = res.body()
                    if (body != null) {
                        razorpayOrderId = body.order_id
                        openRazorpay(body)
                    } else {
                        Toast.makeText(this@PaymentActivity, "Order failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CreateOrderResponse>, t: Throwable) {
                    Toast.makeText(this@PaymentActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openRazorpay(order: CreateOrderResponse) {
        val checkout = Checkout()
        checkout.setKeyID(order.key_id)

        val options = JSONObject().apply {
            put("name", "GPS Toll Payment")
            put("description", "Trip Toll Charge")
            put("currency", order.currency)
            put("amount", order.amount)
            put("order_id", order.order_id)
            put("prefill", JSONObject().apply {
                put("email", "test@example.com")
                put("contact", "9999999999")
            })
        }

        checkout.open(this, options)
    }

    override fun onPaymentSuccess(paymentId: String?) {
        if (paymentId == null) {
            Toast.makeText(this, "Invalid Payment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        verifyPayment(paymentId)
    }

    override fun onPaymentError(code: Int, message: String?) {
        Toast.makeText(this, "Payment Failed ❌", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun verifyPayment(paymentId: String) {
        val req = VerifyPaymentRequest(
            order_id = razorpayOrderId,
            payment_id = paymentId,
            signature = "",
            vehicle_id = vehicleId,
            amount = amount,
            trip_id = tripId
        )

        RetroFitClient.api.verifyPayment(req)
            .enqueue(object : Callback<VerifyPaymentResponse> {
                override fun onResponse(call: Call<VerifyPaymentResponse>, res: Response<VerifyPaymentResponse>) {
                    Toast.makeText(this@PaymentActivity, "Payment Verified! ✅", Toast.LENGTH_LONG).show()
                    val result = intent
                    result.putExtra("trip_id", tripId)
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }

                override fun onFailure(call: Call<VerifyPaymentResponse>, t: Throwable) {
                    Toast.makeText(this@PaymentActivity, "Verification failed", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }
}
