/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:36
 */

package org.novid20.sdk.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.novid20.sdk.api.models.AnalyticsEvent

@Entity
internal data class ContactEntryEntity(
    @PrimaryKey val id: Long? = null,
    val time: Long,
    val user: String,
    val source: String?,
    var duration: Long? = null,
    val distance: Int? = null,
    val rssi: Int? = null,
    val background: Boolean = false
) {

    companion object {
        fun from(entry: ContactEntry): ContactEntryEntity {
            return ContactEntryEntity(
                id = entry.id,
                user = entry.user,
                time = entry.time,
                source = entry.source,
                duration = entry.duration,
                distance = entry.distance,
                rssi = entry.rssi,
                background = entry.background
            )
        }
    }

    fun toContactEntry(): ContactEntry {
        return ContactEntry(
            id = id,
            time = time,
            user = user,
            source = source,
            duration = duration,
            distance = distance,
            rssi = rssi,
            background = background
        )
    }
}

@Entity
internal data class LocationEntryEntity(
    @PrimaryKey val time: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
) {

    companion object {
        fun from(entry: LocationEntry): LocationEntryEntity {
            return LocationEntryEntity(
                entry.time,
                entry.latitude,
                entry.longitude,
                entry.accuracy
            )
        }
    }

    fun toLocationEntry(): LocationEntry {
        return LocationEntry(time, latitude, longitude, accuracy)
    }
}

@Entity
internal data class AnalyticsEventEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String,
    val timestamp: Long
) {

    companion object {
        fun from(analyticsEvent: AnalyticsEvent) = AnalyticsEventEntryEntity(
            analyticsEvent.id,
            analyticsEvent.name,
            analyticsEvent.value,
            analyticsEvent.timestamp
        )
    }

    fun toAnalyticsEventEntry() = AnalyticsEventEntry(id, name, value, timestamp)
}