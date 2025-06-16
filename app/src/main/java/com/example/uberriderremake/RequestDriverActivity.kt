 package com.example.uberriderremake

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.gms.location.FusedLocationProviderClient
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.google.firebase.database.ValueEventListener
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.uberriderremake.Model.DriverGeoModel
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
import com.example.uberriderremake.databinding.FragmentHomeBinding
import com.example.uberriderremake.databinding.LayoutConfirmPickupBinding
import com.example.uberriderremake.databinding.LayoutConfirmUberBinding
import com.example.uberriderremake.databinding.LayoutDriverInfoBinding
import com.example.uberriderremake.databinding.LayoutFindingYourDriverBinding
import com.example.uberriderremake.databinding.OriginInfoWindowsBinding
import com.example.uberriderremake.login.User_rider
import com.firebase.ui.auth.data.model.User
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.PolyUtil
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
     private lateinit var driverInfoBinding: LayoutDriverInfoBinding

     private lateinit var fragmentHomeBinding: FragmentHomeBinding


     private var driverMarker: Marker? = null
     private var routePolyline: Polyline? = null
     private var riderMarker: Marker? = null



     private var isDriverAcceptedToastShown = false



     private lateinit var mainLayout: RelativeLayout

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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        confirmUberBinding = LayoutConfirmUberBinding.bind(binding.includeConfirmUber.root)
        confirmPickupBinding = LayoutConfirmPickupBinding.bind(binding.includeConfirmPickup.root)
        val originInfoWindowView = layoutInflater.inflate(R.layout.origin_info_windows, null)
        originInfoBinding = OriginInfoWindowsBinding.bind(originInfoWindowView)
        findingDriverBinding = LayoutFindingYourDriverBinding.bind(binding.includeFindingYourDriver.root)
        driverInfoBinding = LayoutDriverInfoBinding.bind(binding.includeDriverInfo.root)

        mainLayout = findViewById(R.id.main_layout)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("Location", "Got location: ${location.latitude}, ${location.longitude}")
                // Use location here
            } else {
                Log.d("Location", "Location is null")
            }
        }

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

             val foundDriver = findNearbyDriver(selectedPlaceEvent)
             if (foundDriver != null) {
                 // 1. Get reference to "Trips" node (changed from "Rides")
                 val tripsRef = FirebaseDatabase.getInstance().getReference("Trips")

                 // 2. Generate a unique trip ID
                 val tripId = tripsRef.push().key ?: return@setOnClickListener

                 val locationCallback = object : LocationCallback() {
                     override fun onLocationResult(locationResult: LocationResult) {
                         val location = locationResult.lastLocation
                         if (location != null) {
                             Log.d("DriverLocation", "Got location: ${location.latitude}, ${location.longitude}")
                             tripsRef.child("currentLat").setValue(location.latitude)
                             tripsRef.child("currentLng").setValue(location.longitude)
                         }
                     }
                 }


                 // 3. Prepare trip data with all fields matching your screenshot
                 val tripData = mapOf(
                     "cancel" to false,
                     "currentLat" to 0.0,
                     "currentLng" to 0.0,
                     "destination" to "${selectedPlaceEvent!!.destination.latitude},${selectedPlaceEvent!!.destination.longitude}",
                     "destinationString" to "${selectedPlaceEvent!!.destination.latitude},${selectedPlaceEvent!!.destination.longitude}",
                     "distancePickup" to "0.0 mi", // Will be updated by driver
                     "done" to false,
                     "driver" to "", // Empty until driver accepts
                     "origin" to "${selectedPlaceEvent!!.origin.latitude},${selectedPlaceEvent!!.origin.longitude}",
                     "originString" to "${selectedPlaceEvent!!.origin.latitude},${selectedPlaceEvent!!.origin.longitude}",
                     "rider" to FirebaseAuth.getInstance().currentUser!!.uid,
                     "durationPickup" to "0 min" // Will be updated by driver
                 )

                 // 4. Add the trip entry to Firebase
                 tripsRef.child(tripId).setValue(tripData)
                     .addOnSuccessListener {
                         Log.d("TripRequest", "Trip requested successfully!")
                         Toast.makeText(this, "Trip requested successfully!", Toast.LENGTH_SHORT).show()

                         // Send notification to driver
                         User_rider.sendRequestToDriver(
                             context = this,
                             mainLayout = mainLayout,
                             foundDriver = foundDriver,
                             selectedPlaceEvent = selectedPlaceEvent!!,
                             tripId = tripId
                         )

                         val tripRef = FirebaseDatabase.getInstance().getReference("Trips").child(tripId)
                         tripRef.addValueEventListener(object : ValueEventListener {
                             override fun onDataChange(snapshot: DataSnapshot) {
                                 val status = snapshot.child("status").getValue(String::class.java)
                                 Log.d("TripRequest", "Trip status changed: $status")

                                 // Get rider origin from trip data (THIS IS THE FIX)
                                 val originString = snapshot.child("origin").getValue(String::class.java) ?: ""
                                 val destinationString = snapshot.child("destination").getValue(String::class.java) ?: ""
                                 val riderLatLng = if (originString.isNotEmpty() && originString.contains(",")) {
                                     val parts = originString.split(",")
                                     LatLng(parts[0].toDouble(), parts[1].toDouble())
                                 } else null

                                 val destinationLatLng = if (destinationString.isNotEmpty() && destinationString.contains(",")) {
                                     val parts = destinationString.split(",")
                                     LatLng(parts[0].toDouble(), parts[1].toDouble())
                                 } else null

                                 val lat = snapshot.child("driverLocation").child("lat").getValue(Double::class.java) ?: 0.0
                                 val lng = snapshot.child("driverLocation").child("lng").getValue(Double::class.java) ?: 0.0
                                 val driverLatLng = LatLng(lat, lng)


                                 Log.d("TripRequest", "Driver location updated: $driverLatLng")
                                 Log.d("TripRequest", "Rider location updated: $riderLatLng")

                                 if (lat != 0.0 && lng != 0.0 && riderLatLng != null && status == "accepted") {
                                     updateDriverOnMap(driverLatLng, riderLatLng)
                                     drawRoute(driverLatLng, riderLatLng)

                                     // Calculate distance to pickup
                                     val results = FloatArray(1)
                                     Location.distanceBetween(
                                         driverLatLng.latitude, driverLatLng.longitude,
                                         riderLatLng!!.latitude, riderLatLng!!.longitude,
                                         results
                                     )
                                     val distanceInMeters = results[0]

                                     if (distanceInMeters <= 50) {
                                         // Optionally, also show a Toast:
                                         Toast.makeText(this@RequestDriverActivity, "Driver Arrived", Toast.LENGTH_LONG).show()
                                     }

                                 }

                                 if (lat != 0.0 && lng != 0.0 && destinationLatLng != null && status == "rideStarted") {
                                     mMap.clear()
                                     drawRoute(driverLatLng, destinationLatLng)
                                     mMap.addMarker(MarkerOptions().position(driverLatLng).title("You"))
                                     mMap.addMarker(MarkerOptions().position(destinationLatLng).title("Destination"))
                                 }

                                 if (lat != 0.0 && lng != 0.0 && destinationLatLng != null && status == "completed") {
                                     mMap.clear()
                                     driverInfoBinding.root.visibility = View.GONE
                                     fragmentHomeBinding.root.visibility = View.VISIBLE
                                 }

                                 when (status) {
                                     "accepted" -> {
                                         // Hide finding driver and fill maps layouts
                                         findingDriverBinding.root.visibility = View.GONE
                                         binding.fillMaps.visibility = View.GONE

                                         // Cancel animations and remove user circle
                                         lastPulseAnimator?.cancel()
                                         lastPulseAnimator = null
                                         lastUserCircle?.remove()
                                         lastUserCircle = null
                                         animator?.cancel()
                                         animator = null

                                         // Get driverId from trip node (could be "driver" or "driverId")
                                         val driverId = snapshot.child("driverId").getValue(String::class.java)
                                             ?: snapshot.child("driver").getValue(String::class.java) ?: ""
                                         Log.d("TripRequest", "Driver ID: $driverId")
                                         if (driverId.isNotEmpty()) {
                                             val driverUsersRef = FirebaseDatabase.getInstance().getReference("users")
                                             driverUsersRef.child(driverId)
                                                 .addListenerForSingleValueEvent(object : ValueEventListener {
                                                     override fun onDataChange(driverSnapshot: DataSnapshot) {
                                                         val driverName = driverSnapshot.child("name").getValue(String::class.java) ?: ""
                                                         val carType = driverSnapshot.child("car").getValue(String::class.java) ?: ""
                                                         val carNumber = driverSnapshot.child("carNumber").getValue(String::class.java) ?: ""
                                                         val driverImgUrl = driverSnapshot.child("profileImageUrl").getValue(String::class.java)
                                                         val driverPhone = driverSnapshot.child("phone").getValue(String::class.java) ?: ""
                                                         Log.d(
                                                             "TripRequest",
                                                             "Driver info: name=$driverName, car=$carType, carNumber=$carNumber, imgUrl=$driverImgUrl"
                                                         )

                                                         binding.includeDriverInfo.imgCallDriver.setOnClickListener {
                                                             if (driverPhone.isNotEmpty()) {
                                                                 val intent = Intent(Intent.ACTION_DIAL)
                                                                 intent.data = Uri.parse("tel:$driverPhone")
                                                                 startActivity(intent)
                                                             }
                                                         }

                                                         // 1. Show toast
                                                         if (!isDriverAcceptedToastShown) {
                                                             Toast.makeText(
                                                                 this@RequestDriverActivity,
                                                                 "Your ride is accepted by $driverName",
                                                                 Toast.LENGTH_LONG
                                                             ).show()
                                                             isDriverAcceptedToastShown = true
                                                         }

                                                         // 2. Show driver info layout (CardView)
                                                         binding.includeDriverInfo.driverInfoLayout.visibility = View.VISIBLE

                                                         // 3. Set driver info in the layout
                                                         binding.includeDriverInfo.txtDriverName.text = driverName
                                                         binding.includeDriverInfo.txtCarType.text = carType
                                                         binding.includeDriverInfo.txtCarNumber.text = carNumber

                                                         // 4. Load driver image (if using Glide)
                                                         if (!driverImgUrl.isNullOrEmpty()) {
                                                             Glide.with(this@RequestDriverActivity)
                                                                 .load(driverImgUrl)
                                                                 .placeholder(R.drawable.baseline_account_circle_24)
                                                                 .error(R.drawable.baseline_account_circle_24)
                                                                 .into(binding.includeDriverInfo.imgDriver)
                                                         } else {
                                                             binding.includeDriverInfo.imgDriver.setImageResource(R.drawable.baseline_account_circle_24)
                                                         }
                                                     }
                                                     override fun onCancelled(error: DatabaseError) {
                                                         Log.e("TripRequest", "Failed to fetch driver info: ${error.message}")
                                                     }
                                                 })
                                         }
                                     }
//
                                 }

//                                 else {
//                                     Log.d("TripRequest", "Status is not accepted: $status")
//                                 }
                             }
                             override fun onCancelled(error: DatabaseError) {
                                 Log.e("TripRequest", "Trip listener cancelled: ${error.message}")
                             }
                         })

                         // Timeout handler: if no driver accepts in 30 seconds
                         val handler = android.os.Handler(mainLooper)
                         handler.postDelayed({
                             tripRef.get().addOnSuccessListener { snapshot ->
                                 val status = snapshot.child("status").getValue(String::class.java)
                                 Log.d("TripRequest", "Timeout check, trip status: $status")
                                 if (status != "accepted" && status != "rideStarted" && status != "completed") {
                                     lastPulseAnimator?.cancel()
                                     lastPulseAnimator = null

                                     lastUserCircle?.remove()
                                     lastUserCircle = null

                                     animator?.cancel()
                                     animator = null

                                     Toast.makeText(this, "No nearby driver accepted your ride, try again please.", Toast.LENGTH_LONG).show()
                                     // Optionally, hide the searching animation/layout
                                     findingDriverBinding.root.visibility = View.GONE
                                     binding.fillMaps.visibility = View.GONE
                                 }
                             }
                         }, 30000) // 30 seconds
                     }
                     .addOnFailureListener { error ->
                         Log.e("TripRequest", "Failed to request trip: ${error.message}")
                         Toast.makeText(this, "Failed to request trip: ${error.message}", Toast.LENGTH_SHORT).show()
                     }


                     .addOnFailureListener { error ->
                         Toast.makeText(
                             this,
                             "Failed to request trip: ${error.message}",
                             Toast.LENGTH_SHORT
                         ).show()
                     }
             }

         }
     }

     private fun updateDriverOnMap(driverLatLng: LatLng, riderLatLng: LatLng) {
         driverMarker?.remove()
         riderMarker?.remove()
         routePolyline?.remove()

         driverMarker = mMap.addMarker(
             MarkerOptions()
                 .position(driverLatLng)
                 .icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap(this, R.drawable.car, 45, 90)))
                 .title("Driver Location")
         )
         riderMarker = mMap.addMarker(
             MarkerOptions()
                 .position(riderLatLng)
                 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                 .title("Rider Location")
         )

         // Move camera to show both markers
         val bounds = LatLngBounds.builder()
             .include(driverLatLng)
             .include(riderLatLng)
             .build()
         mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))

         drawRoute(driverLatLng, riderLatLng)
     }

     fun resizeBitmap(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
         val imageBitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
         return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
     }

     private fun drawRoute(origin: LatLng, destination: LatLng) {
         val originString = "${origin.latitude},${origin.longitude}"
         val destinationString = "${destination.latitude},${destination.longitude}"

         compositeDisposable.add(
             iGoogleAPI.getDirections(
                 "driving",
                 "less_driving",
                 originString,
                 destinationString,
                 getString(R.string.maps_directions_api_key)
             )
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe({ response ->
                     val jsonObject = JSONObject(response)
                     val routes = jsonObject.getJSONArray("routes")
                     if (routes.length() > 0) {
                         val overviewPolyline = routes.getJSONObject(0)
                             .getJSONObject("overview_polyline")
                             .getString("points")
                         val polylineList = PolyUtil.decode(overviewPolyline)
                         animatePolyline(polylineList)
                     } else {
                         Log.e("Route", "No routes found in response")
                     }
                 }, { throwable ->
                     Log.e("Route", "Error fetching route: ${throwable.message}")
                 })
         )
     }

     private fun animatePolyline(polylineList: List<LatLng>) {
         // Always remove the previous polyline before adding a new one
         routePolyline?.remove()
         routePolyline = mMap.addPolyline(
             PolylineOptions()
                 .color(Color.BLACK)
                 .width(8f)
                 .addAll(polylineList)
         )
     }


     private fun addMarkerWithPulseAnimation() {
         confirmPickupBinding.confirmPickupLayout.visibility = View.GONE
         binding.fillMaps.visibility = View.VISIBLE
         findingDriverBinding.findingYourRideLayout.visibility = View.VISIBLE


         originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker())
             .position(selectedPlaceEvent!!.origin))


         addPulsatingEffect(selectedPlaceEvent!!)
     }

     private fun addPulsatingEffect(selectedPlaceEvent: SelectedPlaceEvent) {
         if(lastPulseAnimator != null) lastPulseAnimator!!.cancel()
         if(lastUserCircle != null) lastUserCircle!!.center = selectedPlaceEvent.origin
         lastPulseAnimator = Common.valueAnimate(duration, object : ValueAnimator.AnimatorUpdateListener{
             override fun onAnimationUpdate(animation: ValueAnimator) {
                 if(lastUserCircle != null) lastUserCircle!!.radius = animation!!.animatedValue.toString().toDouble() else {
                     lastUserCircle = mMap.addCircle(CircleOptions()
                         .center(selectedPlaceEvent.origin)
                         .radius(animation!!.animatedValue.toString().toDouble())
                         .strokeColor(Color.WHITE)
                         .fillColor(ContextCompat.getColor(this@RequestDriverActivity, R.color.map_darker))
                     )
                 }
             }

         })

         // Start Rotating Camera
         startMapCameraSpinningAnimation(selectedPlaceEvent)

     }

     private fun startMapCameraSpinningAnimation(selectedPlaceEvent: SelectedPlaceEvent?) {
         if(animator != null) animator!!.cancel()
         animator = ValueAnimator.ofFloat(0f,(DESIRED_NUM_OF_SPINS*360).toFloat())
         animator!!.duration = (DESIRED_NUM_OF_SPINS*DESIRED_SECONDS_PER_ONE_FULL_360_SPIN*1000).toLong()
         animator!!.interpolator = LinearInterpolator()
         animator!!.startDelay = (100)
         animator!!.addUpdateListener { valueAnimator ->
             val newBearingValue = valueAnimator.animatedValue as Float
             mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                 .target(selectedPlaceEvent!!.origin)
                 .zoom(16f)
                 .tilt(45f)
                 .bearing(newBearingValue)
                 .build()
             ))

         }
         animator!!.start()

         findNearbyDriver(selectedPlaceEvent)
     }

     private fun findNearbyDriver(selectedPlaceEvent: SelectedPlaceEvent?): DriverGeoModel? {
         if(Common.driversFound.size > 0) {
             var min = 0f
             var foundDriver = Common.driversFound[Common.driversFound.keys.iterator().next()]
             val currentRiderLocation = Location("")
             currentRiderLocation.latitude = selectedPlaceEvent!!.origin!!.latitude
             currentRiderLocation.longitude = selectedPlaceEvent.origin!!.longitude

             for(key in Common.driversFound.keys) {
                 val driverLocation = Location("")
                 driverLocation.latitude = Common.driversFound[key]!!.geoLocation!!.latitude
                 driverLocation.longitude = Common.driversFound[key]!!.geoLocation!!.longitude

                 if(min == 0f) {
                     min = driverLocation.distanceTo(currentRiderLocation)
                     foundDriver = Common.driversFound[key]
                 }
                 else if(driverLocation.distanceTo(currentRiderLocation) < min) {
                     min = driverLocation.distanceTo(currentRiderLocation)
                     foundDriver = Common.driversFound[key]
                 }
             }
             return foundDriver
         } else {
             Snackbar.make(binding.mainLayout, getString(R.string.drivers_not_found), Snackbar.LENGTH_LONG).show()
             return null
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
