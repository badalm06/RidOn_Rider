 package com.example.uberriderremake

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import com.google.android.gms.location.FusedLocationProviderClient
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.uberriderremake.Model.EventBus.SelectedPlaceEvent
import com.example.uberriderremake.Remote.IGoogleAPI
import com.example.uberriderremake.Remote.RetrofitClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.uberriderremake.databinding.ActivityRequestDriverBinding
import com.example.uberriderremake.databinding.LayoutConfirmPickupBinding
import com.example.uberriderremake.databinding.LayoutConfirmUberBinding
import com.example.uberriderremake.databinding.LayoutFindingYourDriverBinding
import com.example.uberriderremake.databinding.OriginInfoWindowsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Runnable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject


 class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

     companion object {
         private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
     }

     // Spinning Animation
     var animator: ValueAnimator?=null
     private val DESIRED_NUM_OF_SPINS = 5
     private val DESIRED_SECONDS_PER_ONE_FULL_360_SPIN = 40


     // Effects
     var lastUserCircle: Circle?= null
     val duration = 1000
     var lastPulseAnimator: ValueAnimator?= null

    internal lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityRequestDriverBinding
     private lateinit var confirmUberBinding: LayoutConfirmUberBinding
     private lateinit var confirmPickupBinding: LayoutConfirmPickupBinding
     internal lateinit var originInfoBinding: OriginInfoWindowsBinding
     private lateinit var findingDriverBinding: LayoutFindingYourDriverBinding


     internal lateinit var txt_origin: TextView

    internal var selectedPlaceEvent: SelectedPlaceEvent? = null

     private lateinit var mapFragment: SupportMapFragment

     //Routes
     private val compositeDisposable = CompositeDisposable()
     private lateinit var iGoogleAPI: IGoogleAPI
     private var blackPolyline: Polyline?= null
     private var greyPolyline: Polyline?= null
     private var polylineOptions: PolylineOptions?= null
     private var blackPolylineOptions: PolylineOptions?= null
     private var polylineList: ArrayList<LatLng?>? = null
     internal var originMarker: Marker?= null
     internal var destinationMarker: Marker?= null

     private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


     override fun onStart() {
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
        super.onStart()
    }

     override fun onStop() {
         compositeDisposable.clear()
         if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java))
             EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
         EventBus.getDefault().unregister(this)
         super.onStop()
     }

     @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
     fun onSelectPlaceEvent(event: SelectedPlaceEvent) {
         selectedPlaceEvent = event
     }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        confirmUberBinding = LayoutConfirmUberBinding.bind(binding.includeConfirmUber.root)
        confirmPickupBinding = LayoutConfirmPickupBinding.bind(binding.includeConfirmPickup.root)
        val originInfoWindowView = layoutInflater.inflate(R.layout.origin_info_windows, null)
        originInfoBinding = OriginInfoWindowsBinding.bind(originInfoWindowView)
        findingDriverBinding = LayoutFindingYourDriverBinding.bind(binding.includeFindingYourDriver.root)


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

     private fun init() {
         iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

         // Event
         confirmUberBinding.btnConfirmUber.setOnClickListener {
             confirmPickupBinding.confirmPickupLayout.visibility = View.VISIBLE
             confirmUberBinding.confirmUberLayout.visibility = View.GONE


             setDataPickup()
         }

         confirmPickupBinding.btnConfirmPickup.setOnClickListener {
             if(mMap == null) return@setOnClickListener
             if(selectedPlaceEvent == null) return@setOnClickListener

             // Clear map
             mMap.clear()

             // Tilt
             val cameraPos = CameraPosition.Builder().target(selectedPlaceEvent!!.origin)
                 .tilt(45f)
                 .zoom(16f)
                 .build()
             mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos))

             // Start Animation
             addMarkerWithPulseAnimation()

         }
     }

     private fun addMarkerWithPulseAnimation() {
         confirmPickupBinding.confirmPickupLayout.visibility = View.GONE
         binding.fillMaps.visibility = View.VISIBLE
         findingDriverBinding.findingYourRideLayout.visibility = View.VISIBLE

         originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker())
             .position(selectedPlaceEvent!!.origin))


         addPulsatingEffect(selectedPlaceEvent!!.origin)
     }

     private fun addPulsatingEffect(origin: LatLng) {
         if(lastPulseAnimator != null) lastPulseAnimator!!.cancel()
         if(lastUserCircle != null) lastUserCircle!!.center = origin
         lastPulseAnimator = Common.valueAnimate(duration, object : ValueAnimator.AnimatorUpdateListener{
             override fun onAnimationUpdate(animation: ValueAnimator) {
                 if(lastUserCircle != null) lastUserCircle!!.radius = animation!!.animatedValue.toString().toDouble() else {
                     lastUserCircle = mMap.addCircle(CircleOptions()
                         .center(origin)
                         .radius(animation!!.animatedValue.toString().toDouble())
                         .strokeColor(Color.WHITE)
                         .fillColor(ContextCompat.getColor(this@RequestDriverActivity, R.color.map_darker))
                     )
                 }
             }

         })

         // Start Rotating Camera
         startMapCameraSpinningAnimation(mMap.cameraPosition.target)

     }

     private fun startMapCameraSpinningAnimation(target: LatLng) {
         if(animator != null) animator!!.cancel()
         animator = ValueAnimator.ofFloat(0f,(DESIRED_NUM_OF_SPINS*360).toFloat())
         animator!!.duration = (DESIRED_NUM_OF_SPINS*DESIRED_SECONDS_PER_ONE_FULL_360_SPIN*1000).toLong()
         animator!!.interpolator = LinearInterpolator()
         animator!!.startDelay = (100)
         animator!!.addUpdateListener { valueAnimator ->
             val newBearingValue = valueAnimator.animatedValue as Float
             mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                 .target(target)
                 .zoom(16f)
                 .tilt(45f)
                 .bearing(newBearingValue)
                 .build()
             ))

         }
         animator!!.start()

         findNearbyDriver(target)
     }

     private fun findNearbyDriver(target: LatLng?) {
         if(Common.driversFound.size > 0) {
             var min = 0f
             var foundDriver = Common.driversFound[Common.driversFound.keys.iterator().next()]   // Default found driver is the fist driver
             val currentRiderLocation = Location("")
             currentRiderLocation.latitude = target!!.latitude
             currentRiderLocation.longitude = target!!.longitude

             for(key in Common.driversFound.keys) {
                 val driverLocation = Location("")
                 driverLocation.latitude = Common.driversFound[key]!!.geoLocation!!.latitude
                 driverLocation.longitude = Common.driversFound[key]!!.geoLocation!!.longitude

                 if(min == 0f) {
                     min = driverLocation.distanceTo(currentRiderLocation)
                     foundDriver = Common.driversFound[key]
                 }
                 else if(driverLocation.distanceTo(currentRiderLocation) < min)
                 {
                     min = driverLocation.distanceTo(currentRiderLocation)
                     foundDriver = Common.driversFound[key]
                 }
             }
             Snackbar.make(binding.mainLayout, StringBuilder("Found Driver: ").append(foundDriver!!.driverInfoModel!!.phone), Snackbar.LENGTH_LONG).show()
         }
         else {
             Snackbar.make(binding.mainLayout, getString(R.string.drivers_not_found), Snackbar.LENGTH_LONG).show()
         }
     }

     override fun onDestroy() {
         if(animator != null) animator!!.end()
         super.onDestroy()
     }

     private fun setDataPickup() {
         confirmPickupBinding.txtAddressPickup.text  = if(true) originInfoBinding.txtOrigin.text else "None"
         mMap.clear()

         addPickupMarker()
     }

     private fun addPickupMarker() {
         val view = layoutInflater.inflate(R.layout.pickup_info_window, null)

         val generator = IconGenerator(this)
         generator.setContentView(view)
         generator.setBackground(ColorDrawable(Color.TRANSPARENT))
         val icon = generator.makeIcon()
         originMarker = mMap.addMarker(MarkerOptions()
             .icon(BitmapDescriptorFactory.fromBitmap(icon))
             .position(selectedPlaceEvent!!.origin))

     }

     /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

         // Check location permission and enable location
         if (checkLocationPermission()) {
             enableMyLocation()
         } else {
             requestLocationPermission()
         }

        selectedPlaceEvent?.let {
            drawPath(it)
        } ?: run {
            Log.e("drawPath", "selectedPlaceEvent is null")
            Toast.makeText(this, "Failed to draw path: location info missing", Toast.LENGTH_SHORT).show()
        }


        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                R.raw.uber_maps_style))
            if(!success)
                Snackbar.make(mapFragment.requireView(), "Load map style failed", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception)
        {
            Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
        }

    }


     private fun checkLocationPermission(): Boolean {
         return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
     }

     private fun requestLocationPermission() {
         ActivityCompat.requestPermissions(
             this,
             arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
             LOCATION_PERMISSION_REQUEST_CODE
         )
     }

     private fun enableMyLocation() {
         if (checkLocationPermission()) {
             mMap.isMyLocationEnabled = true
             getDeviceLocation()
         }
     }

     @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
     private fun getDeviceLocation() {
         fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
             if (location != null) {
                 val currentLatLng = LatLng(location.latitude, location.longitude)
                 mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

             } else {
                 Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
             }
         }.addOnFailureListener {
             Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
         }
     }

     // Handle permission request result
     override fun onRequestPermissionsResult(
         requestCode: Int, permissions: Array<out String>, grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)

         if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
             if (grantResults.isNotEmpty()
                 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                 && grantResults[1] == PackageManager.PERMISSION_GRANTED
             ) {
                 enableMyLocation()
             } else {
                 Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
             }
         }
     }


     private fun drawPath(selectedPlaceEvent: SelectedPlaceEvent) {
         // Request API
         compositeDisposable.add(iGoogleAPI.getDirections("driving",
             "less_driving",
             selectedPlaceEvent.originString, selectedPlaceEvent.destinationString,
             getString(R.string.maps_directions_api_key))
             .subscribeOn(Schedulers.io())
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe { returnResult ->
                 Log.d("API_RETURN", returnResult)
                 try {

                     val jsonObject = JSONObject(returnResult)
                     val jsonArray = jsonObject.getJSONArray("routes");
                     for (i in 0 until jsonArray.length()) {
                         val route = jsonArray.getJSONObject(i)
                         val poly = route.getJSONObject("overview_polyline")
                         val polyline = poly.getString("points")
                         polylineList = Common.decodePoly(polyline)
                     }

                     polylineOptions = PolylineOptions()
                     polylineOptions!!.color(Color.GRAY)
                     polylineOptions!!.width(12f)
                     polylineOptions!!.startCap(SquareCap())
                     polylineOptions!!.jointType(JointType.ROUND)
                     polylineOptions!!.addAll(polylineList!!)
                     greyPolyline = mMap.addPolyline(polylineOptions!!)

                     blackPolylineOptions = PolylineOptions()
                     blackPolylineOptions!!.color(Color.BLACK)
                     blackPolylineOptions!!.width(5f)
                     blackPolylineOptions!!.startCap(SquareCap())
                     blackPolylineOptions!!.jointType(JointType.ROUND)
                     blackPolylineOptions!!.addAll(polylineList!!)
                     blackPolyline = mMap.addPolyline(blackPolylineOptions!!)


                     // Animator
                     val valueAnimator = ValueAnimator.ofInt(0, 100)
                     valueAnimator.duration = 1100
                     valueAnimator.repeatCount = ValueAnimator.INFINITE
                     valueAnimator.interpolator = LinearInterpolator()
                     valueAnimator.addUpdateListener { value ->
                         val points = greyPolyline!!.points
                         val percentValue = value.animatedValue.toString().toInt()
                         val size = points.size
                         val newpoints = (size * (percentValue / 100.0f)).toInt()
                         val p = points.subList(0, newpoints)
                         blackPolyline!!.points = p
                     }
                     valueAnimator.start()


                     val latLngBound = LatLngBounds.Builder()
                         .include(selectedPlaceEvent.origin)
                         .include(selectedPlaceEvent.destination)
                         .build()

                     // Add car icon for origin
                     val objects = jsonArray.getJSONObject(0)
                     val legs = objects.getJSONArray("legs")
                     val legsObject = legs.getJSONObject(0)
                     val time = legsObject.getJSONObject("duration")
                     val duration = time.getString("text")
                     val start_address = legsObject.getString("start_address")
                     val end_address = legsObject.getString("end_address")

                     addOriginMarker(duration, start_address)

                     addDestinationMarker(end_address)

                     mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                     mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))


                 } catch (e: java.lang.Exception) {
                     Toast.makeText(this, e.message!!, Toast.LENGTH_SHORT).show()
                 }
             }
         )
     }
 }


 private fun RequestDriverActivity.addOriginMarker(duration: String, start_address: String) {
     val view = layoutInflater.inflate(R.layout.origin_info_windows, null)
     originInfoBinding = OriginInfoWindowsBinding.bind(view)

     originInfoBinding.txtOrigin.text = start_address
     originInfoBinding.txtTime.text = duration
     val generator = IconGenerator(this)
     generator.setContentView(view)
     generator.setBackground(ColorDrawable(Color.TRANSPARENT))
     val icon = generator.makeIcon()
     originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.origin))

     originMarker?.showInfoWindow()
 }

 private fun RequestDriverActivity.addDestinationMarker(end_address: String) {
     destinationMarker = mMap.addMarker(
         MarkerOptions()
             .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
             .position(selectedPlaceEvent!!.destination)
             .title(end_address)
     )
     destinationMarker?.showInfoWindow()
 }


// private fun RequestDriverActivity.addDestinationMarker(endAddress:String) {
//     val view = layoutInflater.inflate(R.layout.destination_info_windows, null)
//     val txt_destination = view.findViewById<View>(R.id.txt_destination) as TextView
//
//     txt_destination.text  = Common.formatAddress(endAddress)
//
//     val generator = IconGenerator(this)
//     generator.setContentView(view)
//     generator.setBackground(ColorDrawable(Color.TRANSPARENT))
//     val icon = generator.makeIcon()
//     destinationMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.destination))
//
// }
//
// private fun RequestDriverActivity.addOriginMarker(duration: String, startAddress: String) {
//
//     val view = layoutInflater.inflate(R.layout.origin_info_windows, null)
//     val txt_time = view.findViewById<View>(R.id.txt_time) as TextView
//     txt_origin = view.findViewById<View>(R.id.txt_origin) as TextView
//
//     txt_time.text = Common.formatDuration(duration)
//     txt_origin.text  = Common.formatAddress(startAddress)
//
//     val generator = IconGenerator(this)
//     generator.setContentView(view)
//     generator.setBackground(ColorDrawable(Color.TRANSPARENT))
//     val icon = generator.makeIcon()
//     originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.origin))
//
//
// }
