package org.novid20.sdk.ble

import java.util.UUID

data class BleConfig(
    val appUuid: UUID,
    val serviceUuid: UUID,
    val characteristicUuid: UUID
)

internal val DEFAULT_BLE_CONFIG = BleConfig(
    appUuid = UUID.fromString("de65c482-7a45-11ea-bc55-0242ac130003"),
    serviceUuid = UUID.fromString("e9143e04-7a45-11ea-bc55-0242ac130003"),
    characteristicUuid = UUID.fromString("f0626dc0-7a45-11ea-bc55-0242ac130003")
)