package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(conversationKey: String) {
        repository.deleteConversation(conversationKey)
    }
}
