/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 16:12
 */

package org.novid20.sdk.api

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.novid20.sdk.BuildConfig
import org.novid20.sdk.Logger

private const val TAG = "CustomClientImpl"

/**
 * Own client without sdk headers
 */
internal class CustomClientImpl(val context: Context) {

    private val client: OkHttpClient
    private val gson: Gson

    private val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        when {
            BuildConfig.DEBUG -> httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
            else -> httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }

        client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(LoggerInterceptor(context))
            .addInterceptor(ChuckerInterceptor(context))
            .build()

        val gsonBuilder = GsonBuilder()
        gson = gsonBuilder.create()
    }

    fun trackIntent(deliveryId: String, deliveryToken: String) {

        val url = "https://track.customer.io/push/events"

        val customerIoBody = JSONObject()
        customerIoBody.accumulate("delivery_id", deliveryId)
        customerIoBody.accumulate("event", "opened")
        customerIoBody.accumulate("device_id", deliveryToken)
        customerIoBody.accumulate("timestamp", System.currentTimeMillis() / 1000)

        val content = gson.toJson(customerIoBody)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            Logger.debug(TAG, "CustomerIO tracking successful")
        } else {
            Logger.debug(TAG, "CustomerIO tracking failed")
        }
    }
}