/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.analytics

import org.novid20.sdk.model.AnalyticsStateEntry

interface DeviceDataProvider {
    fun getAppVersion(): String

    fun getBuildNumber(): Int

    fun getStates(): List<AnalyticsStateEntry>
}