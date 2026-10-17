package com.example.notificationsaver.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName", "timestamp"])
    ]
)
data class NotificationData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val notificationKey: String
)
