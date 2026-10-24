package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.ConversationSummary
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentConversationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(): Flow<List<ConversationSummary>> {
        return repository.getAllNotifications()
            .map { items ->
                groupIntoConversations(items)
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

    private fun groupIntoConversations(items: List<NotificationItem>): List<ConversationSummary> {
        if (items.isEmpty()) return emptyList()

        return items.groupBy { item ->
            val cleanDisplay = resolveSenderName(item)
            "${item.packageName}:$cleanDisplay"
        }.map { (key, groupItems) ->
            val sorted = groupItems.sortedByDescending { it.timestamp }
            val latest = sorted.first()
            val displayTitle = resolveSenderName(latest)
            val avatarPath = sorted.firstOrNull { !it.senderIconPath.isNullOrBlank() }?.senderIconPath

            ConversationSummary(
                conversationKey = key,
                packageName = latest.packageName,
                displayTitle = displayTitle,
                latestMessageContent = latest.content,
                latestTimestamp = latest.timestamp,
                totalCount = groupItems.size,
                senderIconPath = avatarPath
            )
        }.sortedByDescending { it.latestTimestamp }
    }
}
