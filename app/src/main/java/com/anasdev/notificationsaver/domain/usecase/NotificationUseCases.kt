package com.anasdev.notificationsaver.domain.usecase

import javax.inject.Inject

data class NotificationUseCases @Inject constructor(
    val getGroupedNotifications: GetGroupedNotificationsUseCase,
    val getRecentConversations: GetRecentConversationsUseCase,
    val getConversationHistory: GetConversationHistoryUseCase,
    val deleteNotification: DeleteNotificationUseCase,
    val deleteConversation: DeleteConversationUseCase,
    val deleteAppGroup: DeleteAppGroupUseCase,
    val clearAllNotifications: ClearAllNotificationsUseCase,
    val pruneOldNotifications: PruneOldNotificationsUseCase,
)
