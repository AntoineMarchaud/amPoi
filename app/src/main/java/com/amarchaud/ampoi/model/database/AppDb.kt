package com.amarchaud.ampoi.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amarchaud.ampoi.model.entity.Favorite


@Database(entities = [Favorite::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun AppDao(): AppDao
}