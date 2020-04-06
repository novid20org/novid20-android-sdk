/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:53
 */

package org.novid20.sdk.model

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.novid20.sdk.NovidSdkImpl

internal class LocationRepositoryImpl(private val sdk: NovidSdkImpl) : LocationRepository, CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.Main + job

    override fun storeLocation(location: Location) {

        launch(Dispatchers.IO) {
            val locationDao = sdk.database.locationDao()
            locationDao.insert(
                LocationEntryEntity(
                    time = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )
            )
        }
    }
}