package com.anasdev.notificationsaver.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "notification_saver_background"

        /**
         * Component Toggle Trick: Force-restarts the Android system NotificationManagerService
         * binding by toggling the service component state in PackageManager.
         */
        fun forceRebind(context: Context) {
            try {
                val component = ComponentName(context, NotificationListenerService::class.java)
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureActiveScope()
        startForegroundService()
    }

    private fun ensureActiveScope() {
        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
        }
    }

    private fun startForegroundService() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Notification Listener",
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Saver Active")
                .setContentText("Saving incoming notifications in background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ensureActiveScope()
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ensureActiveScope()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        forceRebind(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        forceRebind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        ensureActiveScope()

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
