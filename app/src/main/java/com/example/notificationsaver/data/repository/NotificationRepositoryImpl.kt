package com.example.notificationsaver.data.repository

import com.example.notificationsaver.data.local.NotificationDao
import com.example.notificationsaver.data.mapper.toDomainModel
import com.example.notificationsaver.data.mapper.toEntity
import com.example.notificationsaver.domain.model.NotificationItem
import com.example.notificationsaver.domain.repository.NotificationRepository
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
