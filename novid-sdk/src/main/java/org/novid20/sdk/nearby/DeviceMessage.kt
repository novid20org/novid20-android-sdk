/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:02
 */

package org.novid20.sdk.nearby

import com.google.android.gms.nearby.messages.Message
import com.google.gson.Gson
import java.nio.charset.StandardCharsets

internal class DeviceMessage private constructor(
    private val uuid: String,
    val messageBody: String = uuid
) {

    companion object {
        private val gson = Gson()

        fun newNearbyMessage(instanceId: String): Message {
            val deviceMessage = DeviceMessage(instanceId)
            return Message(gson.toJson(deviceMessage).toByteArray(StandardCharsets.UTF_8))
        }

        fun fromNearbyMessage(message: Message): DeviceMessage {
            val nearbyMessageString = String(message.content).trim { it <= ' ' }
            return gson.fromJson(
                String(nearbyMessageString.toByteArray(StandardCharsets.UTF_8)),
                DeviceMessage::class.java
            )
        }
    }
}