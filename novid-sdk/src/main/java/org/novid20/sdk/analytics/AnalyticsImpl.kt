/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 19:23
 */

package org.novid20.sdk.analytics

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.novid20.sdk.Logger
import org.novid20.sdk.model.NovidRepository
import java.util.Locale

const val EVENT_SCREEN_VIEW = "screen_view"

internal class AnalyticsImpl(context: Context, private val repository: NovidRepository) : Analytics {

    companion object {
        private const val TAG = "AnalyticsImpl"
    }

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun sendScreenView(activity: Activity, screenName: String) {
        Logger.debug(TAG, "Logging screen view for screen: $screenName")

        firebaseAnalytics.setCurrentScreen(activity, screenName, null)
        GlobalScope.launch { repository.saveEvent(EVENT_SCREEN_VIEW, screenName) }
    }

    override fun sendEvent(event: String, value: String?) {
        val bundle = Bundle().apply { putString("value", value) }
        Logger.debug(TAG, "Logging event $event with data $bundle")

        firebaseAnalytics.logEvent(event.toLowerCase(Locale.getDefault()), bundle)
        GlobalScope.launch { repository.saveEvent(event, value.orEmpty()) }
    }

}