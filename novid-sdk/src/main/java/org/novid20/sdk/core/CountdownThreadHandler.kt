/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 16:12
 */

package org.novid20.sdk.core

import org.novid20.sdk.Logger
import kotlin.math.max

internal open class CountdownThreadHandler(val runnable: Runnable) {

    private val TAG = Logger.makeLogTag(this@CountdownThreadHandler::class.java)

    private var thread: Thread? = null

    private var lastStart: Long = 0
    private var lastDuration: Long = 0

    fun extend(duration: Long) {
        interrupt()

        val current = System.currentTimeMillis()
        val remaining = (lastStart + duration) - current
        val countdownTime = max(remaining, duration)
        Logger.verbose(TAG, "Extend about: $countdownTime")
        Logger.verbose(TAG, "Remaining about: $remaining")

        val thread = object : Thread() {
            override fun run() {
                try {
                    sleep(countdownTime)
                    runnable.run()
                } catch (iox: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
        thread.start()
        lastStart = current
        lastDuration = duration
        CountdownThread@ this.thread = thread
    }

    private fun interrupt() {
        thread?.let {
            val alive: Boolean = it.isAlive
            if (alive) {
                it.interrupt()
            }
        }
    }
}