/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:50
 */

package org.novid20.sdk.ble

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BuildConfig
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.novid20.sdk.Logger

/**
 * Note: If a user is next to a beacon then [detectionCallback] will be called for every
 * scan detection and scans can happen as often as every 2 seconds. This means that if
 * this runs for 24 hours then [detectionCallback] can potentially be called 43,200 times.
 *
 * TODO: Evaluate if we want some scan throttling
 * TODO: Evaluate if we want to add [org.altbeacon.beacon.powersave.BackgroundPowerSaver]
 */
internal class NovidBeaconManager(
    private val context: Context,
    private val detectionCallback: (NovidBeacon) -> Unit
) : BeaconConsumer, MonitorNotifier, RangeNotifier {

    companion object {
        private const val TAG: String = "NovidBeaconManager"
        private const val LAYOUT_IBEACON = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

        /** Only beacons with this UUID will be found */
        private const val NOVID_UUID = "4ac8aff8-ed66-4500-af5b-d99ad69ac1ce"

        /** This is the prefix of the user id */
        private const val NOVID_PREFIX = "nov20-"
    }

    private val beaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)

    /**
     * The region id is an internal value to identify this region.
     * The null values for id1, id2 and id3 indicate a wildcard and will match any value.
     */
    private val region = Region("NovidRegion", Identifier.parse(NOVID_UUID), null, null)

    init {
        BeaconManager.setDebug(BuildConfig.DEBUG)

        beaconManager.beaconParsers.apply {
            // TODO: Do we want to scan for all beacon layouts?
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
            BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT)
            BeaconParser().setBeaconLayout(LAYOUT_IBEACON)
        }
    }

    /**
     * Call this to bind this [BeaconConsumer] to the [BeaconManager] and
     * actively start scanning. Any found [NovidBeacon] will be passed to [detectionCallback].
     */
    fun startScanning() {
        Logger.debug(TAG, "Binding beacon manager and starting scanning ASAP.")
        beaconManager.bind(this)
    }

    /**
     * Call this to stop all scanning activity and to stop receiving any [NovidBeacon]s in
     * your [detectionCallback].
     */
    fun stopScanning() {
        Logger.debug(TAG, "Unbinding beacon manager and stopping scanning.")
        reset()
        beaconManager.unbind(this)
    }

    override fun getApplicationContext(): Context = context.applicationContext

    override fun unbindService(connection: ServiceConnection) {
        Logger.debug(TAG, "unbindService")
        context.applicationContext.unbindService(connection)
    }

    override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int): Boolean {
        Logger.debug(TAG, "bindService")
        return context.applicationContext.bindService(intent, connection, flags)
    }

    override fun onBeaconServiceConnect() {
        Logger.debug(TAG, "Beacon service connected!")
        reset()

        Logger.debug(TAG, "Adding new notifiers.")
        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)

        Logger.debug(TAG, "Starting monitoring and ranging")
        startMonitoringAndRanging()
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        Logger.debug(TAG, "didDetermineStateForRegion: $state")
    }

    /**
     * Currently not used by us.
     */
    override fun didEnterRegion(region: Region) {
        Logger.debug(TAG, "Beacon seen for the first time.")
    }

    /**
     * Currently not used by us.
     */
    override fun didExitRegion(region: Region) {
        Logger.debug(TAG, "Beacon lost.")
    }

    /**
     * This will be called every 2 seconds when actively scanning. When no beacons are
     * found then the [beacons] array is empty.
     */
    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>, region: Region) {
        Logger.verbose(TAG, "Found ${beacons.size} beacons.")
        beacons.forEach { beacon ->
            Logger.debug(
                TAG, "Found a beacon: " +
                    "distance: " + (beacon.distance * 100) + "cm, " +
                    "toString(): " + beacon.toString()
            )

            val novidBeacon = beacon.toNovidBeacon()
            if (novidBeacon != null) {
                Logger.debug(TAG, "The found beacon is a valid Novid beacon with uuid: ${novidBeacon.userId}")
                detectionCallback(novidBeacon)
            } else {
                Logger.debug(TAG, "The found beacon is NOT a valid Novid beacon.")
            }
        }
    }

    /**
     * This function stop all scanning activity and removes all notifiers.
     */
    private fun reset() {
        Logger.debug(TAG, "Resetting monitoring and ranging state.")
        beaconManager.apply {
            stopMonitoringBeaconsInRegion(region)
            stopRangingBeaconsInRegion(region)
            removeAllMonitorNotifiers()
            removeAllRangeNotifiers()
        }
    }

    /**
     * Starts monitoring and ranging.
     * No error is thrown if one or both of them fail.
     */
    private fun startMonitoringAndRanging() {
        beaconManager.runCatching { startRangingBeaconsInRegion(region) }
            .onSuccess { Logger.debug(TAG, "Ranging in region $region started.") }
            .onFailure { Logger.error(TAG, "Failed to start ranging beacons in region.") }
        beaconManager.runCatching { startMonitoringBeaconsInRegion(region) }
            .onSuccess { Logger.debug(TAG, "Monitoring in region $region started.") }
            .onFailure { Logger.error(TAG, "Failed to start monitoring beacons in region.") }

    }

    /**
     * This extension function tries to
     * parse a [Beacon] to a custom [NovidBeacon] if this
     * beacon matches the novid format. Otherwise null will
     * be returned.
     */
    private fun Beacon.toNovidBeacon(): NovidBeacon? {
        val majorValue = id2.toInt()
        val minorValue = id3.toInt()

        if (majorValue == 0 || minorValue == 0) {
            Logger.warn(
                TAG, "Found beacon with major ($majorValue) or minor ($minorValue) " +
                        "value of 0 which is not valid."
            )
            return null
        }
        return NovidBeacon(
            userId = novidUidTransformer(id2, id3),
            rssi = rssi
        )
    }

    /**
     * This function creates a novid user id based on
     * the major and minor values of a [Beacon].
     */
    private fun novidUidTransformer(major: Identifier, minor: Identifier): String {
        return NOVID_PREFIX + major.toInt().toString() + minor.toInt().toString()
    }

    data class NovidBeacon(val userId: String, val rssi: Int)
}