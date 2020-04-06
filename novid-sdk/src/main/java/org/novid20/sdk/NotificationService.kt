/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:35
 */

package org.novid20.sdk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

private const val TAG = "NotificationService"
internal const val NOTIFICATION_ID = 13
internal const val NOTIFICATION_CHANNEL = "NOVID_CHANNEL"
const val NOTIFICATION_MESSAGE_ID = 14
const val NOTIFICATION_MESSAGE_CHANNEL = "MESSAGE_CHANNEL"

class NotificationService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    private fun createNotification(context: Context): Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Novid20" // The user-visible name of the channel.
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL, name, importance)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = intent?.let {
            PendingIntent.getActivity(this, 0, intent, 0)
        }

        val contentTitle = context.getString(R.string.notification_title)

        val channelId = NOTIFICATION_CHANNEL
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(contentTitle)
            .setShowWhen(false)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentText(context.getString(R.string.notification_content))
            .setChannelId(channelId)

        if (contentIntent != null) { // unit test may have null values
            builder.setContentIntent(contentIntent)
        }

        return builder.build()
    }

    private fun showNotification() {
        val notification = createNotification(this)
        startForeground(NOTIFICATION_ID, notification)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {

        fun start(context: Context) {
            val serviceIntent = Intent(context, NotificationService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stop(context: Context) {
            val serviceIntent = Intent(context, NotificationService::class.java)
            context.stopService(serviceIntent)
        }
    }
}