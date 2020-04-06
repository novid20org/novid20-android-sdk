/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 16:59
 */

package org.novid20.sdk

import android.util.Log

class Logger {

    interface NovidLogger {
        fun log(level: Int, tag: String, message: String? = null, error: Throwable? = null)
    }

    companion object {

        private const val LOG_PREFIX = ""
        private const val LOG_PREFIX_LENGTH = LOG_PREFIX.length
        private const val MAX_LOG_TAG_LENGTH = 23

        private const val VERBOSE = 0
        private const val DEBUG = 1
        private const val INFO = 2
        private const val WARNING = 3
        private const val ERROR = 4

        private val mLogLevel = if (BuildConfig.DEBUG) VERBOSE else ERROR

        private var NOVID_LOGGER: NovidLogger? = null
        fun setLogger(novidLogger: NovidLogger) {
            NOVID_LOGGER = novidLogger
        }

        /**
         * Don't use this when obfuscating class names!
         */
        fun makeLogTag(cls: Class<*>): String {
            return makeLogTag(cls.simpleName)
        }

        fun makeLogTag(str: String): String {
            return if (str.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
                LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
            } else LOG_PREFIX + str
        }

        fun verbose(tag: String, message: String?) {
            if (mLogLevel > VERBOSE) return
            message?.let { Log.v(tag, it) }
            NOVID_LOGGER?.log(VERBOSE, tag, message)
        }

        fun debug(tag: String, message: String?) {
            if (mLogLevel > DEBUG) return
            message?.let { Log.d(tag, it) }
            NOVID_LOGGER?.log(DEBUG, tag, message)
        }

        fun info(tag: String, message: String?) {
            if (mLogLevel > INFO) return
            message?.let { Log.i(tag, it) }
            NOVID_LOGGER?.log(INFO, tag, message)
        }

        fun warn(tag: String, message: String?, t: Throwable? = null) {
            if (mLogLevel > WARNING) return
            message?.let { Log.w(tag, it, t) } ?: run { Log.w(tag, t?.javaClass?.simpleName, t) }
            NOVID_LOGGER?.log(WARNING, tag, message, t)
        }

        fun error(tag: String, message: String?, t: Throwable? = null) {
            if (mLogLevel > ERROR) return
            message?.let { Log.e(tag, it, t) } ?: run { Log.e(tag, t?.javaClass?.simpleName, t) }
            NOVID_LOGGER?.log(ERROR, tag, message, t)
        }
    }
}