package com.anasdev.notificationsaver.presentation.state

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.notificationsaver.domain.model.AppNotificationGroup
import com.anasdev.notificationsaver.domain.model.ConversationSummary
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.usecase.ClearAllNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.DeleteAppGroupUseCase
import com.anasdev.notificationsaver.domain.usecase.DeleteConversationUseCase
import com.anasdev.notificationsaver.domain.usecase.DeleteNotificationUseCase
import com.anasdev.notificationsaver.domain.usecase.GetConversationHistoryUseCase
import com.anasdev.notificationsaver.domain.usecase.GetGroupedNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.GetRecentConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getGroupedNotificationsUseCase: GetGroupedNotificationsUseCase,
    getRecentConversationsUseCase: GetRecentConversationsUseCase,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val deleteAppGroupUseCase: DeleteAppGroupUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
) : ViewModel() {

    private val _expandedApps = MutableStateFlow<Set<String>>(emptySet())
    val expandedApps: StateFlow<Set<String>> = _expandedApps.asStateFlow()

    private val _selectedConversationKey = MutableStateFlow<String?>(null)
    val selectedConversationKey: StateFlow<String?> = _selectedConversationKey.asStateFlow()

    val groupedNotifications: StateFlow<List<AppNotificationGroup>> = getGroupedNotificationsUseCase()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val recentConversations: StateFlow<List<ConversationSummary>> = getRecentConversationsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun toggleAppExpansion(packageName: String) {
        _expandedApps.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun openConversation(conversationKey: String) {
        _selectedConversationKey.value = conversationKey
    }

    fun closeConversation() {
        _selectedConversationKey.value = null
    }

    fun getConversationHistory(conversationKey: String): Flow<List<NotificationItem>> {
        return getConversationHistoryUseCase(conversationKey)
    }

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch {
            deleteNotificationUseCase(notification)
        }
    }

    fun deleteConversation(conversationKey: String) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationKey)
        }
    }

    fun deleteAppGroup(packageName: String) {
        _expandedApps.update { current -> current - packageName }
        viewModelScope.launch {
            deleteAppGroupUseCase(packageName)
        }
    }

    fun clearAllNotifications() {
        _expandedApps.value = emptySet()
        _selectedConversationKey.value = null
        viewModelScope.launch {
            clearAllNotificationsUseCase()
        }
    }

    fun checkNotificationPermission(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun rebindListenerService() {
        if (checkNotificationPermission()) {
            com.anasdev.notificationsaver.data.service.NotificationListenerService.forceRebind(context)
        }
    }
}
