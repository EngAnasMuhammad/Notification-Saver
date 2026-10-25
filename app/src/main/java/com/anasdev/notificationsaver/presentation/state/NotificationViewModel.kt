package com.anasdev.notificationsaver.presentation.state

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.notificationsaver.domain.model.AppNotificationGroup
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.usecase.NotificationUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupedUiState {
    data object Loading : GroupedUiState
    data class Success(val groups: List<AppNotificationGroup>) : GroupedUiState
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val useCases: NotificationUseCases,
) : ViewModel() {

    private val _expandedApps = MutableStateFlow<Set<String>>(emptySet())
    val expandedApps: StateFlow<Set<String>> = _expandedApps.asStateFlow()

    val groupedUiState: StateFlow<GroupedUiState> = useCases.getGroupedNotifications()
        .map { GroupedUiState.Success(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupedUiState.Loading,
        )

    fun toggleAppExpansion(packageName: String) {
        _expandedApps.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun getConversationHistory(conversationKey: String): Flow<List<NotificationItem>> {
        return useCases.getConversationHistory(conversationKey)
    }

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch {
            useCases.deleteNotification(notification)
        }
    }

    fun deleteConversation(conversationKey: String) {
        viewModelScope.launch {
            useCases.deleteConversation(conversationKey)
        }
    }

    fun deleteAppGroup(packageName: String) {
        _expandedApps.update { current -> current - packageName }
        viewModelScope.launch {
            useCases.deleteAppGroup(packageName)
        }
    }

    fun clearAllNotifications() {
        _expandedApps.value = emptySet()
        viewModelScope.launch {
            useCases.clearAllNotifications()
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
