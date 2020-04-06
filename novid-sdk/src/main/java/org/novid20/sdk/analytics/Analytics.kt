/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 19:23
 */

package org.novid20.sdk.analytics

import android.app.Activity

interface Analytics {
    fun sendScreenView(activity: Activity, screenName: String)

    fun sendEvent(event: String, value: String? = null)
}