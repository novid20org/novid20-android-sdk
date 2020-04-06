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

    @Query("SELECT * FROM LocationEntryEntity ORDER BY time DESC")
    fun getAll(): List<LocationEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: LocationEntryEntity): Long

    @Query("DELETE FROM LocationEntryEntity")
    fun nukeTable()

}