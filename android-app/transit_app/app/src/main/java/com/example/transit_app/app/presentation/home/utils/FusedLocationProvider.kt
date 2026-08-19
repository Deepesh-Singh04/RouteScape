package com.example.transit_app.app.presentation.home.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

class FusedLocationProvider(context: Context) : IMyLocationProvider {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var myLocationConsumer: IMyLocationConsumer? = null
    private var lastKnownLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                lastKnownLocation = location
                myLocationConsumer?.onLocationChanged(location, this@FusedLocationProvider)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        this.myLocationConsumer = myLocationConsumer

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateDistanceMeters(2f)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastKnownLocation = location
                this.myLocationConsumer?.onLocationChanged(location, this)
            }
        }

        return true
    }

    override fun stopLocationProvider() {
        myLocationConsumer = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun getLastKnownLocation(): Location? {
        return lastKnownLocation
    }

    override fun destroy() {
        stopLocationProvider()
    }
}