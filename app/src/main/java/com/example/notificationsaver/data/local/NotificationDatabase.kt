package com.example.notificationsaver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.notificationsaver.data.model.NotificationData

@Database(entities = [NotificationData::class], version = 2, exportSchema = false)
abstract class NotificationDatabase: RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}