package org.novid20.sdk.ble

import java.util.UUID

data class BleDetectionConfig(
    val namePrefix: String,
    val appUuid: UUID,
    val serviceUuid: UUID,
    val characteristicUuid: UUID
)

private val DEFAULT_BLE_DETECTION_CONFIG = BleDetectionConfig(
    namePrefix = "nov20-",
    appUuid = UUID.fromString("8b9b6576-6db7-11ea-bc55-0242ac130003"),
    serviceUuid = UUID.fromString("b16efb34-6c34-11ea-bc55-0242ac130003"),
    characteristicUuid = UUID.fromString("1d45dc00-6db7-11ea-bc55-0242ac130003")
)