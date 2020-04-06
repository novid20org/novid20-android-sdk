/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import org.novid20.sdk.Logger
import org.novid20.sdk.TECHNOLOGY_BLE_CACHE
import org.novid20.sdk.TECHNOLOGY_BLE_CLIENT
import org.novid20.sdk.TECHNOLOGY_BLE_NAME
import org.novid20.sdk.ble.UserIdProfile.APP_SERVICE
import org.novid20.sdk.ble.UserIdProfile.NOVID_SERVICE
import org.novid20.sdk.ble.UserIdProfile.USERID_INFO
import org.novid20.sdk.model.NovidRepository
import org.novid20.sdk.model.NovidRepositoryImpl
import java.util.concurrent.TimeUnit
import kotlin.math.min

private val TAG: String = Logger.makeLogTag("BleBluetoothManager")

// Bluetooth device address to userId
internal val deviceMap: MutableMap<String, String> = mutableMapOf()
internal val foundAddressMap: MutableMap<String, Long> = mutableMapOf()

internal class BleBluetoothManager(
    private val context: Context,
    repo: NovidRepository
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val scanCallback = BleScanCallback(repo)

    init {
        BluetoothScanReceiver().apply {
            register(context)
        }
    }

    fun startDiscovery() {

        val scanSetting = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setReportDelay(5000)
            .build()

        val scanFilters: MutableList<ScanFilter> = mutableListOf()


        val builder = ScanFilter.Builder()
        builder.setServiceUuid(ParcelUuid(UserIdProfile.APP_SERVICE))
        // Filter is not working for IOS Background advertising
        //scanFilters.add(builder.build())

        val bluetoothAdapter = bluetoothManager.adapter
        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Logger.error(TAG, "Failed to start scan because bleScanner is null!")
        } else {
            bleScanner.startScan(scanFilters, scanSetting, scanCallback)
            Logger.verbose(TAG, "startScan")
        }
    }

    fun stopDiscovery() {
        Logger.verbose(TAG, "stopDiscovery")
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
    }

    class GattCallback(private val repo: NovidRepository) : BluetoothGattCallback() {

        private val TAG: String = Logger.makeLogTag("GattCallback")

        var connecting = false

        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Logger.info(TAG, "Connected to GATT " + gatt?.discoverServices())
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Logger.info(TAG, "Disconnected from GATT server.")
                connecting = false
            } else {
                Logger.info(TAG, "onConnectionStateChange $status $newState")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Logger.warn(TAG, "Service discovery failed: $status")
                return
            }

            val services = gatt?.services
            if (services != null) {
                for (service in services) {
                    Logger.verbose(TAG, "Service: " + service.uuid.toString())
                }
            }

            val characteristic = gatt
                ?.getService(NOVID_SERVICE)
                ?.getCharacteristic(USERID_INFO)

            characteristic?.let {
                gatt.readCharacteristic(it)
            } ?: gatt?.disconnect()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Logger.verbose(TAG, "onCharacteristicRead")

            characteristic.let {
                val userIdData = it.value
                val userId = userIdData?.toString(Charsets.UTF_8)
                Logger.debug(TAG, "BLE UserId: $userId")

                userId?.let { id ->
                    val address = gatt.device!!.address
                    // userid is sometimes repeating oO - nov20-1422053658860784nov20-1422053658860784n
                    // at least this happens at pixel XL

                    try {
                        // Try to extract the nov20 id, not beautiful but at least it works
                        val second = id.indexOf("nov20", id.indexOf("nov20") + 1)
                        val trimmedId = if (second > 0) {
                            id.substring(0, min(id.length, second))
                        } else {
                            id
                        }
                        val verifiedContact = repo.contactDetected(trimmedId, source = TECHNOLOGY_BLE_CLIENT)
                        if (verifiedContact) {
                            // only store in map if real userid
                            deviceMap[address] = trimmedId
                        }
                    } catch (t: Throwable) {
                        // java.lang.StringIndexOutOfBoundsException: String index out of range: -1
                        // Make sure that this does not fuck up bluetooth
                        Logger.warn(TAG, t.message, t)
                    }
                }
            }
            gatt.disconnect()
        }
    }

    class BleScanCallback(private val repo: NovidRepository) : ScanCallback() {

        private val gattCallback = GattCallback(repo)

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            Logger.warn(TAG, "Scan failed with code: $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            scanResultReceived(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

            if (!results.isNullOrEmpty()) {
                Logger.verbose(TAG, "onBatchScanResults")
                for (result in results.distinctBy { it.device.address }) {
                    scanResultReceived(result)
                }
            }
        }

        private fun scanResultReceived(result: ScanResult?) {
            val device = result?.device

            val deviceName = device?.name
            val deviceAddress = device?.address
            val scanRecord = result?.scanRecord
            val uuids = scanRecord?.serviceUuids

            val rssi = result?.rssi
            val bytes = scanRecord?.bytes

            Logger.verbose(TAG, "BLE Result: $deviceName rssi:$rssi address:$deviceAddress ${bytes?.joinToString()}")

            if (uuids != null) {
                for (uuid in uuids) {
                    Logger.verbose(TAG, "Found UUID: " + uuid.uuid.toString())
                }
            }

            val validUuids = uuids?.filter {
                it.uuid.toString() == APP_SERVICE.toString()
            }
            if (!validUuids.isNullOrEmpty()) {
                if (deviceName?.startsWith(NovidRepositoryImpl.BT_NAME_PREFIX) == true) {
                    Logger.debug(TAG, "Found iOS: $deviceName $deviceAddress rssi:$rssi")
                    // IOS Case
                    deviceName.let { repo.contactDetected(it, source = TECHNOLOGY_BLE_NAME, rssi = rssi) }
                } else {
                    // Android case? - lets discover services
                    Logger.debug(TAG, "Found Android: $deviceName $deviceAddress rssi:$rssi")

                    val current = System.currentTimeMillis()
                    val lastFound =
                        if (foundAddressMap.containsKey(deviceAddress)) foundAddressMap[deviceAddress] else 0
                    val diff = current - (lastFound ?: 0)

                    val isInCache = deviceAddress?.let { deviceMap.contains(it) ?: false }
                    if (isInCache == false) {
                        connectTo(device)
                    } else if (diff > TimeUnit.SECONDS.toMillis(30)) {  // only report every x seconds
                        val userId = deviceMap[deviceAddress]
                        if (userId?.startsWith(NovidRepositoryImpl.BT_NAME_PREFIX) == true) {
                            deviceAddress?.let { foundAddressMap[deviceAddress] = current }
                            userId.let { id ->
                                repo.contactDetected(
                                    id,
                                    source = TECHNOLOGY_BLE_CACHE,
                                    rssi = rssi
                                )
                            }
                        }
                    }
                }
            } else if (deviceName?.startsWith(NovidRepositoryImpl.BT_NAME_PREFIX) == true) {
                Logger.debug(TAG, "Found iOS Background: $deviceName $deviceAddress rssi:$rssi")
                // IOS Case
                deviceName.let { repo.contactDetected(it, source = TECHNOLOGY_BLE_NAME, rssi = rssi) }
            }
        }

        private fun connectTo(device: BluetoothDevice) {
            val deviceAddress = device.address
            if (!gattCallback.connecting) {
                gattCallback.connecting = true

                // The context argument can be null in this case because it is never used anyways (as of Android 5 - 11)
                device.connectGatt(null, false, gattCallback)
            }
        }
    }
}