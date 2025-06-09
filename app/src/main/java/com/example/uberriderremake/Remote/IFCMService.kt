package com.example.uberriderremake.Remote

import com.example.uberriderremake.Model.FCMResponse
import com.example.uberriderremake.Model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key="
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse?>?
}