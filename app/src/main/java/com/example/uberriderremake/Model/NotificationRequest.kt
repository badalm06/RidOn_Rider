package com.example.uberriderremake.Model

data class NotificationRequest(
    val pickup_location: PickupLocation,
    val data: Map<String, String>,
    val notification: NotificationContent
)


data class NotificationContent(
    val title: String,
    val body: String
)

data class NotificationResponse(
    val success: Boolean,
    val error: String?
)

data class PickupLocation(
    val lat: Double,
    val lng: Double
)
