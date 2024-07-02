package com.example.tnd

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.preference.PreferenceManager
import android.util.Log
import com.example.tnd.databinding.ActivityExploreBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding
    private lateinit var map: MapView

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(15.0)

        checkLocationPermission()
        fetchLocationsAndUpdateMap()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            enableMyLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    enableMyLocation()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                }
                return
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Create and add the location overlay
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)

            // Zoom to the user's location
            val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
            val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)

            lastKnownLocation?.let {
                val userLocation = GeoPoint(it.latitude, it.longitude)
                map.controller.setCenter(userLocation)
            } ?: run {
                val defaultLocation = GeoPoint(40.359094404842565, 0.3996138427450273)
                map.controller.setCenter(defaultLocation)
            }

            map.controller.setZoom(15.0)
            map.invalidate() // Refresh the map
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    data class LocationItem(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    private fun fetchLocationsAndUpdateMap() {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/TNDpay/vendors/main/vendors.json")
                    .build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                Log.d("ExploreActivity", "Response Data: $responseData")

                val gson = Gson()
                val locationListType = object : TypeToken<List<LocationItem>>() {}.type
                val locations: List<LocationItem?> = gson.fromJson(responseData, locationListType)

                runOnUiThread {
                    locations.filterNotNull().forEach { location ->
                        val marker = Marker(map)
                        marker.position = GeoPoint(location.latitude, location.longitude)
                        marker.title = location.name
                        map.overlays.add(marker)
                        Log.d("ExploreActivity", "Location added: ${location.name} at ${marker.position}")
                    }
                    map.invalidate()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ExploreActivity", "Error fetching locations: ${e.message}")
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}