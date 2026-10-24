package com.anasdev.notificationsaver.data.repository

import com.anasdev.notificationsaver.data.local.NotificationDao
import com.anasdev.notificationsaver.data.mapper.toDomainModel
import com.anasdev.notificationsaver.data.mapper.toEntity
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<NotificationItem>> {
        return dao.getAllNotificationsByDate().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun saveNotification(notification: NotificationItem) {
        dao.insertNotification(notification.toEntity())
    }

    override suspend fun deleteNotification(notification: NotificationItem) {
        dao.deleteNotification(notification.toEntity())
    }

    override suspend fun deleteConversation(conversationKey: String) {
        val packageName = conversationKey.substringBefore(':')
        val title = conversationKey.substringAfter(':')
        dao.deleteConversationByPackageAndTitle(packageName, title)
    }

    override suspend fun deleteAppGroup(packageName: String) {
        dao.deleteAppNotifications(packageName)
    }

    override suspend fun clearAllNotifications() {
        dao.deleteAllNotifications()
    }

    override suspend fun isDuplicate(
        packageName: String,
        title: String,
        content: String,
        timestamp: Long
    ): Boolean {
        return dao.isNotificationDuplicate(packageName, title, content, timestamp)
    }
}
