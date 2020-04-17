/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 19:23
 */

package org.novid20.sdk.analytics

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.novid20.sdk.Logger
import org.novid20.sdk.model.NovidRepository

const val EVENT_SCREEN_VIEW = "screen_view"
const val EVENT_SIGN_UP = "sign_up"

internal class SdkAnalyticsImpl(private val repository: NovidRepository,
                                private val appAnalytics: AppAnalytics?) : Analytics {

    companion object {
        private const val TAG = "AnalyticsImpl"
    }

    override fun sendScreenView(activity: Activity, screenName: String) {
        Logger.debug(TAG, "Logging screen view for screen: $screenName")

        GlobalScope.launch { repository.saveEvent(EVENT_SCREEN_VIEW, screenName) }
        appAnalytics?.sendScreenView(activity, screenName)
    }

    override fun sendEvent(event: String, value: String?) {
        Logger.debug(TAG, "Logging event $event with value $value")
        GlobalScope.launch { repository.saveEvent(event, value.orEmpty()) }
        appAnalytics?.sendEvent(event, value)
    }

}