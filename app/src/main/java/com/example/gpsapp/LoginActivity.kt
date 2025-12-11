package com.example.gpsapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var loginVehicle: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoRegister: TextView

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("gps_prefs", MODE_PRIVATE)

        // AUTO-LOGIN if vehicle saved
        prefs.getString("vehicle_id", null)?.let {
            if (it.isNotBlank()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
        }

        loginVehicle = findViewById(R.id.loginVehicleId)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoRegister = findViewById(R.id.btnGoRegister)

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val vid = loginVehicle.text.toString().uppercase().trim()
            if (vid.isBlank()) {
                Toast.makeText(this, "Enter vehicle number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkVehicleOnBackend(vid)
        }
    }

    private fun checkVehicleOnBackend(vehicleId: String) {

        val json = """{"vehicle_id":"$vehicleId"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(BuildConfig.API_BASE + "/check_vehicle")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val raw = response.body?.string() ?: ""

                if (!raw.trim().startsWith("{")) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Server error (invalid JSON)", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val obj = JSONObject(raw)
                val exists = obj.optBoolean("registered", false)

                runOnUiThread {

                    if (!exists) {
                        Toast.makeText(this@LoginActivity, "Not registered!", Toast.LENGTH_SHORT).show()

                        val i = Intent(this@LoginActivity, RegisterActivity::class.java)
                        i.putExtra("vehicle_id", vehicleId)
                        startActivity(i)
                        return@runOnUiThread
                    }

                    // Save on device
                    val prefs = getSharedPreferences("gps_prefs", MODE_PRIVATE)
                    prefs.edit().putString("vehicle_id", vehicleId).apply()

                    // Upload the FCM token NOW
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        VehicleRegistrationHelper.saveTokenToBackend(this@LoginActivity, token)
                    }

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        })
    }
}
