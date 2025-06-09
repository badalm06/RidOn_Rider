package com.example.uberriderremake.Model

data class NotificationRequest(
    val driverUserId: String,
//    val token: String,
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
