/*
 *  Created by Christoph Kührer & Florian Knoll on 13.04.20 17:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 13.04.20 17:10
 */

package org.novid20.sdk.utils

import org.novid20.sdk.DetectionConfig
import org.novid20.sdk.Logger

internal object Extensions {

    private const val TAG = "Extensions"

    internal fun String.isValidNovidUserId(detectionConfig: DetectionConfig): Boolean {
        if (!startsWith(detectionConfig.namePrefix)) {
            // Ignore values that start with the custom prefix.
            Logger.warn(
                TAG, "User id does not " +
                    "start with \"${detectionConfig.namePrefix}\" ($this)"
            )
            return false
        }

        if (length > detectionConfig.maxLength) {
            Logger.warn(TAG, "User id is too long ($this)")
            return false
        }

        if (length < detectionConfig.minLength) {
            Logger.warn(TAG, "User id is too short ($this)")
            return false
        }

        return true
    }

}