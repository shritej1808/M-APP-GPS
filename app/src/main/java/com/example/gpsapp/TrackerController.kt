package com.example.gpsapp

import android.content.Context
import android.content.Intent
import android.os.Build

object TrackerController {

    fun startForegroundTracking(context: Context) {
        val intent = Intent(context, LocationService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}