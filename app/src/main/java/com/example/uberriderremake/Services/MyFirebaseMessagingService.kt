package com.example.uberriderremake.Services

import android.content.Intent
import com.example.uberriderremake.Common
import com.example.uberriderremake.login.SplashScreen
import com.example.uberriderremake.login.User_rider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(FirebaseAuth.getInstance().currentUser != null) {
            User_rider.updateToken(this,token)
        }
    }

        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            super.onMessageReceived(remoteMessage)

            val title = remoteMessage.data[Common.NOTI_TITLE]
            val body = remoteMessage.data[Common.NOTI_BODY]

            val intent = Intent(this, SplashScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            Common.showNotification(this, 0, title, body, intent)
        }
    }