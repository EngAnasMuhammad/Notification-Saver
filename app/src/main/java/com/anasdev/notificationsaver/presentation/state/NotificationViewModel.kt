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
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
) : ViewModel() {

    val notifications = getNotificationsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
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
