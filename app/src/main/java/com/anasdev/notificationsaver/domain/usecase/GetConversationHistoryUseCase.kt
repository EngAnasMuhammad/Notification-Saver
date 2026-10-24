package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(conversationKey: String): Flow<List<NotificationItem>> {
        return repository.getAllNotifications()
            .map { items ->
                items.filter { item ->
                    val resolvedName = resolveSenderName(item)
                    val rawDisplay = item.conversationTitle?.takeIf { it.isNotBlank() }
                        ?: item.title.ifBlank { "General" }
                    val itemKeyResolved = "${item.packageName}:$resolvedName"
                    val itemKeyRaw = "${item.packageName}:$rawDisplay"

                    itemKeyResolved == conversationKey || itemKeyRaw == conversationKey
                }.sortedByDescending { it.timestamp }
            }
            .flowOn(Dispatchers.Default)
    }

    private fun resolveSenderName(item: NotificationItem): String {
        if (item.content.contains(":")) {
            val candidate = item.content.substringBefore(":").trim()
            if (candidate.isNotBlank() && candidate.length < 35 && !candidate.equals("General", ignoreCase = true)) {
                return candidate
            }
        }

        val rawTitle = item.conversationTitle?.takeIf { it.isNotBlank() }
            ?: item.title.takeIf { it.isNotBlank() }
            ?: "General"

        val cleanTitle = if (rawTitle.contains(":")) rawTitle.substringAfterLast(":").trim() else rawTitle.trim()
        return cleanTitle.ifBlank { "General" }
    }
}
