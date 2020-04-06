/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:21
 */

package org.novid20.sdk.model

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.novid20.sdk.BuildConfigHelper
import org.novid20.sdk.Logger
import org.novid20.sdk.NOTIFICATION_CHANNEL
import org.novid20.sdk.NovidSDKAnalytics
import org.novid20.sdk.NovidSdkImpl
import org.novid20.sdk.R
import org.novid20.sdk.api.NovidClient
import org.novid20.sdk.api.NovidClientImpl
import org.novid20.sdk.api.models.AnalyticsEvent
import org.novid20.sdk.api.models.AnalyticsRequest
import org.novid20.sdk.api.models.AnalyticsState
import org.novid20.sdk.api.models.ApiContact
import org.novid20.sdk.api.models.ApiLocation
import org.novid20.sdk.api.models.ApiResponse
import org.novid20.sdk.core.Dispatchers
import org.novid20.sdk.core.DispatchersImpl
import java.util.concurrent.TimeUnit


private const val TAG: String = "NovidRepositoryImpl"

internal class NovidRepositoryImpl(
    private val novidSdk: NovidSdkImpl,
    private val context: Context,
    private val dispatchers: Dispatchers = DispatchersImpl()
) : NovidRepository, CoroutineScope {

    companion object {
        internal const val BT_NAME_PREFIX = "nov20-"

        const val ERROR_CODE_OTHER = -1
    }

    private val job = Job()
    override val coroutineContext = dispatchers.main + job

    private val client: NovidClient by lazy {
        NovidClientImpl(
            context, novidSdk.authTokenLoader
                ?: throw IllegalArgumentException("You have to set an AuthTokenLoader before the NovidSdk can be used.")
        )
    }

    override fun register(uid: String, authToken: String, deviceToken: String) {
        Logger.debug(TAG, "Register user: $uid")
        val config = novidSdk.config

        val registered = config.registered

        if (!registered) {
            GlobalScope.launch {
                try {
                    config.deviceToken = deviceToken
                    config.authToken = authToken
                    config.uid = uid

                    val userId = client.registerUser(deviceToken)
                    if (!userId.isNullOrBlank()) {
                        config.userId = userId
                        config.registered = true

                        novidSdk.analytics.sendEvent(FirebaseAnalytics.Event.SIGN_UP)
                    }
                } catch (t: Throwable) {
                    Logger.error(TAG, t.message, t)
                }
            }
        }
    }

    override fun updateDeviceToken(uid: String, deviceToken: String) {
        GlobalScope.launch {
            try {
                client.updateDeviceToken(deviceToken)
            } catch (t: Throwable) {
                // Shoot and forget.. not special handling for that, just don't crash
                Logger.error(TAG, t.message, t)
            }
        }
    }

    private val detectedUserCache = mutableSetOf<String>()

    private val recentFoundMap = mutableMapOf<String, Long>()

    override fun contactDetected(userid: String, timestamp: Long, source: String?, rssi: Int?): Boolean {

        if (!userid.startsWith(BT_NAME_PREFIX)) {
            // Ignore non nov values
            Logger.warn(TAG, "Found contact but it's name does not start with \"$BT_NAME_PREFIX\" ($userid)")
            return false
        }

        if (userid.length > 30) {
            Logger.warn(TAG, "Found contact but was too long ($userid)")
            return false
        }

        val current = System.currentTimeMillis()
        val foundKey = userid + source
        val lastFound = if (recentFoundMap.containsKey(foundKey)) recentFoundMap[foundKey] else 0
        val diff = current - (lastFound ?: 0)
        if (diff < TimeUnit.SECONDS.toMillis(30)) {
            Logger.verbose(TAG, "Found $userid already ${diff}ms ago")
            return true // valid contact, but just not added
        }

        recentFoundMap[foundKey] = current

        Logger.debug(TAG, "contactDetected: $userid $source")

        novidSdk.locationManager.requestAndStoreCurrentLocation()

        launch(dispatchers.io) {
            val contactDao = novidSdk.database.contactDao()

            if (!detectedUserCache.contains(userid)) {
                detectedUserCache.add(userid)
                // Potential new user, check in db
                val contacts = contactDao.getContactsWith(userid)
                if (contacts.isNullOrEmpty()) {
                    newContactDetected(userid, timestamp, source)
                }
            }

            // Insert it into db
            val entity = ContactEntryEntity(
                user = userid,
                time = timestamp,
                duration = null,
                source = source,
                rssi = rssi
            )
            contactDao.insert(entity)
        }
        return false
    }

    private fun newContactDetected(userid: String, timestamp: Long, source: String?) {
        Logger.debug(TAG, "new contact detected: $userid")

        if (BuildConfigHelper.DEBUG) {
            val contentTitle = context.getString(R.string.notification_title)
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                .setContentTitle(contentTitle)
                .setShowWhen(true)
                .setWhen(timestamp)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentText("New contact detected: $userid ($source)")
                .setLights(Color.RED, 3000, 3000)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setChannelId(NOTIFICATION_CHANNEL)

            val notification = builder.build()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(userid.hashCode(), notification)

            novidSdk.analytics.sendEvent(NovidSDKAnalytics.EVENT_CONTACT, userid)
        }
    }

    override fun updateDuration(userid: String, timestamp: Long, duration: Long) {
        GlobalScope.launch {
            val contactDao = novidSdk.database.contactDao()
            val updated = contactDao.updateDuration(timestamp, userid, duration)
            Logger.verbose(TAG, "Update duration result: $updated")
        }
    }

    override suspend fun reportInfection(sendLocations: Boolean): Result {
        try {
            val current = System.currentTimeMillis()
            val since = current - TimeUnit.DAYS.toMillis(3)
            val recentContacts = getContacts(since)

            val contacts = recentContacts.map {
                ApiContact(
                    userId = it.user,
                    timestamp = it.time,
                    duration = it.duration,
                    distance = it.distance,
                    rssi = it.rssi,
                    background = it.background
                )
            }
            var locations = emptyList<ApiLocation>()
            if (sendLocations) {
                val locationDao = novidSdk.database.locationDao()
                locations = locationDao.getAll().map {
                    ApiLocation(
                        timestamp = it.time,
                        lat = it.latitude,
                        lng = it.longitude
                    )
                }
            }
            novidSdk.config.infectionReported = true
            return client.reportInfection(contacts, locations).toResult()
        } catch (error: Throwable) {
            Logger.error(TAG, "Report infections call failed: ${error.message}", error)
            return ApiResponse(status = ERROR_CODE_OTHER, message = error.message).toResult()
        }
    }

    override suspend fun getContacts(since: Long): List<ContactEntry> {
        val contactDao = novidSdk.database.contactDao()
        val userContacts = contactDao.getAllData(since)
        return userContacts.map {
            val entry = it.toContactEntry()
            entry
        }
    }

    override fun getContactsContinuously(): LiveData<List<ContactEntry>> {
        return Transformations
            .map(novidSdk.database.contactDao().getAllContinuously()) {
                it.map(ContactEntryEntity::toContactEntry)
            }
    }

    override suspend fun verifyUser(phoneNumber: String): Result {
        return try {
            client.verifyUser(phoneNumber).toResult()
        } catch (error: Throwable) {
            Logger.error(TAG, "Verify user call failed: ${error.message}", error)
            ApiResponse(status = ERROR_CODE_OTHER, message = error.message).toResult()
        }
    }

    override suspend fun verifyCode(phoneNumber: String, tan: String, sendLocations: Boolean): Result {
        return try {
            val verifyResponse = client.verifyCode(tan)
            if (verifyResponse.isSuccessful) {
                reportInfection(sendLocations)
            } else {
                verifyResponse.toResult()
            }
        } catch (error: Throwable) {
            Logger.error(TAG, "Verify otp code failed: ${error.message}", error)
            ApiResponse(status = ERROR_CODE_OTHER, message = error.message).toResult()
        }
    }

    override suspend fun getAlerts(): List<Alert> {
        val alerts = mutableListOf<Alert>()
        try {
            val config = novidSdk.config
            val status = client.getStatus()
            val alertCount = status.infectedContactCount
            config.infectionConfirmed = status.infected
            repeat(alertCount) { alerts.add(Alert()) }
        } catch (t: Throwable) {
            Logger.error(TAG, t.message, t)
        }
        return alerts
    }

    override fun syncAllAnalyticsData(): Boolean {
        val analyticsDao = novidSdk.database.analyticsDao()
        val deviceDataProvider = novidSdk.deviceDataProvider ?: throw IllegalStateException(
            "NovidSdk#deviceDataProvider has to be " +
                    "initialized before syncAllAnalyticsData can be used."
        )

        // Sync all events
        val events = analyticsDao.getAllEvents()
        val request = AnalyticsRequest(
            timestamp = System.currentTimeMillis(),
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            appVersion = deviceDataProvider.getAppVersion(),
            buildNumber = deviceDataProvider.getBuildNumber(),
            deviceModel = Build.BRAND + " (" + Build.MODEL + ")",
            states = deviceDataProvider.getStates().map { AnalyticsState(it.name, it.value) },
            events = events.map { AnalyticsEvent(it.id, it.name, it.value, it.timestamp) }
        )

        val responseSuccessful = client.submitAnalytics(request)
        if (responseSuccessful) {
            // Sync was successful, we can delete synced events now
            analyticsDao.nukeEventsTable()
        }

        return responseSuccessful
    }

    override suspend fun saveEvent(name: String, value: String) {
        val event = AnalyticsEventEntryEntity(
            name = name,
            value = value,
            timestamp = System.currentTimeMillis()
        )
        novidSdk.database.analyticsDao().insertAll(event)
    }

    override fun nukeData() {
        novidSdk.database.contactDao().nukeTable()
        novidSdk.database.locationDao().nukeTable()
        novidSdk.database.analyticsDao().nukeEventsTable()
    }
}