/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 16:26
 */

package org.novid20.sdk.api

import android.content.Context
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.novid20.sdk.BuildConfigHelper
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.Locale

internal class HeaderInterceptor(
    private val context: Context,
    private val authTokenLoader: AuthTokenLoader
) : Interceptor {

    companion object {

        private val TAG = Logger.makeLogTag("HeaderInterceptor")
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest = chain.request()
        val novidSdk = NovidSdk.getInstance()
        val config = novidSdk.getConfig()
        val accessToken = config.accessToken
        val newRequest = oldRequest.newBuilder().addNovidHeaders(config.uid, config.authToken, accessToken).build()
        val response = chain.proceed(newRequest)
        val responseCode = response.code

        // Acquiring lock to prevent other requests from the same instance
        // from updating the token at the same time
        synchronized(novidSdk) {
            if (responseCode == HTTP_UNAUTHORIZED) {
                // Auth token is invalid, try to refresh it
                Logger.debug(TAG, "Auth token is expired, refreshing.")

                Logger.debug(TAG, "Making call to AuthTokenLoader.refreshToken")
                val newToken = authTokenLoader.refreshToken(config.uid.orEmpty())
                Logger.debug(TAG, "New token: $newToken")
                config.authToken = newToken
                return chain.proceed(oldRequest.newBuilder().addNovidHeaders(config.uid, newToken, accessToken).build())
            }
        }

        return response
    }

    /**
     * This function adds the following headers to a request:r
     * Accept-Language
     * User-Agent
     * x-access-token
     * x-auth-token
     * x-uid
     */
    private fun Request.Builder.addNovidHeaders(
        uid: String?,
        authToken: String?,
        accessToken: String
    ): Request.Builder {
        val locale = Locale.getDefault()
        val country = locale.country
        val language = locale.language
        val languageCode = "$language-$country"

        addHeader("Accept-Language", languageCode)

        val osName = Build.VERSION.SDK_INT.toString() + " " + Build.VERSION.CODENAME
        val userAgent = "Android/" + BuildConfigHelper.VERSION_NAME + " (" + context.packageName + ") " + osName
        addHeader("User-Agent", userAgent)
        addHeader("x-access-token", accessToken)

        authToken?.let { addHeader("x-auth-token", it) }
        uid?.let { addHeader("x-uid", it) }
        return this
    }
}