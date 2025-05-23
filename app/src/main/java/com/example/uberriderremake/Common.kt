package com.example.uberriderremake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.uberriderremake.Model.DriverGeoModel
import com.example.uberriderremake.Model.DriverInfoModel
import com.example.uberriderremake.login.User_rider
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.HashMap
import kotlin.text.isNullOrEmpty

object Common {

    fun buildWelcomeMessage(): String {
        return if (currentUserRider != null && !currentUserRider!!.name.isNullOrEmpty()) {
            "Welcome, ${currentUserRider!!.name}"
        } else {
            "Welcome! Rider"
        }
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent: PendingIntent? = null
        if(intent != null) {
            pendingIntent = PendingIntent.getActivity(context, id, intent!!, PendingIntent.FLAG_UPDATE_CURRENT)
            val NOTIFICATION_CHANNEL_ID = "Uber_Remake"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Uber Remake",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = "Uber_Remake"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                notificationChannel.enableVibration(true)

                notificationManager.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            builder.setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.baseline_directions_car_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.baseline_directions_car_24))

            if(pendingIntent != null) {
                builder.setContentIntent(pendingIntent!!)
                val notification = builder.build()
                notificationManager.notify(id, notification)
            }
        }
    }

    fun buildName(firstName: String?, lastName: String?): String? {
        return java.lang.StringBuilder(firstName).append(" ").append(lastName).toString()
    }


    val NOTI_TITLE: String = "title"
    val NOTI_BODY: String = "body"
    val TOKEN_REFERENCE: String = "Token"
    val RIDER_LOCATION_REFERENCE: String="RidersLocation"
    var currentUserRider: User_rider?=null
    val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    val driversFound: MutableSet<DriverGeoModel> = HashSet<DriverGeoModel> ()
    val DRIVER_INFO_REFERENCE: String = "DriverInfo"
    val markerList: MutableMap<String, Marker> = HashMap<String, Marker>()
    internal val driverInfoMap = HashMap<String, DriverInfoModel>()


}
