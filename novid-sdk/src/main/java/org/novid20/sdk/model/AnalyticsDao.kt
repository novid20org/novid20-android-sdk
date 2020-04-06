/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface AnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entries: AnalyticsEventEntryEntity)

    @Query("SELECT * FROM AnalyticsEventEntryEntity ORDER BY timestamp DESC")
    fun getAllEvents(): List<AnalyticsEventEntryEntity>

    @Query("DELETE FROM AnalyticsEventEntryEntity")
    fun nukeEventsTable()
}