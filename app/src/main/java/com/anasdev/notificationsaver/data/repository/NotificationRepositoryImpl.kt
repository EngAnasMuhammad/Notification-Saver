package com.anasdev.notificationsaver.data.repository

import com.anasdev.notificationsaver.data.local.NotificationDao
import com.anasdev.notificationsaver.data.mapper.toDomainModel
import com.anasdev.notificationsaver.data.mapper.toEntity
import com.anasdev.notificationsaver.data.util.AvatarStorageManager
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
    private val avatarStorageManager: AvatarStorageManager,
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<NotificationItem>> {
        return dao.getAllNotificationsByDate().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getConversationHistory(packageName: String, title: String): Flow<List<NotificationItem>> {
        return dao.getConversationHistoryByPackageAndTitle(packageName, title).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun saveNotification(notification: NotificationItem) {
        dao.insertNotification(notification.toEntity())
    }

    override suspend fun deleteNotification(notification: NotificationItem) {
        dao.deleteNotification(notification.toEntity())
        notification.senderIconPath?.let { avatarStorageManager.deleteAvatarFile(it) }
    }

    override suspend fun deleteConversation(conversationKey: String) {
        val packageName = conversationKey.substringBefore(':')
        val title = conversationKey.substringAfter(':')
        val avatarPaths = dao.getAvatarPathsForConversation(packageName, title)
        dao.deleteConversationByPackageAndTitle(packageName, title)
        avatarStorageManager.deleteAvatarFiles(avatarPaths)
    }

    override suspend fun deleteAppGroup(packageName: String) {
        val avatarPaths = dao.getAvatarPathsForApp(packageName)
        dao.deleteAppNotifications(packageName)
        avatarStorageManager.deleteAvatarFiles(avatarPaths)
    }

    override suspend fun clearAllNotifications() {
        dao.deleteAllNotifications()
        avatarStorageManager.clearAllAvatars()
    }

    override suspend fun pruneOldNotifications(cutoffTimestamp: Long) {
        val avatarPaths = dao.getAvatarPathsOlderThan(cutoffTimestamp)
        dao.deleteNotificationsOlderThan(cutoffTimestamp)
        avatarStorageManager.deleteAvatarFiles(avatarPaths)
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
