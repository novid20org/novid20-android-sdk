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
internal interface LocationDao {

    @Query("SELECT * FROM LocationEntryEntity WHERE time > :since ORDER BY time DESC")
    fun getAll(since: Long = 0): List<LocationEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: LocationEntryEntity): Long

    /**
     * Deletes entries that are older than [timestamp]
     */
    @Query("DELETE FROM LocationEntryEntity WHERE time < :timestamp")
    fun deleteEntriesOlderThan(timestamp: Long): Int

    @Query("DELETE FROM LocationEntryEntity")
    fun nukeTable()

}