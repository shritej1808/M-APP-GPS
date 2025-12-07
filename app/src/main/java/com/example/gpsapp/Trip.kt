package com.example.gpsapp

import com.google.gson.annotations.SerializedName

data class Trip(
    @SerializedName("_id") val trip_id: String?,
    @SerializedName("distance") val distance: Double,
    @SerializedName("toll") val toll: Double,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("entry_toll") val entryToll: String?,
    @SerializedName("exit_toll") val exitToll: String?,
    @SerializedName("is_paid") var is_paid: Boolean = false
)

