/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:24
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:13
 */

package org.novid20.sdk.analytics

import android.content.Context
import android.text.format.DateUtils
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import java.util.concurrent.TimeUnit

class AnalyticsSyncWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Logger.debug(TAG, "Starting analytics sync worker.")
        val sdk = NovidSdk.getInstance()

        val lastSuccessfulSync = sdk.getConfig().lastSuccessfulSync
        if (!shouldSync(sdk.getConfig().lastSuccessfulSync)) {
            Logger.debug(
                TAG, "Cancelling sync because not enough time passed since last successful sync. " +
                        "Last sync time: " + DateUtils.getRelativeTimeSpanString(lastSuccessfulSync) +
                        " ($lastSuccessfulSync)."
            )
            return Result.success()
        }

        val success = sdk.getRepository().syncAllAnalyticsData()
        return if (success) {
            Logger.debug(TAG, "Sync completed successfully.")
            sdk.getConfig().lastSuccessfulSync = System.currentTimeMillis()
            Result.success()
        } else {
            Logger.warn(TAG, "Sync failed, rescheduling.")
            Result.retry()
        }
    }

    private fun shouldSync(lastSyncTime: Long): Boolean {
        return lastSyncTime == 0L || System.currentTimeMillis() - MIN_SYNC_DELAY > lastSyncTime
    }

    companion object {
        private const val TAG = "AnalyticsSyncWorker"

        /**
         * Min time in between consecutive requests
         */
        private const val MIN_SYNC_DELAY: Long = 60 * 60 * 1000 // 1 hour

        /**
         * Initial sync delay, in milliseconds
         */
        private const val INITIAL_SYNC_DELAY: Long = 10 * 1000 // 10 sec

        /**
         * Call this function to launch the periodic AnalyticsSyncWorker.
         * If this worker is already scheduled, then the current instance will be replaced by the new one.
         * The first run of this worker will happen after a delay of [initialSyncDelay] ms.
         */
        fun schedule(context: Context, initialSyncDelay: Long = INITIAL_SYNC_DELAY) {
            Logger.debug(TAG, "Launching AnalyticsSyncWorker with initial delay of $initialSyncDelay ms")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val work = PeriodicWorkRequest.Builder(AnalyticsSyncWorker::class.java, 1L, TimeUnit.DAYS)
                .addTag(TAG)
                .setConstraints(constraints)
                .setInitialDelay(initialSyncDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, work)
        }
    }
}