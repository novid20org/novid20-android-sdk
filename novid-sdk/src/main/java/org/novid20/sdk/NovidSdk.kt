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
import org.novid20.sdk.analytics.AppAnalytics
import org.novid20.sdk.analytics.DeviceDataProvider
import org.novid20.sdk.analytics.DeviceDataProviderImpl
import org.novid20.sdk.api.AuthTokenLoader
import org.novid20.sdk.ble.BleConfig
import org.novid20.sdk.ble.DEFAULT_BLE_CONFIG

interface NovidSdk {

    fun getConfig() = sdk?.config!!

    fun getRepository() = sdk?.novidRepository!!

    val analytics: Analytics

    val bundleId: String

    val bleConfig: BleConfig

    val detectionConfig: DetectionConfig

    val deviceDataProvider: DeviceDataProvider

    val appAnalytics: AppAnalytics?

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
        fun initialize(
            context: Context,
            accessToken: String,
            bundleId: String = context.packageName,
            detectionConfig: DetectionConfig = DEFAULT_DETECTION_CONFIG,
            bleConfig: BleConfig = DEFAULT_BLE_CONFIG,
            deviceDataProvider: DeviceDataProvider = DeviceDataProviderImpl(context),
            appAnalytics: AppAnalytics? = null
        ) {
            Logger.debug(TAG, "Initializing NovidSdk.")
            sdk = NovidSdkImpl(
                context.applicationContext,
                accessToken,
                bundleId,
                detectionConfig,
                bleConfig,
                deviceDataProvider,
                appAnalytics
            )
        }
    }
}
