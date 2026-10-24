package com.anasdev.notificationsaver.domain.model

data class ConversationGroup(
    val conversationKey: String,
    val conversationTitle: String,
    val latestTimestamp: Long,
    val latestMessage: String,
    val senderIconPath: String? = null,
    val notifications: List<NotificationItem>
)
