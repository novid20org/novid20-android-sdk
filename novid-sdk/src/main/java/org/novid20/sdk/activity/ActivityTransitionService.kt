/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 19:23
 */

package org.novid20.sdk.activity

import android.app.IntentService
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import org.novid20.sdk.TIMEOUT_STILL
import java.util.concurrent.TimeUnit

private const val TAG: String = "ActivityService"

internal class ActivityTransitionService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        Logger.verbose(TAG, "onHandleIntent")

        val result = ActivityTransitionResult.extractResult(intent)
        if (result != null) {
            val transitionEvents = result.transitionEvents
            for (event in transitionEvents) {

                // chronological sequence of events....
                val activityType = event.activityType
                val transitionType = event.transitionType

                val activityName: String? = getActivityName(activityType)
                val transitionName: String? = getTransitionName(transitionType)
                Logger.debug(TAG, "Got transition: $activityName - $transitionName")

                val novidSdk = NovidSdk.getInstance()
                if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    if (activityType == DetectedActivity.STILL) {
                        val duration = TimeUnit.MINUTES.toMillis(TIMEOUT_STILL)
                        novidSdk.stopDetectionDelayed(duration)
                    } else {
                        novidSdk.startDetection()
                    }
                }
            }
        }
    }

    private fun getActivityName(activityType: Int): String? {
        var activityName: String? = null
        when (activityType) {
            DetectedActivity.IN_VEHICLE -> activityName = "IN_VEHICLE"
            DetectedActivity.ON_BICYCLE -> activityName = "ON_BICYCLE"
            DetectedActivity.ON_FOOT -> activityName = "ON_FOOT"
            DetectedActivity.RUNNING -> activityName = "RUNNING"
            DetectedActivity.WALKING -> activityName = "WALKING"
            DetectedActivity.STILL -> activityName = "STILL"
            DetectedActivity.TILTING -> activityName = "TILTING"
            DetectedActivity.UNKNOWN -> activityName = "UNKNOWN"
            else -> Logger.error(TAG, "Unknown activityType $activityType received.")
        }
        return activityName
    }

    private fun getTransitionName(transitionType: Int): String? {
        var transitionName: String? = null
        when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> transitionName = "ACTIVITY_TRANSITION_ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> transitionName = "ACTIVITY_TRANSITION_EXIT"
        }
        return transitionName
    }
}