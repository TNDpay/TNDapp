package com.example.tnd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tnd.databinding.ActivityExploreBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding
    private lateinit var map: MapView
    private var isAddingStore = false
    private var newStoreLocation: GeoPoint? = null
    private var userAddress: String? = null

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PREFS_NAME = "MapPreferences"
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ZOOM = "last_zoom"
        private const val DEFAULT_LATITUDE = 40.359094404842565
        private const val DEFAULT_LONGITUDE = 0.3996138427450273
        private const val DEFAULT_ZOOM = 15.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userAddress = intent.getStringExtra("USER_ADDRESS")

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeMap()
        loadLastMapPosition()
        checkGpsAndRequestPermission()
        fetchLocationsAndUpdateMap()
        setupAddStoreButton()
        setupMapClickListener()
        setupConfirmCancelButtons()
    }

    private fun initializeMap() {
        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setUseDataConnection(true)
        //map.cacheManager.setCacheSize(1024L * 1024L * 100L) // 100MB cache
    }

    private fun loadLastMapPosition() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastLat = prefs.getFloat(LAST_LATITUDE, DEFAULT_LATITUDE.toFloat()).toDouble()
        val lastLon = prefs.getFloat(LAST_LONGITUDE, DEFAULT_LONGITUDE.toFloat()).toDouble()
        val lastZoom = prefs.getFloat(LAST_ZOOM, DEFAULT_ZOOM.toFloat()).toDouble()

        map.controller.apply {
            setCenter(GeoPoint(lastLat, lastLon))
            setZoom(lastZoom)
        }
    }

    private fun saveMapPosition() {
        val center = map.mapCenter
        val zoom = map.zoomLevelDouble

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putFloat(LAST_LATITUDE, center.latitude.toFloat())
            putFloat(LAST_LONGITUDE, center.longitude.toFloat())
            putFloat(LAST_ZOOM, zoom.toFloat())
            apply()
        }
    }

    private fun checkGpsAndRequestPermission() {
        if (!isGpsEnabled()) {
            promptEnableGps()
        } else {
            checkLocationPermission()
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun promptEnableGps() {
        AlertDialog.Builder(this)
            .setTitle("Enable GPS")
            .setMessage("GPS is required for this app to work. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation(true)
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation(false)
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun enableMyLocation(preciseLocation: Boolean) {
        if (ContextCompat.checkSelfPermission(
                this,
                if (preciseLocation) Manifest.permission.ACCESS_FINE_LOCATION
                else Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationProvider = GpsMyLocationProvider(this).apply {
                if (!preciseLocation) {
                    locationUpdateMinTime = 10000
                    locationUpdateMinDistance = 100f
                }
            }

            val myLocationOverlay = MyLocationNewOverlay(locationProvider, map)
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)

            // Get last known location immediately
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val lastKnownLocation = if (preciseLocation) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            // Use last known location if available
            lastKnownLocation?.let {
                val userLocation = GeoPoint(it.latitude, it.longitude)
                map.controller.animateTo(userLocation)
            }

            // Set up listener for location updates
            val locationListener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    val userLocation = GeoPoint(location.latitude, location.longitude)
                    map.controller.animateTo(userLocation)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (preciseLocation) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            } else {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10000L,
                    100f,
                    locationListener
                )
            }

            map.invalidate()
        }
    }

    private fun setupAddStoreButton() {
        binding.btnAddStore.setOnClickListener {
            isAddingStore = true
            binding.layoutConfirmCancel.visibility = View.VISIBLE
            Toast.makeText(this, "Tap on the map to place a new store", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMapClickListener() {
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (isAddingStore && p != null) {
                    newStoreLocation = p
                    addTemporaryMarker(p)
                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
        map.overlays.add(mapEventsOverlay)
    }

    private fun addTemporaryMarker(point: GeoPoint) {
        map.overlays.removeAll { it is Marker && it.id == "tempMarker" }
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.id = "tempMarker"
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun setupConfirmCancelButtons() {
        binding.btnConfirm.setOnClickListener {
            newStoreLocation?.let { location ->
                val intent = Intent(this, AddStoreActivity::class.java).apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                    putExtra("USER_ADDRESS", userAddress)
                }
                startActivity(intent)
            }
            resetAddStoreMode()
        }

        binding.btnCancel.setOnClickListener {
            resetAddStoreMode()
        }
    }

    private fun resetAddStoreMode() {
        isAddingStore = false
        newStoreLocation = null
        binding.layoutConfirmCancel.visibility = View.GONE
        map.overlays.removeAll { it is Marker && it.id == "tempMarker" }
        map.invalidate()
    }

    private fun fetchLocationsAndUpdateMap() {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/TNDpay/vendors/main/vendors.json")
                    .build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                val gson = Gson()
                val locationListType = object : TypeToken<List<LocationItem>>() {}.type
                val locations: List<LocationItem?> = gson.fromJson(responseData, locationListType)

                runOnUiThread {
                    locations.filterNotNull().forEach { location ->
                        val marker = Marker(map)
                        marker.position = GeoPoint(location.latitude, location.longitude)
                        marker.title = location.name
                        map.overlays.add(marker)
                    }
                    map.invalidate()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ExploreActivity", "Error fetching locations: ${e.message}")
            }
        }.start()
    }

    data class LocationItem(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    when {
                        grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                            when (permissions[0]) {
                                Manifest.permission.ACCESS_FINE_LOCATION -> {
                                    enableMyLocation(true)
                                }
                                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                                    enableMyLocation(false)
                                }
                            }
                        }
                        else -> {
                            Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        checkGpsAndRequestPermission()
    }

    override fun onPause() {
        super.onPause()
        saveMapPosition()
        map.onPause()
    }

    override fun onStop() {
        super.onStop()
        saveMapPosition()
    }
}