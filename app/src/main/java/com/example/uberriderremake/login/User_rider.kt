package com.example.uberriderremake.login

import android.content.Context
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import com.example.uberriderremake.Model.TokenModel
import com.example.uberriderremake.Common
import com.example.uberriderremake.Model.DriverGeoModel
import com.example.uberriderremake.Model.EventBus.SelectedPlaceEvent
import com.example.uberriderremake.Model.FCMSendData
import com.example.uberriderremake.Model.NotificationContent
import com.example.uberriderremake.Model.NotificationRequest
import com.example.uberriderremake.Model.NotificationResponse
import com.example.uberriderremake.Model.PickupLocation
import com.example.uberriderremake.R
import com.example.uberriderremake.Remote.IFCMService
import com.example.uberriderremake.Remote.RetrofitFCMClient
import com.example.uberriderremake.Services.BackendService
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class User_rider(
    var name: String = "",
    var email: String = "",
    var number: String = "",
    var profileImageUrl: String? = null
) {
    companion object {
        // Move these inside the companion object
        private val retrofit = Retrofit.Builder()
            .baseUrl("https://uber-backend-462313.el.r.appspot.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        private val backendService = retrofit.create(BackendService::class.java)

        fun updateToken(context: Context, token: String) {
            val tokenModel = TokenModel()
            tokenModel.token = token

            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(tokenModel)
                .addOnFailureListener { e -> Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener { }
        }

        fun sendRequestToDriver(
            context: Context,
            mainLayout: RelativeLayout?,
            foundDriver: DriverGeoModel?,
            selectedPlaceEvent: SelectedPlaceEvent,
            tripId: String
        ) {
            // Get Token
            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(foundDriver!!.key!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val tokenModel = dataSnapshot.getValue(TokenModel::class.java)

                            val notificationData = mapOf(
                                "trip_id" to tripId,
                                "pickup_location" to "${selectedPlaceEvent.origin.latitude},${selectedPlaceEvent.origin.longitude}",
                                "rider_key" to FirebaseAuth.getInstance().currentUser!!.uid // <-- Add this line

                            )

                            val notificationContent = NotificationContent(
                                title = "Request Driver",
                                body = "This message represent for Request Driver Action"
                            )

                            val request = NotificationRequest(
                                pickup_location = PickupLocation(selectedPlaceEvent.origin.latitude, selectedPlaceEvent.origin.longitude),
                                notification = notificationContent,
                                data = notificationData
                            )


                            // Call your backend to send the notification
                            backendService.sendNotification(request).enqueue(object : retrofit2.Callback<NotificationResponse> {
                                override fun onResponse(
                                    call: retrofit2.Call<NotificationResponse>,
                                    response: retrofit2.Response<NotificationResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        Snackbar.make(mainLayout!!, "Notification sent to driver!", Snackbar.LENGTH_LONG).show()
                                    } else {
                                        Snackbar.make(mainLayout!!, "Failed to send notification.", Snackbar.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(
                                    call: retrofit2.Call<NotificationResponse>,
                                    t: Throwable
                                ) {
                                    Snackbar.make(mainLayout!!, "Network error: ${t.message}", Snackbar.LENGTH_LONG).show()
                                }
                            })
                        } else {
                            Snackbar.make(mainLayout!!, context.getString(R.string.token_not_found), Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Snackbar.make(mainLayout!!, databaseError.message, Snackbar.LENGTH_LONG).show()
                    }
                })
        }
    }
}




//        fun sendRequestToDriver(
//            context: Context,
//            mainLayout: RelativeLayout?,
//            foundDriver: DriverGeoModel?,
//            target: LatLng
//        ) {
//            val compositeDisposable = CompositeDisposable()
//            val ifcmService = RetrofitFCMClient.instance!!.create(IFCMService::class.java)
//
//            // Get Token
//            FirebaseDatabase.getInstance()
//                .getReference(Common.TOKEN_REFERENCE)
//                .child(foundDriver!!.key!!)
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        if(dataSnapshot.exists()) {
//                            val tokenModel = dataSnapshot.getValue(TokenModel::class.java)
//                            Log.d("FCM_DEBUG", "Fetched Token: ${tokenModel?.token}")
//
//
//                            val notificationData: MutableMap<String, String> = HashMap()
//                            notificationData.put(Common.NOTI_TITLE, Common.REQUEST_DRIVER_TITLE)
//                            notificationData.put(Common.NOTI_BODY, "This message represent for Request Driver Action")
//                            notificationData.put(Common.RIDER_KEY, FirebaseAuth.getInstance().currentUser!!.uid)
//                            notificationData.put(Common.PICKUP_LOCATION, StringBuilder()
//                                .append(target.latitude)
//                                .append(",")
//                                .append(target.longitude)
//                                .toString()
//                            )
//
//                            val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)
//                            Log.d("FCM_DEBUG", "Sending payload: $fcmSendData")
//
//                            compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
//                                .subscribeOn(Schedulers.newThread())
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe ({ fcmResponse ->
//                                    Log.d("FCM_DEBUG", "FCM Response: $fcmResponse")
//                                    if(fcmResponse!!.success == 0) {
//                                        compositeDisposable.clear()
//                                        Snackbar.make(mainLayout!!,context.getString(R.string.send_request_driver_failed), Snackbar.LENGTH_LONG).show()
//
//                                    }
//
//                                },{ t: Throwable? ->
//                                    Log.e("FCM_ERROR", "Throwable error: ${t?.message}")
//
//                                    compositeDisposable.clear()
//                                    Snackbar.make(mainLayout!!,t!!.message!!, Snackbar.LENGTH_LONG).show()
//
//                                }))
//
//
//                        }
//                        else {
//                            Snackbar.make(mainLayout!!,context.getString(R.string.token_not_found), Snackbar.LENGTH_LONG).show()
//                        }
//                    }
//
//                    override fun onCancelled(databaseError: DatabaseError) {
//                        Snackbar.make(mainLayout!!, databaseError.message, Snackbar.LENGTH_LONG).show()
//                    }
//
//                })
//
//        }