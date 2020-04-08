/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import org.novid20.sdk.TECHNOLOGY_BLUETOOTH
import org.novid20.sdk.model.NovidRepositoryImpl

internal class BluetoothScanReceiver : BroadcastReceiver() {

    private val TAG = Logger.makeLogTag(this@BluetoothScanReceiver.javaClass)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (BluetoothDevice.ACTION_FOUND == action) {
            val name = device.name
            val address = device.address
            Logger.verbose(TAG, "Device found $name $address")
            onDeviceFound(context, intent)
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
            val name = device.name
            val address = device.address
            Logger.verbose(TAG, "Device bond changed $name $address")
            onBondChanged(context, intent)
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
            val name = device.name
            val address = device.address
            Logger.verbose(TAG, "Device is now connected $name $address")
            onConnected(context, intent)
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
            Logger.verbose(TAG, "Done searching")
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED == action) {
            Logger.verbose(TAG, "Device is about to disconnect")
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
            Logger.verbose(TAG, "Device has disconnected")
        }
    }

    private fun onConnected(context: Context, intent: Intent) {
        trackResult(context, intent)
    }

    private fun onBondChanged(context: Context, intent: Intent) {
        trackResult(context, intent)
    }

    private fun onDeviceFound(context: Context, intent: Intent) {
        trackResult(context, intent)
    }

    /**
     * Store the result to local database
     * @param intent
     */
    private fun trackResult(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (device != null) {
            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
            val address: String? = device.address
            val deviceName: String? = device.name
            val bondState: Int? = device.bondState

            val novidRepository = NovidSdk.getInstance().getRepository()
            if (deviceName?.startsWith(NovidRepositoryImpl.BT_NAME_PREFIX) == true) {
                Logger.debug(TAG, "Bluetooth: $deviceName rssi:$rssi")
                novidRepository.contactDetected(deviceName, source = TECHNOLOGY_BLUETOOTH, rssi = rssi)
            }
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        context.registerReceiver(this, filter)
    }
}