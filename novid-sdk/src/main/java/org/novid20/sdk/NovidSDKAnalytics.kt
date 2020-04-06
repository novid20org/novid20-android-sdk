/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:35
 */

package org.novid20.sdk

internal interface NovidSDKAnalytics {

    companion object {
        const val EVENT_CONTACT = "contact_detected"

        const val EVENT_SERVICE_ON = "service_on"
        const val EVENT_SERVICE_OFF = "service_off"

        const val EVENT_DETECTION_ON = "detection_start"
        const val EVENT_DETECTION_OFF = "detection_stop"
    }
}