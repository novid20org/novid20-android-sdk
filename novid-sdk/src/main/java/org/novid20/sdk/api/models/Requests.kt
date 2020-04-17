/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.api.models

internal data class RegisterUserRequest(
    val deviceToken: String,
    val bundleId: String
)

internal data class RegisterUserResponse(
    val status: String,
    val message: String,
    val userId: String
)

internal data class InfectionRequest(
    val bundleId: String,
    val timestamp: Long,
    val contacts: List<ApiContact> = emptyList(),
    val locations: List<ApiLocation> = emptyList()
)

internal data class VerifyUserRequest(
    val phone: String
)

internal data class UpdateTokenRequest(val deviceToken: String)

internal data class VerifyCodeRequest(val code: String)

internal data class AnalyticsRequest(
    val timestamp: Long,
    val os: String,
    val osVersion: String,
    val appVersion: String,
    val buildNumber: Long,
    val deviceModel: String,
    val states: List<AnalyticsState>,
    val events: List<AnalyticsEvent>
)