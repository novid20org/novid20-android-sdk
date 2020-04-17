/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import org.novid20.sdk.TECHNOLOGY_BLE_SERVER
import org.novid20.sdk.model.NovidRepository

private val TAG: String = Logger.makeLogTag("BleServerManager")

internal class BleServerManager(
    private val repo: NovidRepository,
    private val bleConfig: BleConfig,
    private val context: Context
) {

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGattServer: BluetoothGattServer? = null

    private val registeredDevices = mutableSetOf<BluetoothDevice>()

    private val appService = BluetoothGattService(
        bleConfig.appUuid,
        BluetoothGattService.SERVICE_TYPE_PRIMARY
    )

    val novidService = UserIdProfile.createNovidService(bleConfig)

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Logger.info(TAG, "BluetoothDevice CONNECTED: $device (status=$status)")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Logger.info(TAG, "BluetoothDevice DISCONNECTED: $device (status=$status)")
                //Remove device from any active subscriptions
                registeredDevices.remove(device)
            } else {
                Logger.info(TAG, "Unhandled BluetoothDevice state change for state: $newState")
            }
            trackResult(device)
        }

        private fun trackResult(device: BluetoothDevice) {
            val deviceName: String? = device.name
            if (!deviceName.isNullOrBlank()) {
                repo.contactDetected(deviceName, source = TECHNOLOGY_BLE_SERVER)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            super.onServiceAdded(status, service)
            Logger.info(TAG, "onServiceAdded " + service?.uuid)

            if (bluetoothGattServer?.services?.contains(novidService) == false) {
                bluetoothGattServer?.addService(novidService)
            } else {
                startAdvertising()
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Logger.verbose(TAG, "onMtuChanged $mtu")
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {

            val now = System.currentTimeMillis()
            when {
                bleConfig.characteristicUuid == characteristic.uuid -> {

                    val userId = NovidSdk.getInstance().getConfig().userId

                    if (offset >= userId!!.length) {
                        bluetoothGattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_INVALID_OFFSET,
                            offset,
                            null
                        )
                    } else {
                        val response = userId.substring(offset).toByteArray(Charsets.UTF_8)
                        Logger.verbose(TAG, "Read UserId with offset $offset")
                        bluetoothGattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            response
                        )
                    }
                }
                else -> {
                    // Invalid characteristic
                    Logger.warn(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        offset,
                        null
                    )
                }
            }
        }
    }

    fun start() {

        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (checkBluetoothSupport(bluetoothAdapter)) {
            startServer()
        }
    }

    fun stop() {

        try {
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter.isEnabled) {
                stopServer()
                stopAdvertising()
            }
        } catch (t: Throwable) {
            // TODO maybe... ^^
            //   kotlin.UninitializedPropertyAccessException:
            //   lateinit property bluetoothManager has not been initialized
        }
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private fun startServer() {
        Logger.debug(TAG, "startServer")
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        bluetoothGattServer?.let {
            it.addService(appService)
        } ?: Logger.warn(TAG, "Unable to create GATT server")
    }

    /**
     * Shut down the GATT server.
     */
    private fun stopServer() {
        Logger.debug(TAG, "startServer")
        bluetoothGattServer?.close()
    }

    private fun startAdvertising() {
        val adapter = bluetoothManager.adapter
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(bleConfig.appUuid))
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val parameters =
                    AdvertisingSetParameters.Builder()
                        .setLegacyMode(true)
                        .setScannable(true)
                        .setConnectable(true)
                        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_LOW)
                        .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                        .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
                        .build()

                it.startAdvertisingSet(
                    parameters,
                    data,
                    null,
                    null,
                    null,
                    object : AdvertisingSetCallback() {

                        override fun onAdvertisingSetStarted(
                            advertisingSet: AdvertisingSet?,
                            txPower: Int,
                            status: Int
                        ) {
                            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
                            Logger.debug(TAG, "onAdvertisingSetStarted")
                        }
                    }
                )
            } else {
                it.startAdvertising(settings, data, advertiseCallback)
            }
        }
    }

    private fun stopAdvertising() {
        try {
            val adapter = bluetoothManager.adapter
            val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = adapter.bluetoothLeAdvertiser
            bluetoothLeAdvertiser?.let {
                it.stopAdvertising(advertiseCallback)
            } ?: Logger.warn(TAG, "Failed to create advertiser")
        } catch (t: Throwable) {
            /**
             * Fatal Exception: kotlin.UninitializedPropertyAccessException
             * lateinit property bluetoothManager has not been initialized
             * org.novid20.sdk.ble.BleServerManager.stop
             */
        }
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System [BluetoothAdapter].
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null) {
            Logger.warn(TAG, "Bluetooth is not supported")
            return false
        }

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Logger.warn(TAG, "Bluetooth LE is not supported")
            return false
        }

        return true
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Logger.info(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Logger.warn(TAG, "LE Advertise Failed: $errorCode")
        }
    }
}