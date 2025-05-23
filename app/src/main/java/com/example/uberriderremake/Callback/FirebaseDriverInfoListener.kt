package com.example.uberriderremake.Callback

import com.example.uberriderremake.Model.DriverGeoModel

interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?)
}