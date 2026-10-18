package com.anasdev.notificationsaver.data.service

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.usecase.SaveNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var saveNotificationUseCase: SaveNotificationUseCase

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Force the Android OS system framework to rebind to this service if disconnected
        requestRebind(ComponentName(this, NotificationListenerService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
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

        serviceScope.launch {
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
