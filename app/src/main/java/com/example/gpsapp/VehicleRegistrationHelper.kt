package com.example.gpsapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object VehicleRegistrationHelper {

    private val client = OkHttpClient()

    fun saveTokenToBackend(token: String) {
        val vehicleId = "UP70GT1215" // hard-coded for now (ok for demo)

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
                Log.e("FCM_REG", "Failed: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM_REG", "Saved token to backend")
            }
        })
    }
}