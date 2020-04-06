/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:34
 */

package org.novid20.sdk.analytics

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.novid20.sdk.Logger
import org.novid20.sdk.api.CustomClientImpl

object CustomerIo {

    // customer.io keys
    private const val KEY_CIO_DELIVERY_ID = "CIO-Delivery-ID"
    private const val KEY_CIO_DELIVERY_TOKEN = "CIO-Delivery-Token"

    private val TAG: String = Logger.makeLogTag(CustomerIo::class.java)

    fun trackIntent(context: Context, intent: Intent?) {
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                if (intent.hasExtra(KEY_CIO_DELIVERY_ID)) {
                    val deliveryId = intent.getStringExtra(KEY_CIO_DELIVERY_ID)
                    val deliveryToken = intent.getStringExtra(KEY_CIO_DELIVERY_TOKEN)
                    Logger.debug(TAG, "handleIntent (customer.io): [$deliveryId] [$deliveryToken]")

                    GlobalScope.launch {
                        if (deliveryId != null && deliveryToken != null) {
                            val customClientImpl = CustomClientImpl(context)
                            customClientImpl.trackIntent(deliveryId, deliveryToken)
                        }
                    }
                }
            }
        }
    }
}