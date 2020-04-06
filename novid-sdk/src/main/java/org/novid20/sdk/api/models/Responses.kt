/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.api.models

/**
 * We need the @JvmOverloads annotation so that
 * GSON can create an object with only the required fields.
 */
internal data class ApiResponse @JvmOverloads constructor(
    val status: Int,
    val message: String? = null,
    val extraInfo: String? = null,
    val code: String? = null
) {
    val isSuccessful: Boolean
        get() = status in 200..299

    internal fun toResult(): org.novid20.sdk.model.Result {
        return org.novid20.sdk.model.Result(
            status = status,
            message = message,
            extraInfo = extraInfo,
            code = code
        )
    }
}

internal data class GetStatusResponse(
    val userId: String,
    val infectedContacts: Int,
    val isInfected: Boolean
)
