package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: NotificationItem) {
        repository.deleteNotification(notification)
    }
}
