package com.music.bitchord.data.firebase

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Handles device location acquisition with high accuracy,
 * reverse geocoding to city/country, and forwarding to FirestoreManager.
 */
object LocationTracker {

    private const val TAG = "LocationTracker"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): FirestoreManager.LocationData? {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return withContext(Dispatchers.IO) {
            val location = tryPlayServicesLocation(context) ?: trySystemLocation(context)
            if (location == null) {
                Log.w(TAG, "Unable to obtain location from any provider")
                return@withContext null
            }

            val address = geocodeCoordinates(context, location.latitude, location.longitude)
            val locationData = FirestoreManager.LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                city = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Unknown City",
                state = address?.adminArea.orEmpty(),
                country = address?.countryName.orEmpty(),
                timestamp = System.currentTimeMillis(),
            )

            FirestoreManager.updateLocation(locationData)
            locationData
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryPlayServicesLocation(context: Context): Location? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine { continuation ->
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (continuation.isActive) continuation.resume(loc)
                        } else {
                            client.lastLocation.addOnSuccessListener { last ->
                                if (continuation.isActive) continuation.resume(last)
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Play services location failed: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun trySystemLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) return loc
                }
            }
            null
        } catch (e: Throwable) {
            Log.w(TAG, "System location manager failed: ${e.message}")
            null
        }
    }

    private fun geocodeCoordinates(context: Context, lat: Double, lon: Double): Address? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(lat, lon, 1)
            results?.firstOrNull()
        } catch (e: Throwable) {
            Log.w(TAG, "Reverse geocoding failed: ${e.message}")
            null
        }
    }
}
