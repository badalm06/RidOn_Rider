package com.example.uberriderremake.ui.home

import android.Manifest
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.uberriderremake.Callback.FirebaseDriverInfoListener
import com.example.uberriderremake.Callback.FirebaseFailedListener
import com.example.uberriderremake.Common
import com.example.uberriderremake.Model.DriverGeoModel
import com.example.uberriderremake.Model.DriverInfoModel
import com.example.uberriderremake.Model.GeoQueryModel
import com.example.uberriderremake.R
import com.example.uberriderremake.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.Locale

class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener {

        private var _binding: FragmentHomeBinding? = null
        private val binding get() = _binding!!
        private lateinit var mMap: GoogleMap

        private lateinit var mapFragment: SupportMapFragment


        //Location
        private lateinit var locationRequest: LocationRequest
        private lateinit var locationCallback: LocationCallback
        private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


        // Online System
        private lateinit var onlineRef: DatabaseReference
        private lateinit var currentUserRef: DatabaseReference
        private lateinit var riderLocationRef: DatabaseReference
        private lateinit var geoFire: GeoFire

        //Load Driver
        var distance = 1.0
        val LIMIT_RANGE = 10.0
        var previousLocation: Location? = null
        var currentLocation: Location? = null

    var firstTime = true

    // Listener
    lateinit var iFirebaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var iFirebaseFailedListener: FirebaseFailedListener

    var cityName = ""



