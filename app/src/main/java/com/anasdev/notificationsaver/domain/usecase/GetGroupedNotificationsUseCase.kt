package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.AppNotificationGroup
import com.anasdev.notificationsaver.domain.model.ConversationGroup
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGroupedNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(): Flow<List<AppNotificationGroup>> {
        return repository.getAllNotifications().map { flatList ->
            groupNotifications(flatList)
        }
    }

    private fun resolveSenderName(item: NotificationItem): String {
        val rawTitle = item.conversationTitle?.takeIf { it.isNotBlank() }
            ?: item.title.takeIf { it.isNotBlank() }
            ?: "General"

        val cleanTitle = if (item.packageName == "com.instagram.android" && rawTitle.contains(":")) {
            rawTitle.substringAfterLast(":").trim()
        } else {
            rawTitle.trim()
        }

        return cleanTitle.ifBlank { "General" }
    }

    private fun groupNotifications(items: List<NotificationItem>): List<AppNotificationGroup> {
        if (items.isEmpty()) return emptyList()

        return items.groupBy { it.packageName }
            .map { (packageName, appItems) ->
                val conversationGroups = appItems.groupBy { item ->
                    val cleanDisplay = resolveSenderName(item)
                    "$packageName:$cleanDisplay"
                }.map { (key, convItems) ->
                    val sortedConvItems = convItems.sortedByDescending { it.timestamp }
                    val latestItem = sortedConvItems.first()
                    val displayTitle = resolveSenderName(latestItem)

                    val maxTimestamp = latestItem.timestamp
                    val latestMsg = latestItem.content
                    val avatarPath = sortedConvItems.firstOrNull { !it.senderIconPath.isNullOrBlank() }?.senderIconPath

                    ConversationGroup(
                        conversationKey = key,
                        conversationTitle = displayTitle,
                        latestTimestamp = maxTimestamp,
                        latestMessage = latestMsg,
                        senderIconPath = avatarPath,
                        notifications = sortedConvItems
                    )
                }.sortedByDescending { it.latestTimestamp }

                val appMaxTimestamp = conversationGroups.firstOrNull()?.latestTimestamp ?: 0L
                val totalCount = appItems.size

                AppNotificationGroup(
                    packageName = packageName,
                    latestTimestamp = appMaxTimestamp,
                    totalNotificationsCount = totalCount,
                    conversations = conversationGroups
                )
            }.sortedByDescending { it.latestTimestamp }
    }
}
