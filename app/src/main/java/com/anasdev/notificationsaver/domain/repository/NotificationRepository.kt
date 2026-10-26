package com.anasdev.notificationsaver.domain.repository

import com.anasdev.notificationsaver.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationItem>>
    suspend fun saveNotification(notification: NotificationItem)
    suspend fun deleteNotification(notification: NotificationItem)
    suspend fun deleteConversation(conversationKey: String)
    suspend fun deleteAppGroup(packageName: String)
    suspend fun clearAllNotifications()
    suspend fun pruneOldNotifications(cutoffTimestamp: Long)
    suspend fun isDuplicate(packageName: String, title: String, content: String, timestamp: Long): Boolean
}
