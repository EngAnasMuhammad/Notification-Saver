package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(conversationKey: String): Flow<List<NotificationItem>> {
        val packageName = conversationKey.substringBefore(':')
        val title = conversationKey.substringAfter(':')
        return repository.getConversationHistory(packageName, title)
            .flowOn(Dispatchers.Default)
    }
}
