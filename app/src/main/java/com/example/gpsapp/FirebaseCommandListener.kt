package com.example.gpsapp

import android.content.Context
import android.util.Log
import com.google.firebase.database.*

object FirebaseCommandListener {

    private lateinit var db: DatabaseReference

    fun init(context: Context, vehicleId: String) {
        db = FirebaseDatabase.getInstance().reference
            .child("vehicles")
            .child(vehicleId)
            .child("commands")
            .child("start")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val start = snapshot.getValue(Boolean::class.java) ?: false

                if (start) {
                    Log.d("CMD", "Firebase triggered START")
                    TrackerController.startForegroundTracking(context)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CMD", "Firebase command error: ${error.message}")
            }
        })
    }
}
