package com.example.tracking

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class DeviceTracker(private val context: Context) {

    fun getCurrentDeviceCoordinates(): Pair<Double, Double> {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled) {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    return Pair(loc.latitude, loc.longitude)
                }
            }
            if (isNetworkEnabled) {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    return Pair(loc.latitude, loc.longitude)
                }
            }
        } catch (e: SecurityException) {
            Log.e("DeviceTracker", "Missing ACCESS_FINE_LOCATION permission, returning simulated Algiers GPS coords", e)
        } catch (e: Exception) {
            Log.e("DeviceTracker", "Failed acquiring coordinates", e)
        }

        // Default Algiers City Center fallback coordinates if GPS hardware doesn't boot
        return Pair(36.7538, 3.0588)
    }
}
