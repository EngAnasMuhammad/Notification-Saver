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

/** Representing the home screen notification list state. */
sealed interface GroupedUiState {
    /** Initial state during Room query execution. */
    data object Loading : GroupedUiState
    /** State emitted when notifications are loaded and grouped. */
    data class Success(val groups: List<AppNotificationGroup>) : GroupedUiState
}

/**
 * Main ViewModel managing notification grouping, expand/collapse toggles,
 * conversation timeline history, and notification deletion operations.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val useCases: NotificationUseCases,
) : ViewModel() {

    private val _expandedApps = MutableStateFlow<Set<String>>(emptySet())
    /** StateFlow emitting the set of currently expanded application package names on the home screen. */
    val expandedApps: StateFlow<Set<String>> = _expandedApps.asStateFlow()

    /** StateFlow emitting the current grouped notification UI state. */
    val groupedUiState: StateFlow<GroupedUiState> = useCases.getGroupedNotifications()
        .map { GroupedUiState.Success(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupedUiState.Loading,
        )

    init {
        pruneOldNotifications()
    }

    /** Triggers auto-delete retention pruning on the domain layer. */
    fun pruneOldNotifications() {
        viewModelScope.launch {
            useCases.pruneOldNotifications()
        }
    }

    /** Toggles the expansion state of an application group card on the home screen. */
    fun toggleAppExpansion(packageName: String) {
        _expandedApps.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    /** Retrieves the conversation message history for a specific conversation key. */
    fun getConversationHistory(conversationKey: String): Flow<List<NotificationItem>> {
        return useCases.getConversationHistory(conversationKey)
    }

    /** Deletes an individual notification message from the database. */
    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch {
            useCases.deleteNotification(notification)
        }
    }

    /** Deletes all notification messages belonging to a conversation. */
    fun deleteConversation(conversationKey: String) {
        viewModelScope.launch {
            useCases.deleteConversation(conversationKey)
        }
    }

    /** Deletes all notification messages belonging to an application package. */
    fun deleteAppGroup(packageName: String) {
        _expandedApps.update { current -> current - packageName }
        viewModelScope.launch {
            useCases.deleteAppGroup(packageName)
        }
    }

    /** Clears all notification records from the local database. */
    fun clearAllNotifications() {
        _expandedApps.value = emptySet()
        viewModelScope.launch {
            useCases.clearAllNotifications()
        }
    }

    /** Checks if Notification Listener permission is granted by system settings. */
    fun checkNotificationPermission(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    /** Checks if the app is exempt from system battery optimization. */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Force-rebinds the notification listener service if permission is active. */
    fun rebindListenerService() {
        pruneOldNotifications()
        if (checkNotificationPermission()) {
            com.anasdev.notificationsaver.data.service.NotificationListenerService.forceRebind(context)
        }
    }
}
