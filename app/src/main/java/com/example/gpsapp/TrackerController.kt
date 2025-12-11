package com.example.gpsapp

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

object TrackerController {

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForegroundTracking(context: Context) {
        val i = Intent(context, LocationService::class.java)
        context.startForegroundService(i)
    }

    fun stopForegroundTracking(context: Context) {
        val i = Intent(context, LocationService::class.java)
        context.stopService(i)
    }
}
