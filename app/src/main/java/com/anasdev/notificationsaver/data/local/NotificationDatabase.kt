package com.anasdev.notificationsaver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anasdev.notificationsaver.data.model.NotificationData

/**
 * Room Database abstract class managing SQLite database creation and DAO access (Version 4).
 */
@Database(entities = [NotificationData::class], version = 4, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    /** Returns the DAO instance for querying notification tables. */
    abstract fun notificationDao(): NotificationDao
}
