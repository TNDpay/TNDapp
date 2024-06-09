package com.example.tnd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.location.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tnd.databinding.ActivityExploreBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log


class ExploreActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityExploreBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Handle the permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Do the location-related task you need to do.
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                }
            } else {
                // Permission denied, Disable the functionality that depends on this permission.
                // You can show an explanation to the user, etc.
            }
        }
    }
    data class LocationItem(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )
    private fun fetchLocationsAndUpdateMap() {
        // Run network operations on a background thread
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/TNDpay/vendors/main/vendors.json")
                    .build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                Log.d("ExploreActivity", "Response Data: $responseData")
                // Parse JSON to a list of LocationItem
                val gson = Gson()
                val locationListType = object : TypeToken<List<LocationItem>>() {}.type
                val locations: List<LocationItem> = gson.fromJson(responseData, locationListType)

                // Update the map with the fetched locations
                runOnUiThread {
                    locations?.forEach { location ->
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                            Log.d("ExploreActivity", "Location added: ${it.name} at $latLng")
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ExploreActivity", "Error fetching locations: ${e.message}")

            }
        }.start()
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        fetchLocationsAndUpdateMap()
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations, this can be null.
                location?.let {
                    val userLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } ?: run {
                    val lastLocation = LatLng(40.359094404842565, 0.3996138427450273)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 15f))
                }
            }
        } else {
            // Request location permission, so that we can get the location of the device
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

}