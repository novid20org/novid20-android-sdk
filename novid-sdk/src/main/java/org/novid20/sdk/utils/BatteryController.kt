/*
 *  Created by Christoph Kührer & Florian Knoll on 11.04.20 09:55
 *  Copyright (c) 2020. All rights reserved.
 *  Last modified 11.04.20 09:55
 */

package org.novid20.sdk.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import org.novid20.sdk.Logger

class BatteryController {

    companion object {

        private const val BATTERY_REQUEST_CODE = 253

        private val TAG: String = Logger.makeLogTag(BatteryController::class.java)

        @RequiresApi(Build.VERSION_CODES.M)
        fun requestIgnoreBatteryOptimization(context: Activity) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            val packageName: String = context.packageName
            intent.data = Uri.parse("package:$packageName")

            try {
                context.startActivityForResult(intent, BatteryController.BATTERY_REQUEST_CODE)
            } catch (t: Throwable) {
                Logger.error(TAG, t.message, t)
            }
        }

        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            var ignoringBatteryOptimizations = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                pm?.let {
                    val packageName = context.packageName
                    ignoringBatteryOptimizations = it.isIgnoringBatteryOptimizations(packageName)
                }
            }
            return ignoringBatteryOptimizations
        }
    }
}