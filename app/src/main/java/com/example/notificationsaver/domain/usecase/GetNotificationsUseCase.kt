package com.example.notificationsaver.domain.usecase

import com.example.notificationsaver.domain.model.NotificationItem
import com.example.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(): Flow<List<NotificationItem>> {
        return repository.getAllNotifications()
    }
}
