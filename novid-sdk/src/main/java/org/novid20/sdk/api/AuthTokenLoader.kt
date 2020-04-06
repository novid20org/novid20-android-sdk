/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.api

import androidx.annotation.WorkerThread

interface AuthTokenLoader {
    @WorkerThread
    fun refreshToken(uid: String): String
}