/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:54
 */

package org.novid20.sdk.model

data class Result @JvmOverloads constructor(
    val status: Int,
    val message: String? = null,
    val extraInfo: String? = null,
    val code: String? = null
) {
    val isSuccessful: Boolean
        get() = status in 200..299
}

class ContactEntry(
    internal val id: Long? = null,
    val user: String,
    val time: Long,
    val source: String?,
    val duration: Long? = null,
    var infected: Boolean = false,
    val distance: Int? = null,
    val rssi: Int? = null,
    val background: Boolean = false
)

class LocationEntry(
    val time: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)

data class AnalyticsStateEntry(
    val name: String,
    val value: String
)

data class AnalyticsEventEntry(
    val id: Int,
    val name: String,
    val value: String,
    val timestamp: Long
)

data class Alert(
    val shown: Boolean = false
)