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

class NovidBootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        Logger.debug(TAG, "onReceive")
    }

}