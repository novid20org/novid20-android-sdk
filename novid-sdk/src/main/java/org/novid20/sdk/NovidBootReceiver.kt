/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:35
 */

package org.novid20.sdk

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

private const val TAG = "NotificationService"

/**
 * This class is triggered whenever the device boots.
 * By launching this service, the Application class is also initialized
 * and the Application class should have the NovidSdk initialization
 * code in it and will also start the application.
 */
class NovidBootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        Logger.debug(TAG, "NovidBootReceiver triggered and Application class initialized.")
    }

}