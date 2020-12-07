package com.example.uberbykotlin.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.uberbykotlin.Common
import com.example.uberbykotlin.R
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mapFragment: SupportMapFragment

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var userLatLng: LatLng

    //Online system
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private var onlineValueEventListener = object: ValueEventListener{
        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()

        }
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()){
                currentUserRef.onDisconnect().removeValue()
            }
        }

    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        init()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    private fun init() {

        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        driversLocationRef =
            FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
        currentUserRef =
            FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE).child(
                FirebaseAuth.getInstance().currentUser!!.uid
            )

        geoFire = GeoFire(driversLocationRef)
        registerOnlineSystem()

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.interval = 5000
        locationRequest.smallestDisplacement = 10f

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        var lastKnownLocation: Location ?= null
        //////////////////////////////////////////////////
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) {
            if (it.isSuccessful) {
                // Set the map's camera position to the current location of the device.
                if (it.result != null) {
                    lastKnownLocation = it.result
                }
            } else {
                Log.d("User Location", "Current location is null. Using defaults.")
                Log.e("User Location", "Exception: %s", it.exception)
            }
        }
        /////////////////////////////////////////////////
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)


                var newPos = LatLng(locationResult!!.lastLocation.latitude, locationResult!!.lastLocation.longitude)
                if(newPos==null){
                    newPos = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                //Update Location
                geoFire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(newPos.latitude,newPos.longitude)
                ){key:String? , error: DatabaseError->
                    if(error != null){
                        Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                    }else{
                        Snackbar.make(mapFragment.requireView(), "Your're Online!", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability?) {
                super.onLocationAvailability(p0)
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        //Request Permission
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //Enable Location Button
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {

                        Toast.makeText(context!!, "Button Clicked", Toast.LENGTH_SHORT).show()
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener {
                                Toast.makeText(context!!, "${it.message}", Toast.LENGTH_SHORT)
                                    .show()
                            }.addOnSuccessListener {
                                userLatLng = LatLng(it.latitude, it.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        18f
                                    )
                                )
                            }
                        true
                    }

                    val locationButton = (mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent as View).findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END,RelativeLayout.TRUE)
                    params.bottomMargin = 50
                    params.marginEnd = 50
                    if (ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) { }

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        context!!,
                        "Permission: ${p0!!.permissionName} was denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.uber_maps_style))
            if (!success){
                Log.e("Google Map Style", "onMapReady: style parsing error", )
            }
        }catch(e: Resources.NotFoundException){
            Log.e("Google Map Style", "onMapReady: ${e.message}", )
        }
    }
}