/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:42
 */

package org.novid20.sdk.api

import org.novid20.sdk.api.models.AnalyticsRequest
import org.novid20.sdk.api.models.ApiContact
import org.novid20.sdk.api.models.ApiLocation
import org.novid20.sdk.api.models.ApiResponse
import org.novid20.sdk.api.models.Status

internal interface NovidClient {

    fun registerUser(deviceToken: String): String?

    fun updateDeviceToken(deviceToken: String): Boolean

    fun verifyUser(phone: String): ApiResponse

    fun verifyCode(tan: String): ApiResponse

    fun reportInfection(
        contacts: List<ApiContact>,
        location: List<ApiLocation>
    ): ApiResponse

    fun getStatus(): Status?

    fun submitAnalytics(analyticsRequest: AnalyticsRequest): Boolean
}