        private val onlineValueEventListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists())
                    currentUserRef.onDisconnect().removeValue()
            }

            override fun onCancelled(p0: DatabaseError) {
                Snackbar.make(mapFragment.requireView(),p0.message, Snackbar.LENGTH_LONG).show()
            }

        }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationEnableDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Required")
            .setMessage("Please enable location services to use this app.")
            .setCancelable(false)
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            init()
        } else {
            Toast.makeText(requireContext(), "Location permission is required!", Toast.LENGTH_SHORT).show()
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            val root: View = binding.root

            init()

            mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            return root
        }

        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        private fun init() {

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request permissions here, or return
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }

            if (!isLocationEnabled()) {
                showLocationEnableDialog()
                return // Do not proceed if location is off
            }
            iFirebaseFailedListener = object : FirebaseFailedListener {
                override fun onFirebaseFailed(message: String) {
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
                }
            }


            //      iFirebaseDriverInfoListener = this

            onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")
            riderLocationRef = FirebaseDatabase.getInstance().getReference(Common.RIDER_LOCATION_REFERENCE)
            currentUserRef = FirebaseDatabase.getInstance().getReference(Common.RIDER_LOCATION_REFERENCE).child(
                FirebaseAuth.getInstance().currentUser!!.uid
            )

            geoFire = GeoFire(riderLocationRef)

            currentUserRef.onDisconnect().removeValue()

            registerOnlineSystem()

            locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L // interval
            )
                .setMinUpdateIntervalMillis(3000L) // fastest interval
                .setMinUpdateDistanceMeters(10f) // smallest displacement
                .build()

            locationCallback = object : LocationCallback() {
                @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation ?: return
                    val newPos = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                    // If user has change location, calculate and load driver again
                    if(firstTime) {
                        previousLocation = locationResult.lastLocation
                        currentLocation = locationResult.lastLocation

                        firstTime = false
                    }
                    else {
                        previousLocation = currentLocation
                        currentLocation = locationResult.lastLocation
                    }

                    if(previousLocation!!.distanceTo(currentLocation!!)/1000 <= LIMIT_RANGE)
                        loadAvailableDrivers();

                    // Update Location
                    geoFire.setLocation(
                        FirebaseAuth.getInstance().currentUser!!.uid,
                        GeoLocation(locationResult.lastLocation!!.latitude, locationResult.lastLocation!!.longitude)
                    ) { key: String?, error: DatabaseError? ->
                        if(error != null) {
                            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                        } else {
                            Snackbar.make(mapFragment.requireView(),"You're Online!", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.myLooper())

            loadAvailableDrivers();
        }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun loadAvailableDrivers() {
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(),e.message!!, Snackbar.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { location ->
                if (location == null) {
                    Snackbar.make(requireView(), "Location not available", Snackbar.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                // Load all drivers in city
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList: List<Address> = ArrayList()
                try {
                    addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)!!
                    cityName = addressList[0].locality

                    Log.d("DRIVER_DEBUG", "cityName used for query: $cityName")

                    // Query
                    Common.driversFound.clear()
                    val driversLocationRef = FirebaseDatabase.getInstance().getReference("DriversLocation")
                    driversLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var pendingQueries = snapshot.childrenCount
                            if (pendingQueries == 0L) {
                                addDriverMarker()
                                return
                            }
                            for (citySnapshot in snapshot.children) {
                                val geoFire = GeoFire(citySnapshot.ref)
                                val geoQuery = geoFire.queryAtLocation(
                                    GeoLocation(location.latitude, location.longitude),
                                    15.0 // 15km radius
                                )
                                geoQuery.removeAllListeners()
                                geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
//                                    override fun onKeyEntered(key: String?, geoLocation: GeoLocation?) {
//                                        if (key != null && geoLocation != null) {
//                                            Common.driversFound.add(DriverGeoModel(key, geoLocation))
//                                            Log.d("DRIVER_DEBUG", "Adding driver: $key at ${geoLocation.latitude}, ${geoLocation.longitude}")
//                                        }
//                                    }
override fun onKeyEntered(key: String?, geoLocation: GeoLocation?) {
    if (key != null && geoLocation != null) {
        val alreadyExists = Common.driversFound.any { it.key == key }
        if (!alreadyExists) {
            Common.driversFound.add(DriverGeoModel(key, geoLocation))
            Log.d("DRIVER_DEBUG", "Adding driver: $key at ${geoLocation.latitude}, ${geoLocation.longitude}")
        } else {
            Log.d("DRIVER_DEBUG", "Driver $key already exists, not adding again.")
        }
    }
}

                                    override fun onKeyExited(key: String?) {}
                                    override fun onKeyMoved(key: String?, geoLocation: GeoLocation?) {}
                                    override fun onGeoQueryReady() {
                                        pendingQueries--
                                        if (pendingQueries == 0L) {
                                            addDriverMarker()
                                        }
                                    }
                                    override fun onGeoQueryError(error: DatabaseError?) {
                                        Snackbar.make(requireView(), error?.message ?: "GeoQuery error", Snackbar.LENGTH_SHORT).show()
                                        pendingQueries--
                                        if (pendingQueries == 0L) {
                                            addDriverMarker()
                                        }
                                    }
                                })
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Snackbar.make(requireView(), error.message, Snackbar.LENGTH_SHORT).show()
                        }
                    })

                } catch (e: IOException) {
                    Snackbar.make(requireView(),getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun addDriverMarker() {
        Log.d("DRIVER_DEBUG", "driversFound size1x: ${Common.driversFound.size}")
        if(Common.driversFound.isNotEmpty()) {
            Observable.fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { driverGeoModel: DriverGeoModel? ->
                        findDriverByKey(driverGeoModel)
                    },
                    {
                        t : Throwable? -> Snackbar.make(requireView(), t!!.message!!, Snackbar.LENGTH_SHORT).show()
                    }
                )
        }
        else {
            Snackbar.make(requireView(),getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun findDriverByKey(driverGeoModel: DriverGeoModel?) {
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(driverGeoModel!!.key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val driverInfoModel = snapshot.getValue(DriverInfoModel::class.java)
                        if (driverInfoModel != null) {
                            Common.driverInfoMap[driverGeoModel.key!!] = driverInfoModel
                            // Attach the driver info to the geo model
                            driverGeoModel.driverInfoModel = driverInfoModel
                            // Proceed with displaying driver
                            onDriverInfoLoadSuccess(driverGeoModel)
                        }
                        else {
                            // Data exists but is malformed or incomplete
                            Log.d("DRIVER_DEBUG", "Driver data for key ${driverGeoModel.key} is null or malformed.")
                        }
                    }
                    else {
                        // Key not found!
                        Log.d("DRIVER_DEBUG", "Driver key ${driverGeoModel.key} not found in DriverInfo.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (::iFirebaseFailedListener.isInitialized) {
                        iFirebaseFailedListener.onFirebaseFailed(error.message ?: "Unknown error")
                    }
                }
            })
    }

    fun resizeBitmap(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
        val imageBitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }


    override fun onMapReady(googleMap: GoogleMap) {
            mMap = googleMap

            // Request Permission
            Dexter.withContext(requireContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener{
                    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        // Enable button first
                        mMap.isMyLocationEnabled = true
                        mMap.uiSettings.isMyLocationButtonEnabled = true
                        mMap.setOnMyLocationClickListener {
                            fusedLocationProviderClient.lastLocation
                                .addOnFailureListener { e ->
                                    Toast.makeText(context!!,e.message, Toast.LENGTH_SHORT).show()
                                }.addOnSuccessListener { location ->
                                    val userLatLng = LatLng(location.latitude, location.longitude)
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                                }
                            true
                        }

                        val locationButton = (mapFragment.requireView()!!
                            .findViewById<View>("1".toInt())
                            .parent!! as View)
                            .findViewById<View>("2".toInt())
                        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        params.bottomMargin = 350           // Move to see Zoom Control
                        locationButton.layoutParams = params

                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(context!!, "Permission "+p0!!.permissionName+"was denied", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {

                    }

                }).check()

            mMap.uiSettings.isZoomControlsEnabled = true

            try {
                val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                    R.raw.uber_maps_style))
                if(!success) {
                    Log.e("EDMT_ERROR", "Style parsing error")
                }
            }catch (e: Resources.NotFoundException) {
                Log.e("EDMT_ERROR", e.message.toString())
            }


//        val customLocation = LatLng(27.4924, 77.6737) // Latitude, Longitude of Mathura
//        mMap.addMarker(MarkerOptions().position(customLocation).title("Marker in Mathura"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customLocation, 15f)) // 15f = Zoom level

      //  loadAvailableDrivers()

    }



    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?) {
        if (driverGeoModel == null || driverGeoModel.key == null) return

        val latLng = LatLng(driverGeoModel.geoLocation!!.latitude, driverGeoModel.geoLocation!!.longitude)

        if (!Common.markerList.containsKey(driverGeoModel!!.key)) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(
                        LatLng(
                            driverGeoModel!!.geoLocation!!.latitude,
                            driverGeoModel!!.geoLocation!!.longitude
                        )
                    )
                        .flat(true)
                        .title(
                            Common.buildName(
                                driverGeoModel.driverInfoModel!!.name,
                                driverGeoModel.driverInfoModel!!.email
                            )
                        )

                        .snippet(driverGeoModel.driverInfoModel!!.phone)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap(requireContext(), R.drawable.car_display, 80, 80)))

            )
            marker?.let {
                Common.markerList[driverGeoModel.key!!] = it
            }

            if (!TextUtils.isEmpty(cityName)) {
                val driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCE)
                    .child(driverGeoModel.key!!)

                val valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("DRIVER_DEBUG", "Querying users/${driverGeoModel.key}")
                        Log.d("DRIVER_DEBUG", "Snapshot exists: ${snapshot.exists()}")
                        Log.d("DRIVER_DEBUG", "Snapshot value: ${snapshot.value}")
                        // If driver location is removed from the database
                        if (!snapshot.exists()) {
                            // Remove marker from the map if it exists
                            Common.markerList[driverGeoModel.key!!]?.let { marker ->
                                marker.remove()
                                Common.markerList.remove(driverGeoModel.key!!)
                            }
                            // Remove this listener
                            driverLocation.removeEventListener(this)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Optional: handle error
                    }
                }

                driverLocation.addValueEventListener(valueEventListener)
            }

        }
    }


        override fun onResume() {
            super.onResume()
            if (::onlineRef.isInitialized) {
                registerOnlineSystem()
            }
        }

        private fun registerOnlineSystem() {
            if (!::onlineRef.isInitialized) return
            onlineRef.addValueEventListener(onlineValueEventListener)
        }

        override fun onDestroyView() {
            if (this::geoFire.isInitialized) {
                geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
            }
            super.onDestroyView()
            _binding = null
        }
    }

