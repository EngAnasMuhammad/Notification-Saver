package com.anasdev.notificationsaver.domain.model

data class NotificationItem(
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val notificationKey: String,
    val conversationTitle: String? = null,
    val senderIconPath: String? = null
)
