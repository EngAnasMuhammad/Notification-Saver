package com.anasdev.notificationsaver.domain.model

data class AppNotificationGroup(
    val packageName: String,
    val latestTimestamp: Long,
    val totalNotificationsCount: Int,
    val conversations: List<ConversationGroup>
)
