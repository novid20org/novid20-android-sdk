/*
 *  Created by Christoph Kührer & Florian Knoll on 13.04.20 14:10
 *  Copyright (c) 2020. All rights reserved.
 *  Last modified 13.04.20 14:10
 */

package org.novid20.sdk

data class DetectionConfig(
    val namePrefix: String,
    val minLength: Int,
    val maxLength: Int
)

internal val DEFAULT_DETECTION_CONFIG = DetectionConfig(
    namePrefix = "nvSDK-",
    minLength = 15,
    maxLength = 23
)