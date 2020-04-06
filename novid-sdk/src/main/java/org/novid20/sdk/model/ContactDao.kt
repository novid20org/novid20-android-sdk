/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 12:10
 */

package org.novid20.sdk.model

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
internal interface ContactDao {

    @Query("SELECT * FROM ContactEntryEntity WHERE time > :since ORDER BY time DESC")
    fun getAllData(since: Long = 0): List<ContactEntryEntity>

    @Query("SELECT * FROM ContactEntryEntity ORDER BY time DESC")
    fun getAllContinuously(): LiveData<List<ContactEntryEntity>>

    @Query("SELECT * FROM ContactEntryEntity WHERE time > :since GROUP BY user ORDER BY time DESC, duration DESC")
    fun getMostRecentContacts(since: Long = 0): List<ContactEntryEntity>

    @Query("SELECT user FROM ContactEntryEntity WHERE user = :user ORDER BY time DESC")
    fun getContactsWith(user: List<String>): List<String>

    @Query("SELECT * FROM ContactEntryEntity WHERE user = :user ORDER BY time DESC")
    fun getContactsWith(user: String): List<ContactEntryEntity>

    @Query("SELECT user FROM ContactEntryEntity GROUP BY user ORDER BY time DESC")
    fun getContactedUsers(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: ContactEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: ContactEntryEntity): Long

    @Query("UPDATE ContactEntryEntity SET duration = :duration WHERE user=:userId AND time=:timestamp")
    fun updateDuration(timestamp: Long, userId: String, duration: Long): Int

    @Query("UPDATE ContactEntryEntity SET duration = null")
    fun resetDuration(): Int

    @Delete
    fun delete(contact: ContactEntryEntity?)

    @Query("DELETE FROM ContactEntryEntity")
    fun nukeTable()
}