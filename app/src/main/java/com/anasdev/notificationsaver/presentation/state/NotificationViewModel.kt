package com.anasdev.notificationsaver.presentation.state

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.usecase.ClearAllNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.DeleteNotificationUseCase
import com.anasdev.notificationsaver.domain.usecase.GetNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getNotificationsUseCase: GetNotificationsUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase
) : ViewModel() {

    val notifications = getNotificationsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch {
            deleteNotificationUseCase(notification)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            clearAllNotificationsUseCase()
        }
    }

    fun checkNotificationPermission(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun rebindListenerService() {
        if (checkNotificationPermission()) {
            try {
                android.service.notification.NotificationListenerService.requestRebind(
                    android.content.ComponentName(context, com.anasdev.notificationsaver.data.service.NotificationListenerService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }
}
