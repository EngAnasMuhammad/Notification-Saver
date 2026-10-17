package com.example.notificationsaver.domain.usecase

import com.example.notificationsaver.domain.model.NotificationItem
import com.example.notificationsaver.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: NotificationItem) {
        repository.deleteNotification(notification)
    }
}
