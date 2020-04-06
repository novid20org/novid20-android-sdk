/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:43
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
import org.novid20.sdk.BuildConfig
import org.novid20.sdk.BuildConfigHelper
import org.novid20.sdk.Logger
import org.novid20.sdk.api.models.AnalyticsRequest
import org.novid20.sdk.api.models.ApiContact
import org.novid20.sdk.api.models.ApiLocation
import org.novid20.sdk.api.models.ApiResponse
import org.novid20.sdk.api.models.GetStatusResponse
import org.novid20.sdk.api.models.InfectionRequest
import org.novid20.sdk.api.models.RegisterUserRequest
import org.novid20.sdk.api.models.RegisterUserResponse
import org.novid20.sdk.api.models.Status
import org.novid20.sdk.api.models.UpdateTokenRequest
import org.novid20.sdk.api.models.VerifyCodeRequest
import org.novid20.sdk.api.models.VerifyUserRequest

private const val TAG = "NovidClientImpl"

internal class NovidClientImpl(val context: Context, authTokenLoader: AuthTokenLoader) : NovidClient {

    private val client: OkHttpClient
    private val gson: Gson

    private val baseUrl = BuildConfigHelper.API_ENDPOINT
    private val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        when {
            BuildConfig.DEBUG -> httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
            else -> httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }

        client = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor(context, authTokenLoader))
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(LoggerInterceptor(context))
            .addInterceptor(ChuckerInterceptor(context))
            .build()

        val gsonBuilder = GsonBuilder()
        gson = gsonBuilder.create()
    }

    override fun registerUser(deviceToken: String): String? {

        val registerUserRequest = RegisterUserRequest(
            deviceToken = deviceToken,
            bundleId = context.packageName
        )

        val content = gson.toJson(registerUserRequest)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/registerUser")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful || response.code == 409 /* = USER_ALREADY_EXISTS */) {

            val userResponse = gson.fromJson(
                response.body?.charStream(),
                RegisterUserResponse::class.java
            )

            return userResponse.userId
        }
        throw IllegalArgumentException()
    }

    override fun updateDeviceToken(deviceToken: String): Boolean {
        val registerUserRequest = UpdateTokenRequest(deviceToken)
        val content = gson.toJson(registerUserRequest)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/updateDeviceToken")
            .put(body)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            return true
        }
        return false
    }

    override fun verifyUser(phone: String): ApiResponse {
        val registerUserRequest = VerifyUserRequest(phone)
        val content = gson.toJson(registerUserRequest)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/verifyUser")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return gson.runCatching { fromJson(response.body?.charStream(), ApiResponse::class.java) }
            .onFailure { Logger.error(TAG, "Verify user call failed.", it) }
            .getOrDefault(ApiResponse(status = response.code, message = response.message))
    }

    override fun verifyCode(tan: String): ApiResponse {
        val registerUserRequest = VerifyCodeRequest(tan)
        val content = gson.toJson(registerUserRequest)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/verifyCode")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return gson.runCatching { fromJson(response.body?.charStream(), ApiResponse::class.java) }
            .onFailure { Logger.error(TAG, "Verify code request failed.", it) }
            .getOrDefault(ApiResponse(status = response.code, message = response.message))
    }

    override fun reportInfection(
        contacts: List<ApiContact>,
        location: List<ApiLocation>
    ): ApiResponse {
        val bundleId = context.packageName

        val current = System.currentTimeMillis()
        val registerUserRequest = InfectionRequest(
            bundleId = bundleId,
            timestamp = current,
            contacts = contacts,
            locations = location
        )

        val content = gson.toJson(registerUserRequest)
        val body = content.toRequestBody(contentType)

        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/infected")
            .post(body)
            .build()
        val response = client.newCall(request).execute()

        return gson.runCatching { fromJson(response.body?.charStream(), ApiResponse::class.java) }
            .onFailure { Logger.error(TAG, "Report infection call failed.", it) }
            .getOrDefault(ApiResponse(status = response.code, message = response.message))
    }

    override fun getStatus(): Status {
        val request: Request = Request.Builder()
            .url("${baseUrl}/mobile/status")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            val statusResponse = gson.fromJson(responseBody, GetStatusResponse::class.java)
            val infected = statusResponse.isInfected
            val infectedContactCount = statusResponse.infectedContacts
            return Status(infected, infectedContactCount)
        }

        return Status(false, 0)
    }

    override fun submitAnalytics(analyticsRequest: AnalyticsRequest): Boolean {
        val content = gson.toJson(analyticsRequest)
        val body = content.toRequestBody(contentType)

        val request = Request.Builder()
            .url("$baseUrl/mobile/deviceAnalytics")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.isSuccessful
    }
}