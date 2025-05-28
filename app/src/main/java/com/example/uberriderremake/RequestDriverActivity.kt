 package com.example.uberriderremake

import android.Manifest
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import retrofit2.create
import kotlin.time.Duration

 class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

    internal lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityRequestDriverBinding

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

        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

     private fun init() {
         iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

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

       mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnMapClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlaceEvent!!.origin, 18f))
            true
        }

        selectedPlaceEvent?.let {
            drawPath(it)
        } ?: run {
            Log.e("drawPath", "selectedPlaceEvent is null")
            Toast.makeText(this, "Failed to draw path: location info missing", Toast.LENGTH_SHORT).show()
        }


        // Layout Button
        val locationButton = (findViewById<View>("1".toInt())!!.parent!! as View)
            .findViewById<View>("2".toInt())
        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 250          // Move to see Zoom Control

        mMap.uiSettings.isZoomControlsEnabled = true
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

     private fun drawPath(selectedPlaceEvent: SelectedPlaceEvent) {
         // Request API
         compositeDisposable.add(iGoogleAPI.getDirections("driving",
             "less_driving",
             selectedPlaceEvent.originString, selectedPlaceEvent.destinationString,
             getString(R.string.maps_directions_api_key))
         !!.subscribeOn(Schedulers.io())
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

 private fun RequestDriverActivity.addDestinationMarker(endAddress:String) {
     val view = layoutInflater.inflate(R.layout.destination_info_windows, null)
     val txt_destination = view.findViewById<View>(R.id.txt_destination) as TextView

     txt_destination.text  = Common.formatAddress(endAddress)

     val generator = IconGenerator(this)
     generator.setContentView(view)
     generator.setBackground(ColorDrawable(Color.TRANSPARENT))
     val icon = generator.makeIcon()
     destinationMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.destination))

 }

 private fun RequestDriverActivity.addOriginMarker(duration: String, startAddress: String) {

     val view = layoutInflater.inflate(R.layout.origin_info_windows, null)
     val txt_time = view.findViewById<View>(R.id.txt_time) as TextView
     val txt_origin = view.findViewById<View>(R.id.txt_origin) as TextView

     txt_time.text = Common.formatDuration(duration)
     txt_origin.text  = Common.formatAddress(startAddress)

     val generator = IconGenerator(this)
     generator.setContentView(view)
     generator.setBackground(ColorDrawable(Color.TRANSPARENT))
     val icon = generator.makeIcon()
     originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.origin))


 }
