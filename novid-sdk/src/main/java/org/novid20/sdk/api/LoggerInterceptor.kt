/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:25
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:48
 */

package org.novid20.sdk.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import org.novid20.sdk.BuildConfig
import org.novid20.sdk.Logger
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal open class LoggerInterceptor(val context: Context) : Interceptor {

    companion object {
        private val TAG = Logger.makeLogTag("LoggerInterceptor")
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val requestUrl = request.url
        val requestBody = request.body
        val contentType = requestBody?.contentType()
        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8)
            ?: StandardCharsets.UTF_8
        val requestBuffer = Buffer()
        requestBody?.writeTo(requestBuffer)
        val requestString = requestBuffer.readString(charset)

        val response: Response
        var responseString: String = ""
        try {
            response = chain.proceed(request)

            val responseBody = response.body!!
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body.
            val responseBuffer = source.buffer
            responseString = responseBuffer.clone().readString(charset)

        } catch (e: Exception) {
            Logger.warn(TAG, "<-- HTTP FAILED: $e")
            responseString = e.message.toString()
            throw e
        } finally {
            logResponse(requestUrl.toString(), requestString, responseString)
        }

        return response
    }

    private fun logResponse(url: String, requestString: String, responseString: String) {
        if (BuildConfig.DEBUG) {
            Logger.verbose(
                TAG, "URL: " + url + "\n" +
                    "REQUEST: " + requestString + "\n" +
                    "RESPONSE: " + responseString
            )
        }
    }
}