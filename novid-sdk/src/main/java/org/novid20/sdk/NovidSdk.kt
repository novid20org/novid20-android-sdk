/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 16:24
 */

package org.novid20.sdk

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.util.ProcessUtils
import org.novid20.sdk.analytics.Analytics
import org.novid20.sdk.analytics.DeviceDataProvider
import org.novid20.sdk.api.AuthTokenLoader

interface NovidSdk {

    fun getConfig() = sdk?.config!!

    fun getRepository() = sdk?.novidRepository!!

    val analytics: Analytics

    val deviceDataProvider: DeviceDataProvider

    var authTokenLoader: AuthTokenLoader?

    fun configure(activity: FragmentActivity)

    fun removeData()

    fun startService()

    fun stopService()

    fun isServiceRunning(): Boolean

    fun startDetection()

    fun stopDetectionDelayed(duration: Long)

    fun hasErrors(): Boolean

    fun messageReceived(bundle: Bundle = Bundle())

    companion object {

        private const val TAG = "NovidSdk"
        private var sdk: NovidSdkImpl? = null

        /**
         * This will return the global singleton instance
         * of the NovidSdk. Make sure to call [initialize] before,
         * otherwise an [IllegalStateException] will be thrown.
         */
        fun getInstance(): NovidSdk {
            return sdk ?: throw IllegalStateException(
                "NovidSdk is not initialized " +
                        "in this process ${ProcessUtils.getMyProcessName()}. " +
                        "Make sure to call NovidSdk.initialize(Context) first."
            )
        }

        /**
         * This must be called in your [android.app.Application] class.
         */
        fun initialize(context: Context, accessToken: String, deviceDataProvider: DeviceDataProvider) {
            Logger.debug(TAG, "Initializing NovidSdk.")
            sdk = NovidSdkImpl(context.applicationContext, deviceDataProvider, accessToken)
        }
    }
}