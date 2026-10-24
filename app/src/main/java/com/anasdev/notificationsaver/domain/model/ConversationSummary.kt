package com.anasdev.notificationsaver.domain.model

data class ConversationSummary(
    val conversationKey: String,
    val packageName: String,
    val displayTitle: String,
    val latestMessageContent: String,
    val latestTimestamp: Long,
    val totalCount: Int,
    val senderIconPath: String? = null
)
