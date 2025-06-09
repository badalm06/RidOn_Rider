package com.example.uberriderremake.Services

import com.example.uberriderremake.Model.NotificationRequest
import com.example.uberriderremake.Model.NotificationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface BackendService {
    @POST("send-notification")
    fun sendNotification(@Body body: NotificationRequest): Call<NotificationResponse>
}
