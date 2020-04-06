/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.nearby

import android.app.Activity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Distance
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.MessagesClient
import com.google.android.gms.nearby.messages.MessagesOptions
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.google.android.gms.nearby.messages.PublishCallback
import com.google.android.gms.nearby.messages.PublishOptions
import com.google.android.gms.nearby.messages.Strategy
import com.google.android.gms.nearby.messages.SubscribeOptions
import org.novid20.sdk.Logger
import org.novid20.sdk.NovidSdk
import org.novid20.sdk.TECHNOLOGY_NEARBY_MESSAGES


private const val TAG: String = "NearbyMessagesManager"

internal class NearbyMessagesManager(private val sdk: NovidSdk) {

    private lateinit var nearbyMessage: Message
    private var messageListener: MessageListener

    private var messagesClient: MessagesClient? = null

    init {
        messageListener = object : MessageListener() {

            override fun onFound(message: Message) {
                super.onFound(message)
                Logger.debug(TAG, "onFound: $message")

                val deviceMessage = DeviceMessage.fromNearbyMessage(message)
                val userId = deviceMessage.messageBody
                sdk.getRepository().contactDetected(userId, source = TECHNOLOGY_NEARBY_MESSAGES)
            }

            override fun onLost(message: Message) {
                super.onLost(message)
                Logger.debug(TAG, "Lost: " + String(message.content))
            }

            override fun onDistanceChanged(message: Message?, distance: Distance?) {
                super.onDistanceChanged(message, distance)
                Logger.verbose(TAG, "Distance changed, message: $message, new distance: $distance");
            }
        }
    }

    private var started = false

    private val strategy = Strategy.BLE_ONLY

    private val subscribeOptions: SubscribeOptions = SubscribeOptions.Builder()
        .setStrategy(strategy)
        .build()

    private var publishOptions: PublishOptions = PublishOptions.Builder()
        .setStrategy(Strategy.BLE_ONLY)
        .setCallback(object : PublishCallback() {
            override fun onExpired() {
                super.onExpired()
                Logger.warn(TAG, "onExpired")
                started = false
            }
        })
        .build()

    fun start(activity: Activity) {
        if (started) return
        Logger.debug(TAG, "start")
        started = true

        val userId = NovidSdk.getInstance().getConfig().userId!!
        nearbyMessage = DeviceMessage.newNearbyMessage(userId)

        val messagesOptions = MessagesOptions.Builder()
            .setPermissions(NearbyPermissions.BLE)
            .build()
        messagesClient = Nearby.getMessagesClient(activity, messagesOptions)
        messagesClient
            ?.publish(nearbyMessage, publishOptions)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    Logger.info(TAG, "Published successfully.")
                } else {
                    Logger.warn(TAG, "Could not publish")
                }
            }
        messagesClient
            ?.subscribe(messageListener, subscribeOptions)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    Logger.info(TAG, "Subscribed successfully.")
                } else {
                    Logger.warn(TAG, "Could not subscribe")
                }
            }
    }

    fun stop() {
        Logger.debug(TAG, "stop")
        started = false

        messagesClient?.unsubscribe(messageListener)
        if (::nearbyMessage.isInitialized) {
            messagesClient?.unpublish(nearbyMessage)
        }
    }
}