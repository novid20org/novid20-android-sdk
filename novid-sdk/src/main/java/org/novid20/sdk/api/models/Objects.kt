/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:35
 */

package org.novid20.sdk.api.models

internal data class Status(
    val userId: String,
    val infected: Boolean,
    val infectedContactCount: Int
)

internal data class ApiContact(
    val userId: String,
    val timestamp: Long,
    val duration: Long? = null, // seconds
    val distance: Int? = null,
    val rssi: Int? = null,
    val source: String? = null,
    val background: Boolean? = null
)

internal data class ApiLocation(
    val timestamp: Long,
    val lat: Double,
    val lng: Double
)

internal data class AnalyticsState(val name: String, val value: String)

internal data class AnalyticsEvent(val id: Int, val name: String, val value: String, val timestamp: Long)