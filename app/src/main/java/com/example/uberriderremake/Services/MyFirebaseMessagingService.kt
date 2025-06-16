package com.example.uberriderremake.Services

import android.content.Intent
import com.example.uberriderremake.Common
import com.example.uberriderremake.login.SplashScreen
import com.example.uberriderremake.login.User_rider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(FirebaseAuth.getInstance().currentUser != null) {
            User_rider.updateToken(this,token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        Log.d("Notification", "Title: $title, Body: $body")

        // Default intent: open SplashScreen
        var intent = Intent(this, SplashScreen::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // "Driver Arrived" notification
        if (title == "Driver Arrived") {
            // intent = Intent(this, RideActivity::class.java)
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // "Trip Completed" notification
        if (title == "Trip Completed") {
            // Example: Open RideHistoryActivity
            // intent = Intent(this, RideHistoryActivity::class.java)
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // Show the notification
        Common.showNotification(this, 0, title, body, intent)
    }

}