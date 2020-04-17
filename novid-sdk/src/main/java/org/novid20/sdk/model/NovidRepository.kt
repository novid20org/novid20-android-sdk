/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:36
 */

package org.novid20.sdk.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

interface NovidRepository {

    suspend fun register(uid: String, authToken: String, deviceToken: String): Boolean

    fun updateDeviceToken(uid: String, deviceToken: String)

    fun contactDetected(
        userid: String,
        timestamp: Long = System.currentTimeMillis(),
        source: String? = null,
        rssi: Int? = null
    ): Boolean

    fun updateDuration(userid: String, timestamp: Long, duration: Long)

    suspend fun getContacts(since: Long): List<ContactEntry>

    fun getContactsContinuously(): LiveData<List<ContactEntry>>

    suspend fun reportInfection(sendLocations: Boolean = false): Result

    suspend fun verifyUser(phoneNumber: String): Result

    suspend fun verifyCode(phoneNumber: String, tan: String, sendLocations: Boolean = false): Result

    suspend fun getAlerts(): List<Alert>

    @WorkerThread
    fun syncAllAnalyticsData(): Boolean

    suspend fun saveEvent(name: String, value: String)

    fun deleteOutdatedContactsAndLocations()

    fun nukeData()
}