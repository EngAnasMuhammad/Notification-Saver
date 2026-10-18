package com.anasdev.notificationsaver.domain.repository

import com.anasdev.notificationsaver.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationItem>>
    suspend fun saveNotification(notification: NotificationItem)
    suspend fun deleteNotification(notification: NotificationItem)
    suspend fun clearAllNotifications()
    suspend fun isDuplicate(packageName: String, title: String, content: String, timestamp: Long): Boolean
}
