/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 17:36
 */

package org.novid20.sdk.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntryEntity::class,
        ContactEntryEntity::class,
        AnalyticsEventEntryEntity::class
    ],
    version = 10,
    exportSchema = false
)
internal abstract class NovidDatabase : RoomDatabase() {

    companion object {

        private var INSTANCE: NovidDatabase? = null

        private const val name = "novid.db"

        fun getDatabase(context: Context): NovidDatabase {
            if (INSTANCE == null) {

                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    NovidDatabase::class.java,
                    name
                ).fallbackToDestructiveMigration()
                INSTANCE = builder.build()
            }
            return INSTANCE as NovidDatabase
        }
    }

    internal abstract fun locationDao(): LocationDao

    internal abstract fun contactDao(): ContactDao

    internal abstract fun analyticsDao(): AnalyticsDao

}