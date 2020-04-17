/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

/**
 * Implementation of the Bluetooth GATT Profile.
 * https://www.bluetooth.com/specifications/adopted-specifications
 */

internal object UserIdProfile {

    fun createNovidService(bleConfig: BleConfig): BluetoothGattService {
        val novidService = BluetoothGattService(
            bleConfig.serviceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val userIdCharacteristics = BluetoothGattCharacteristic(
            bleConfig.characteristicUuid,
            //Read-only characteristic
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        novidService.addCharacteristic(userIdCharacteristics)
        return novidService
    }
}