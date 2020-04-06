/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 19:23
 */

package org.novid20.sdk.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import java.util.ArrayList

private const val TAG: String = "ActivityManager"

internal class NovidActivityManager(
    private val context: Context
) {

    fun start() {
        val transitions = mutableListOf<ActivityTransition>()
        val activities: MutableList<Int> = ArrayList()
        activities.add(DetectedActivity.IN_VEHICLE)
        activities.add(DetectedActivity.ON_BICYCLE)
        activities.add(DetectedActivity.WALKING)
        activities.add(DetectedActivity.RUNNING)
        activities.add(DetectedActivity.STILL)

        for (activity in activities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val requestCode = 782

        val transitionService = Intent(context, ActivityTransitionService::class.java)
        val activityTransitionPendingIntent = PendingIntent.getService(
            context,
            requestCode,
            transitionService,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val request = ActivityTransitionRequest(transitions)
        val task = ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(request, activityTransitionPendingIntent)
        task.addOnSuccessListener {
            Logger.debug(TAG, "Transition - onSuccess")
        }

        task.addOnFailureListener { exception: Exception ->
            Logger.error(TAG, "Transition - onFailure: " + exception.message, exception)
        }
    }
}