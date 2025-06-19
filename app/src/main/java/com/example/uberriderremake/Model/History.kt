package com.example.uberriderremake.Model


data class History(
    val start_address: String = "",
    val end_address: String = "",
    val duration: String = "",
    val distanceText: String = "",
    val price: Double = 0.0,
    val rider: String = "", // for filtering
    val tripStartTime: String = "" // <-- Add this field

)
