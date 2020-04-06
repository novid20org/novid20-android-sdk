/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * Implementation of the Bluetooth GATT Profile.
 * https://www.bluetooth.com/specifications/adopted-specifications
 */
internal object UserIdProfile {

    val APP_SERVICE: UUID = UUID.fromString("8b9b6576-6db7-11ea-bc55-0242ac130003")
    val NOVID_SERVICE: UUID = UUID.fromString("b16efb34-6c34-11ea-bc55-0242ac130003")

    val USERID_INFO: UUID = UUID.fromString("1d45dc00-6db7-11ea-bc55-0242ac130003")

    fun createNovidService(): BluetoothGattService {
        val novidService = BluetoothGattService(
            NOVID_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val userIdCharacteristics = BluetoothGattCharacteristic(
            USERID_INFO,
            //Read-only characteristic
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        novidService.addCharacteristic(userIdCharacteristics)
        return novidService
    }
}