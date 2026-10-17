package com.example.notificationsaver.domain.usecase

import com.example.notificationsaver.domain.repository.NotificationRepository
import javax.inject.Inject

class ClearAllNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke() {
        repository.clearAllNotifications()
    }
}
