package com.example.gpsapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var regVehicleId: EditText
    private lateinit var regOwnerName: EditText
    private lateinit var regPhone: EditText
    private lateinit var btnRegister: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        regVehicleId = findViewById(R.id.regVehicleId)
        regOwnerName = findViewById(R.id.regOwnerName)
        regPhone = findViewById(R.id.regPhone)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {

            val vid = regVehicleId.text.toString().uppercase().trim()
            val owner = regOwnerName.text.toString().trim()
            val phone = regPhone.text.toString().trim()

            if (vid.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Vehicle & Phone required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerVehicle(vid, owner, phone)
        }
    }

    private fun registerVehicle(vid: String, owner: String, phone: String) {

        val json = """
            {
                "vehicle_id":"$vid",
                "owner_name":"$owner",
                "phone_number":"$phone"
            }
        """.trimIndent()

        val req = Request.Builder()
            .url(BuildConfig.API_BASE + "/register_owner")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Network error!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                runOnUiThread {

                    if (!response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registration failed!", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    // Save locally
                    val prefs = getSharedPreferences("gps_prefs", MODE_PRIVATE)
                    prefs.edit().putString("vehicle_id", vid).apply()

                    // Upload FCM token NOW
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        VehicleRegistrationHelper.saveTokenToBackend(this@RegisterActivity, token)
                    }

                    Toast.makeText(this@RegisterActivity, "Registered Successfully!", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()
                }
            }
        })
    }
}
