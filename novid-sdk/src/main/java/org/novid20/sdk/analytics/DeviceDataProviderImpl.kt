/*
 *  Created by Christoph Kührer & Florian Knoll on 13.04.20 14:16
 *  Copyright (c) 2020. All rights reserved.
 *  Last modified 13.04.20 14:16
 */

package org.novid20.sdk.analytics

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import org.novid20.sdk.model.AnalyticsStateEntry


internal class DeviceDataProviderImpl(private val context: Context) : DeviceDataProvider {

    override fun getAppVersion(): String {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    }

    override fun getBuildNumber(): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    override fun getStates(): List<AnalyticsStateEntry> {
        return emptyList()
    }
}