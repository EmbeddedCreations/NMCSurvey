package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap
    private lateinit var locationInfoTextView: TextView
    private lateinit var bestLocationInfoTextView: TextView // Added TextView for best location
    private lateinit var getLocationButton: Button
    private lateinit var progressBar: ProgressBar
    private var currentMarker: Marker? = null
    private var isFirstLocationUpdate = true

    // Variables to store current real-time location data
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAccuracy = 0.0f

    // Variables to store best (most accurate) location data
    private var bestLatitude = 0.0
    private var bestLongitude = 0.0
    private var bestAccuracy = Float.MAX_VALUE // Initialize with a large value (worst accuracy)

    private lateinit var locationRequest: LocationRequest

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_SETTINGS_REQUEST_CODE = 2
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            checkLocationPermission()
            // Hide the progress bar when the map is ready
            progressBar.visibility = ProgressBar.VISIBLE
        }

        // Initialize UI elements
        locationInfoTextView = findViewById(R.id.location_info)
        bestLocationInfoTextView = findViewById(R.id.best_location_info) // Initialize the TextView for best location
        getLocationButton = findViewById(R.id.get_location_button)
        progressBar = findViewById(R.id.map_loading_spinner)

        // Initialize LocationRequest
        locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1 second
            fastestInterval = 500 // 0.5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Set up button click listener
        getLocationButton.setOnClickListener {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                // Pass the stored location data back to UploadScreen
                val returnIntent = Intent()
                returnIntent.putExtra("latitude", bestLatitude)
                returnIntent.putExtra("longitude", bestLongitude)
                returnIntent.putExtra("accuracy", bestAccuracy)
                setResult(Activity.RESULT_OK, returnIntent)
                finish() // Close MainActivity and return to UploadScreen

                // Clear the best location after button click
                clearBestLocation()
            } else {
                Log.d(TAG, "No valid location data available")
            }
        }

        // Initialize LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentAccuracy = location.accuracy

                    Log.d(
                        TAG,
                        "Location updated: Lat: $currentLatitude, Lng: $currentLongitude, Accuracy: $currentAccuracy"
                    )

                    updateMapLocation(currentLatitude, currentLongitude)
                    updateLocationInfo(currentLatitude, currentLongitude, currentAccuracy)
                    progressBar.visibility = ProgressBar.GONE

                    // Check if this location is more accurate than the best recorded one
                    if (currentAccuracy < bestAccuracy) {
                        bestLatitude = currentLatitude
                        bestLongitude = currentLongitude
                        bestAccuracy = currentAccuracy

                        updateBestLocationInfo(bestLatitude, bestLongitude, bestAccuracy)
                    }
                } else {
                    Log.d(TAG, "Location accuracy is not within the desired threshold: ${location?.accuracy}")
                }
            }
        }

        checkLocationPermission()
    }

    private fun updateLocationInfo(latitude: Double, longitude: Double, accuracy: Float) {
        val locationInfo = "Lat: %.3f\nLng: %.3f\nAccuracy: %.2f mtr".format(latitude, longitude, accuracy)
        locationInfoTextView.text = locationInfo
    }

    // Method to update the best location information on the screen
    private fun updateBestLocationInfo(latitude: Double, longitude: Double, accuracy: Float) {
        val bestLocationInfo = "B-Lat: %.3f\nB-Lng: %.3f\nB-Accuracy: %.2f mtr".format(latitude, longitude, accuracy)
        bestLocationInfoTextView.text = bestLocationInfo
    }


    // Method to clear the best location data once the button is clicked
    private fun clearBestLocation() {
        bestLatitude = 0.0
        bestLongitude = 0.0
        bestAccuracy = Float.MAX_VALUE // Reset to default value
        bestLocationInfoTextView.text = "Best location cleared"
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, set up location services
            checkLocationSettings()
        }
    }

    private fun startLocationUpdates() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val userLocation = LatLng(latitude, longitude)

        if (currentMarker == null) {
            // If it's the first location update, set the marker and move camera
            currentMarker = mMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
        } else {
            // Update marker position without changing zoom level
            currentMarker?.position = userLocation
            if (isFirstLocationUpdate) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, mMap.cameraPosition.zoom))
                isFirstLocationUpdate = false
            }
        }
    }

    private fun checkLocationSettings() {
        val settingsClient = LocationServices.getSettingsClient(this)
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener(this) {
            startLocationUpdates()
        }

        task.addOnFailureListener(this) { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, LOCATION_SETTINGS_REQUEST_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings() // Permission granted, check location settings and start location updates
            } else {
                // Permission denied, handle accordingly
                Log.d(TAG, "Location permission denied")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkLocationPermission() // Check permission and start location updates if granted
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates() // Stop location updates to avoid charges and save battery
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates() // Stop location updates when the activity is paused
    }
}
