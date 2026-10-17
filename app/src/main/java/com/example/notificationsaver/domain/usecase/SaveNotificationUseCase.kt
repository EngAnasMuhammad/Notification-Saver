package com.example.notificationsaver.domain.usecase

import com.example.notificationsaver.domain.model.NotificationItem
import com.example.notificationsaver.domain.repository.NotificationRepository
import javax.inject.Inject

class SaveNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: NotificationItem) {
        if (!repository.isDuplicate(
                packageName = notification.packageName,
                title = notification.title,
                content = notification.content,
                timestamp = notification.timestamp
            )
        ) {
            repository.saveNotification(notification)
        }
    }
}
