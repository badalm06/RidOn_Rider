package com.example.uberriderremake

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.uberriderremake.Model.AnimationModel
import com.example.uberriderremake.Model.DriverGeoModel
import com.example.uberriderremake.Model.DriverInfoModel
import com.example.uberriderremake.login.User_rider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Calendar
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
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val NOTIFICATION_CHANNEL_ID = "Uber_Remake"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Uber Remake",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Uber_Remake"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_directions_car_24) // Replace with your icon
            .setContentTitle(title ?: "Title")
            .setContentText(body ?: "Body")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(Color.RED, 1000, 1000)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(id, notificationBuilder.build())
    }


//    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
//        val pendingIntent = if (intent != null) {
//            PendingIntent.getActivity(
//                context,
//                id,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//        } else {
//            null
//        }
//            val NOTIFICATION_CHANNEL_ID = "Uber_Remake"
//            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Uber Remake",
//                    NotificationManager.IMPORTANCE_HIGH)
//                notificationChannel.description = "Uber_Remake"
//                notificationChannel.enableLights(true)
//                notificationChannel.lightColor = Color.RED
//                notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
//                notificationChannel.enableVibration(true)
//
//                notificationManager.createNotificationChannel(notificationChannel)
//            }
//            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//            builder.setContentTitle(title)
//                .setContentText(body)
//                .setAutoCancel(false)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setDefaults(Notification.DEFAULT_VIBRATE)
//                .setSmallIcon(R.drawable.baseline_directions_car_24)
//                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.baseline_directions_car_24))
//
//            if(pendingIntent != null) {
//                builder.setContentIntent(pendingIntent!!)
//                val notification = builder.build()
//                notificationManager.notify(id, notification)
//            }
//        }


    fun buildName(firstName: String?, lastName: String?): String? {
        return java.lang.StringBuilder(firstName!!).append(" ").append(lastName).toString()
    }


    val RIDER_KEY: String = "RiderKey"
    val PICKUP_LOCATION: String = "PickupLocation"
    val REQUEST_DRIVER_TITLE: String = "RequestDriver"
    val driverSubscribe: MutableMap<String, AnimationModel> = HashMap<String, AnimationModel>()
    val NOTI_TITLE: String = "title"
    val NOTI_BODY: String = "body"
    val TOKEN_REFERENCE: String = "Token"
    val RIDER_LOCATION_REFERENCE: String="RidersLocation"
    var currentUserRider: User_rider?=null
    val DRIVERS_LOCATION_REFERENCE: String = "users"
    //val driversFound: MutableSet<DriverGeoModel> = mutableSetOf()
    val driversFound: MutableMap<String, DriverGeoModel> = HashMap<String, DriverGeoModel> ()
    val DRIVER_INFO_REFERENCE: String = "users"
    val markerList: MutableMap<String, Marker?> = HashMap<String, Marker?>()
    internal val driverInfoMap = HashMap<String, DriverInfoModel>()

    fun decodePoly(encoded: String): ArrayList<LatLng?> {
        val poly = ArrayList<LatLng?>()
        var index=0
        var len=encoded.length
        var lat=0
        var lng=0
        while(index < len)
        {
            var b: Int
            var shift = 0
            var result = 0
            do{
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5

            }while(b >= 0x20);
            val dlat = if(result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do{
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5

            }while(b >= 0x20);
            val dlng = if(result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        // You can copy this function by link or description
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return Math.toDegrees(Math.atan(lng / lat)).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()
        return (-1).toFloat()
    }

    fun setWelcomeMessage(txtWelcome: TextView) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 1 && hour <= 12)
            txtWelcome.setText(java.lang.StringBuilder("Good Morning"))
        else if (hour > 12 && hour <= 17)
            txtWelcome.setText(java.lang.StringBuilder("Good Afternoon"))
        else
            txtWelcome.setText(java.lang.StringBuilder("Good Evening"))
    }

    fun formatDuration(duration:String): CharSequence? {
        if(duration.contains("mins"))
            return duration.substring(0, duration.length-1)
        else
            return duration
    }

    fun formatAddress(startAddress:String): CharSequence? {
        val firstIndexComma = startAddress.indexOf(",")
        return startAddress.substring(0, firstIndexComma)
    }

    fun valueAnimate(duration: Int, listener: AnimatorUpdateListener): ValueAnimator {
        val va = ValueAnimator.ofFloat(0f, 100f)
        va.duration = duration.toLong()
        va.addUpdateListener(listener)
        va.repeatCount = ValueAnimator.INFINITE
        va.repeatMode = ValueAnimator.RESTART
        va.start()
        return va
    }


}
