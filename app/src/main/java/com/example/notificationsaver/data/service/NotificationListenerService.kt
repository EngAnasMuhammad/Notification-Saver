package com.example.notificationsaver.data.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.notificationsaver.domain.model.NotificationItem
import com.example.notificationsaver.domain.usecase.SaveNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var saveNotificationUseCase: SaveNotificationUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val notification = sbn.notification
        val packageName = sbn.packageName
        val postTime = sbn.postTime
        val notificationKey = sbn.key ?: ""

        val rawTitle = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val rawBigText = notification.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val title = rawTitle.trim()
        val content = if (rawBigText.isNotBlank()) rawBigText.trim() else rawText.trim()

        val groupSummary = NotificationCompat.isGroupSummary(notification)

        scope.launch {
            if (title.isBlank() && content.isBlank()) return@launch

            if (!sbn.isOngoing && !groupSummary && applicationContext.packageName != packageName) {
                val item = NotificationItem(
                    packageName = packageName,
                    title = title,
                    content = content,
                    timestamp = postTime,
                    notificationKey = notificationKey
                )
                saveNotificationUseCase(item)
            }
        }
    }
}
