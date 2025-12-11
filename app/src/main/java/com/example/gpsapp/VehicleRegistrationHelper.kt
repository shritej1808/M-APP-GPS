package com.example.gpsapp

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object VehicleRegistrationHelper {

    private val client = OkHttpClient()

    fun saveTokenToBackend(context: Context, token: String) {
        val prefs = context.getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
        val vehicleId = prefs.getString("vehicle_id", null)

        if (vehicleId.isNullOrEmpty()) {
            Log.e("FCM_REG", "❌ Cannot send token: No vehicle_id saved")
            return
        }

        val json = """
            {
              "vehicle_id": "$vehicleId",
              "fcm_token": "$token"
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(BuildConfig.API_BASE + "/register_device")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM_REG", "Failed to send token: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("FCM_REG", "Failed: ${response.code}")
                    return
                }
                Log.d("FCM_REG", "✔ Token saved for $vehicleId")
            }
        })
    }
}
