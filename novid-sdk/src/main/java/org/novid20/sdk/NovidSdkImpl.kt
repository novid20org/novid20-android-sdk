/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:10
 */

package org.novid20.sdk

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.novid20.sdk.NovidSDKAnalytics.Companion.EVENT_DETECTION_OFF
import org.novid20.sdk.NovidSDKAnalytics.Companion.EVENT_DETECTION_ON
import org.novid20.sdk.NovidSDKAnalytics.Companion.EVENT_SERVICE_OFF
import org.novid20.sdk.NovidSDKAnalytics.Companion.EVENT_SERVICE_ON
import org.novid20.sdk.activity.NovidActivityManager
import org.novid20.sdk.analytics.Analytics
import org.novid20.sdk.analytics.AnalyticsImpl
import org.novid20.sdk.analytics.AnalyticsSyncWorker
import org.novid20.sdk.analytics.DeviceDataProvider
import org.novid20.sdk.api.AuthTokenLoader
import org.novid20.sdk.ble.BleBluetoothManager
import org.novid20.sdk.ble.BleDetectionConfig
import org.novid20.sdk.ble.BleServerManager
import org.novid20.sdk.ble.NovidBeaconManager
import org.novid20.sdk.core.CountdownThreadHandler
import org.novid20.sdk.model.NovidDatabase
import org.novid20.sdk.model.NovidRepository
import org.novid20.sdk.model.NovidRepositoryImpl
import org.novid20.sdk.nearby.NearbyConnectionsManager
import org.novid20.sdk.nearby.NearbyMessagesManager
import java.util.concurrent.TimeUnit


private val TAG: String = Logger.makeLogTag("NovidSdkImpl")

internal const val TIMEOUT_INITIAL = 10L // minutes
internal const val TIMEOUT_FOREGROUND = 10L // minutes
internal const val TIMEOUT_BACKGROUND = 10L // minutes
internal const val TIMEOUT_PUSH = 10L // minutes
internal const val TIMEOUT_STILL = 10L // minutes

internal const val TECHNOLOGY_BLUETOOTH = "bluetooth"
internal const val TECHNOLOGY_BLE_NAME = "ble discovery"
internal const val TECHNOLOGY_BLE_CLIENT = "ble client"
internal const val TECHNOLOGY_BLE_SERVER = "ble server"
internal const val TECHNOLOGY_BLE_CACHE = "ble cache"
internal const val TECHNOLOGY_BEACON = "ble beacon"
internal const val TECHNOLOGY_NEARBY_CONNECTIONS = "nearby connections"
internal const val TECHNOLOGY_NEARBY_MESSAGES = "nearby messages"

