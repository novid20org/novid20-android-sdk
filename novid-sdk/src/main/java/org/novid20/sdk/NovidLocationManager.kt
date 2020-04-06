/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import org.novid20.sdk.model.LocationRepository
import org.novid20.sdk.model.LocationRepositoryImpl
import java.util.concurrent.TimeUnit

internal class NovidLocationManager(
    private val sdk: NovidSdkImpl,
    private val context: Context
) {

    companion object {
        private const val TAG: String = "NovidLocationManager"
    }

    private var lastRequest: Long = 0;

    fun requestAndStoreCurrentLocation() {

        val current = System.currentTimeMillis()
        val diff = current - lastRequest
        // Request at max every 5 minutes a location (battery)
        if (diff > TimeUnit.MINUTES.toMillis(5)) {
            lastRequest = current

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val checkSelfPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    SingleLocationListener(LocationRepositoryImpl(sdk)),
                    Looper.myLooper()
                )
            }
        }
    }

    inner class SingleLocationListener(private val repo: LocationRepository) : LocationListener {

        override fun onLocationChanged(location: Location) {
            Logger.debug(TAG, "onLocationChanged")

            repo.storeLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Logger.verbose(TAG, "onStatusChanged")
        }

        override fun onProviderEnabled(provider: String?) {
            Logger.verbose(TAG, "onProviderEnabled")
        }

        override fun onProviderDisabled(provider: String?) {
            Logger.verbose(TAG, "onProviderDisabled")
        }
    }
}