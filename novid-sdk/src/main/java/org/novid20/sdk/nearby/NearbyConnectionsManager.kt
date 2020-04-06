/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:23
 */

package org.novid20.sdk.nearby

import android.content.Context
import android.os.Handler
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import org.novid20.sdk.TECHNOLOGY_NEARBY_CONNECTIONS
import java.util.concurrent.TimeUnit

private val TAG: String = Logger.makeLogTag("NearbyConnectionsManager")

internal class NearbyConnectionsManager(private val sdk: NovidSdk, private val context: Context) {

    private var started = false

    private val handler = Handler()

    private var connectionsClient: ConnectionsClient? = null

    private val advertisingOptions = AdvertisingOptions.Builder()
        .setStrategy(Strategy.P2P_CLUSTER)
        .build()

    val checkDuration = TimeUnit.MINUTES.toMillis(1)

    private var durationChecker: Runnable = object : Runnable {
        override fun run() {
            try {
                persistCurrentEndpoints()
            } finally {
                // 100% guarantee that this always happens,
                // even if your update method throws an exception
                handler.postDelayed(this, checkDuration)
            }
        }
    }

    fun startRepeatingTask() {
        handler.postDelayed(durationChecker, checkDuration)
    }

    fun stopRepeatingTask() {
        handler.removeCallbacks(durationChecker)
    }

    fun start() {
        Logger.debug(TAG, "start")
        started = true

        val advertisingCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(
                endpointId: String,
                connectionInfo: ConnectionResolution
            ) {
                Logger.verbose(TAG, "onConnectionResult")
            }

            override fun onDisconnected(endpointId: String) {
                Logger.warn(TAG, "onDisconnected")
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                Logger.verbose(TAG, "onConnectionInitiated")
            }
        }

        val advertiseUserId = sdk.getConfig().userId ?: ""
        connectionsClient = Nearby.getConnectionsClient(context)
        connectionsClient?.startAdvertising(
            advertiseUserId,
            context.packageName,
            advertisingCallback,
            advertisingOptions
        )

        val discoveryCallback = object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                val userId = info.endpointName
                Logger.debug(TAG, "onEndpointFound: $endpointId - $userId")

                val millis = System.currentTimeMillis()
                val novidSdk = NovidSdk.getInstance()
                novidSdk.getRepository().contactDetected(userId, source = TECHNOLOGY_NEARBY_CONNECTIONS)
                endpointUserMap[endpointId] = userId
                endpointDiscoveryMap[endpointId] = millis

                startRepeatingTask()
            }

            override fun onEndpointLost(endpointId: String) {
                Logger.debug(TAG, "onEndpointLost: $endpointId")

                val userId = endpointUserMap[endpointId]
                val found = endpointDiscoveryMap[endpointId]
                if (userId != null && found != null) {

                    // Update the duration of the last discovery event
                    val current = System.currentTimeMillis()
                    val duration = current - found
                    val novidSdk = NovidSdk.getInstance()
                    novidSdk.getRepository().updateDuration(userId, found, duration)

                    endpointUserMap.remove(endpointId)
                    endpointDiscoveryMap.remove(endpointId)

                    if (endpointUserMap.isNullOrEmpty()) {
                        stopRepeatingTask()
                    }
                }
            }
        }
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(
                context.packageName,
                discoveryCallback,
                discoveryOptions
            )
    }

    val endpointDiscoveryMap = mutableMapOf<String, Long>()
    val endpointUserMap = mutableMapOf<String, String>()

    internal fun persistCurrentEndpoints() {

        val endpoints = endpointUserMap.keys
        for (endpointId in endpoints) {

            val userId = endpointUserMap[endpointId]
            val found = endpointDiscoveryMap[endpointId]
            if (userId != null && found != null) {
                // Update the duration of the last discovery event
                val current = System.currentTimeMillis()
                val duration = current - found
                val novidSdk = NovidSdk.getInstance()
                novidSdk.getRepository().updateDuration(userId, found, duration)
                Logger.debug(TAG, "Update duration for $userId")
            }
        }
    }

    fun stop() {
        Logger.debug(TAG, "stop")
        started = false

        connectionsClient?.stopAdvertising()
        connectionsClient?.stopDiscovery()
        connectionsClient = null

        endpointUserMap.clear()
        endpointDiscoveryMap.clear()
    }
}