internal class NovidSdkImpl internal constructor(
    val context: Context,
    private val sdkAccessToken: String,
    override val bundleId: String,
    override val bleDetectionConfig: BleDetectionConfig,
    override val deviceDataProvider: DeviceDataProvider
) : NovidSdk {

    override var authTokenLoader: AuthTokenLoader? = null
    var novidRepository: NovidRepository = NovidRepositoryImpl(this, bundleId, bleDetectionConfig, context)

    override val analytics: Analytics = AnalyticsImpl(novidRepository)

    private val bluetoothManager = BleBluetoothManager(context, bleDetectionConfig, novidRepository)
    private val bleServerManager = BleServerManager(this, bleDetectionConfig, context)

    private val activityManager = NovidActivityManager(context)
    internal val locationManager = NovidLocationManager(this, context)
    private val nearbyMessageManager = NearbyMessagesManager(this)
    private val nearbyConnectionManager = NearbyConnectionsManager(this, context)

    private val beaconManager = NovidBeaconManager(context, bleDetectionConfig) {
        novidRepository.contactDetected(
            userid = it.userId,
            source = TECHNOLOGY_BEACON,
            rssi = it.rssi
        )
    }

    internal val config = NovidConfig(context).apply {
        accessToken = sdkAccessToken
    }

    inner class BluetoothChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (state == BluetoothAdapter.STATE_OFF) {
                Logger.debug(TAG, "STATE_OFF")
            } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                Logger.debug(TAG, "STATE_TURNING_OFF")

                bleServerManager.stop()
                bluetoothManager.stopDiscovery()

            } else if (state == BluetoothAdapter.STATE_TURNING_ON) {
                Logger.debug(TAG, "STATE_TURNING_ON")
            } else if (state == BluetoothAdapter.STATE_ON) {
                Logger.debug(TAG, "STATE_ON")

                if (detectionRunning) {
                    bleServerManager.start()
                    bluetoothManager.startDiscovery()
                }
            }
        }
    }

    private val bluetoothChangeReceiver = BluetoothChangeReceiver()

    private val turnDetectionOffHandler: CountdownThreadHandler =
        CountdownThreadHandler(
            Runnable {
                stopDetection()
            }
        )

    internal val database = NovidDatabase.getDatabase(context)

    init {
        val configUserId: String? = config.uid
        Logger.debug(TAG, "Initializing NoVidSdk as $configUserId")

        if (config.onboarded && config.registered && config.enabled) {
            // Start automatic if enabled
            startService()
        } else {
            Logger.debug(
                TAG, "Not starting service because user " +
                    "is not registered (${config.registered}), " +
                    "not onboarded (${config.onboarded}) " +
                    "or is not enabled (${config.enabled})."
            )
        }
    }

    private var serviceRunning = false
    private var detectionRunning = false

    /**
     * Start the general functionality
     */
    override fun startService() {
        if (!hasValidInstallation()) {
            Logger.warn(TAG, "cannnot start service - user has no valid registration")
        }

        Logger.debug(TAG, "startService")
        serviceRunning = true
        startNotification()
        config.enabled = true

        // Requires activity permission
        activityManager.start()

        // Start scanning for beacons if required - not yet used
        //beaconManager.startScanning()

        // Schedule background analytics events sync worker
        AnalyticsSyncWorker.schedule(context)

        analytics.sendEvent(EVENT_SERVICE_ON)

        if (config.onboarded && config.enabled) {
            // Start automatic if enabled
            startDetection()
            val duration = TimeUnit.MINUTES.toMillis(TIMEOUT_INITIAL)
            stopDetectionDelayed(duration)
        }

        // Delete outdated contacts
        novidRepository.deleteOutdatedContactsAndLocations()
    }

    override fun stopService() {
        Logger.debug(TAG, "stopService")
        serviceRunning = false
        config.enabled = false
        stopNotification()

        analytics.sendEvent(EVENT_SERVICE_OFF)

        stopDetection()
    }

    /**
     * Detection is the actual part where we scan actively.
     * During idle/night this detection is off, but service is still marked as "running"
     */
    override fun startDetection() {
        // can only start detection if the service is running
        if (serviceRunning && !detectionRunning) {
            Logger.debug(TAG, "startDetection")
            analytics.sendEvent(EVENT_DETECTION_ON)

            val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bluetoothChangeReceiver, bluetoothFilter)

            detectionRunning = true

            // Nearby messages are triggered by foreground
            nearbyConnectionManager.start()

            bleServerManager.start()
            bluetoothManager.startDiscovery()

            //beaconManager.startAdvertising()
            locationManager.requestAndStoreCurrentLocation()
        }
    }

    override fun stopDetectionDelayed(duration: Long) {
        // Extend the latest turn of task with at least the given duration
        turnDetectionOffHandler.extend(duration)
    }

    private fun stopDetection() {
        Logger.debug(TAG, "stopDetection")
        analytics.sendEvent(EVENT_DETECTION_OFF)

        try {
            context.unregisterReceiver(bluetoothChangeReceiver)
        } catch (ie: IllegalArgumentException) {
            /**
             * Fatal Exception: java.lang.IllegalArgumentException
             * Receiver not registered: org.novid20.sdk.NovidSdkImpl$BluetoothChangeReceiver@3ecf1b
             * org.novid20.sdk.NovidSdkImpl.stopDetectio
             */
            Logger.error(TAG, ie.message, ie)
        }

        detectionRunning = false
        nearbyMessageManager.stop()
        nearbyConnectionManager.stop()

        beaconManager.stopScanning()
        bleServerManager.stop()
        bluetoothManager.stopDiscovery()
    }

    private fun hasValidInstallation(): Boolean {
        return config.registered && config.onboarded && !config.userId.isNullOrBlank()
    }

    override fun isServiceRunning() = serviceRunning

    override fun hasErrors(): Boolean {
        val checkLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )

        val checkActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        val hasLocationPermission = checkLocationPermission == PackageManager.PERMISSION_GRANTED
        val hasActivityPermission = checkActivityPermission == PackageManager.PERMISSION_GRANTED
        return !hasLocationPermission || !hasActivityPermission
    }

    override fun messageReceived(bundle: Bundle) {
        // Start
        startDetection()
        val duration = TimeUnit.MINUTES.toMillis(TIMEOUT_PUSH)
        stopDetectionDelayed(duration)
    }

    override fun getConfig() = config

    private fun startNotification() = NotificationService.start(context)

    private fun stopNotification() = NotificationService.stop(context)

    /**
     * The entry point to Google Play Services.
     */
    private var googleApiClient: GoogleApiClient? = null

    override fun configure(activity: FragmentActivity) {
        buildGoogleApiClient(activity)
    }

    /**
     * TODO: Remove usage of deprecated APIs
     */
    private fun buildGoogleApiClient(activity: FragmentActivity) {
        if (googleApiClient != null) {
            Logger.debug(TAG, "Not building GoogleApiClient because it has already been built.")
            return
        }
        googleApiClient = GoogleApiClient.Builder(context)
            .addApi(Nearby.MESSAGES_API)
            .addApi(Nearby.CONNECTIONS_API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                    Logger.debug(TAG, "GoogleApi connected")
                    if (config.onboarded && config.enabled) {
                        startService()
                    }
                }

                override fun onConnectionSuspended(errorCode: Int) {
                    Logger.debug(TAG, "Connection suspended. Error code: $errorCode")
                }
            })
            .enableAutoManage(activity) { connectionResult ->
                Logger.warn(
                    TAG,
                    "Exception while connecting to Google Play services: " + connectionResult.errorMessage
                )
            }
            .build()
    }

    override fun removeData() {
        GlobalScope.launch {
            novidRepository.nukeData()
        }
    }
}
