package com.anasdev.notificationsaver.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.anasdev.notificationsaver.data.util.AvatarStorageManager
import com.anasdev.notificationsaver.domain.model.NotificationItem
import com.anasdev.notificationsaver.domain.usecase.GetBlacklistedAppsUseCase
import com.anasdev.notificationsaver.domain.usecase.SaveNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var saveNotificationUseCase: SaveNotificationUseCase

    @Inject
    lateinit var avatarStorageManager: AvatarStorageManager

    @Inject
    lateinit var getBlacklistedAppsUseCase: GetBlacklistedAppsUseCase

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    // In-memory debouncer cache: maps Objects.hash(packageName, title, content) -> timestamp
    private val recentNotificationsCache = ConcurrentHashMap<Int, Long>()

    // In-memory blacklist cache: set of package names toggled off by user
    private val blockedPackagesCache = ConcurrentHashMap.newKeySet<String>()

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
                    PackageManager.DONT_KILL_APP,
                )
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
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
        observeBlacklist()
    }

    private fun ensureActiveScope() {
        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
            observeBlacklist()
        }
    }

    private fun observeBlacklist() {
        serviceScope.launch {
            getBlacklistedAppsUseCase().collect { blockedSet ->
                blockedPackagesCache.clear()
                blockedPackagesCache.addAll(blockedSet)
            }
        }
    }

    private fun startForegroundService() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Notification Listener",
                NotificationManager.IMPORTANCE_MIN,
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
        syncActiveNotifications()
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
        processAndSaveNotification(sbn)
    }

    /**
     * Queries all active notifications currently sitting in the status bar
     * and catches any notifications missed while the listener or app was offline.
     */
    private fun syncActiveNotifications() {
        try {
            val activeNotifications = activeNotifications ?: return
            for (sbn in activeNotifications) {
                processAndSaveNotification(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractSenderName(
        notification: Notification,
        rawTitle: String,
        rawText: String,
        rawBigText: String,
        rawConversationTitle: String?
    ): String {
        // 1. NotificationCompat MessagingStyle
        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null) {
                val lastMsg = messagingStyle.messages.lastOrNull()
                val msgSender = lastMsg?.person?.name?.toString()?.trim()
                    ?: lastMsg?.sender?.toString()?.trim()
                if (!msgSender.isNullOrBlank() && !msgSender.equals("General", ignoreCase = true)) {
                    return msgSender
                }
            }
        } catch (_: Exception) {}

        // 2. Person object from EXTRA_MESSAGING_PERSON
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val person = getPersonFromBundle(notification.extras)
                val personName = person?.name?.toString()?.trim()
                if (!personName.isNullOrBlank() && !personName.equals("General", ignoreCase = true)) {
                    return personName
                }
            } catch (_: Exception) {}
        }

        // 3. Sender before colon in EXTRA_TEXT / EXTRA_BIG_TEXT (e.g. "am_dhl11: Sent you a photo")
        val textToScan = if (rawBigText.isNotBlank()) rawBigText.trim() else rawText.trim()
        if (textToScan.contains(":")) {
            val candidate = textToScan.substringBefore(":").trim()
            if (candidate.isNotBlank() && candidate.length < 35 && !candidate.equals("General", ignoreCase = true)) {
                return candidate
            }
        }

        // 4. Sender after colon in EXTRA_CONVERSATION_TITLE or EXTRA_TITLE (e.g. "a24m51: am_dhl11")
        val titleToScan = if (!rawConversationTitle.isNullOrBlank()) rawConversationTitle.trim() else rawTitle.trim()
        if (titleToScan.contains(":")) {
            val candidate = titleToScan.substringAfterLast(":").trim()
            if (candidate.isNotBlank()) {
                return candidate
            }
        }

        if (titleToScan.isNotBlank()) {
            return titleToScan
        }

        return "General"
    }

    private fun processAndSaveNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Stage 3 Check: Immediate Blacklist Gate
        if (blockedPackagesCache.contains(packageName)) {
            return
        }

        val notification = sbn.notification
        val postTime = sbn.postTime
        val notificationKey = sbn.key ?: ""

        // Exclude ongoing (foreground services/media controls), group summaries, non-clearable, and self-app notifications
        val isOngoing = sbn.isOngoing || (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val groupSummary = NotificationCompat.isGroupSummary(notification)
        val isSelfApp = applicationContext.packageName == packageName

        if (isOngoing || groupSummary || isSelfApp || !sbn.isClearable) {
            return
        }

        val rawTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val rawBigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val rawConversationTitle = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()

        val senderName = extractSenderName(notification, rawTitle, rawText, rawBigText, rawConversationTitle)
        val conversationTitle = senderName.ifBlank { "General" }
        val title = senderName.ifBlank { rawTitle.trim().ifBlank { "General" } }

        var content = if (rawBigText.isNotBlank()) rawBigText.trim() else rawText.trim()

        if (title.isBlank() && content.isBlank()) return

        // Pre-clean content if it starts with "a24m51: am_dhl11:" or "am_dhl11:"
        if (content.isNotBlank() && rawTitle.isNotBlank() && content.startsWith(rawTitle, ignoreCase = true)) {
            content = content.substring(rawTitle.length).trim()
            if (content.startsWith(":") || content.startsWith("-") || content.startsWith("–")) {
                content = content.substring(1).trim()
            }
        }
        if (content.isNotBlank() && content.startsWith(senderName, ignoreCase = true)) {
            content = content.substring(senderName.length).trim()
            if (content.startsWith(":") || content.startsWith("-") || content.startsWith("–")) {
                content = content.substring(1).trim()
            }
        }

        val dedupKey = Objects.hash(packageName, title, content)
        val now = System.currentTimeMillis()

        // Bounded eviction: remove stale entries older than 5,000ms or if size exceeds 100
        recentNotificationsCache.entries.removeIf { (now - it.value) > 5_000L || recentNotificationsCache.size > 100 }

        val previous = recentNotificationsCache[dedupKey]
        if (previous != null && (now - previous) < 5_000L) {
            return // Burst duplicate within 5 seconds, drop immediately
        }
        recentNotificationsCache[dedupKey] = now

        val largeIcon: Icon? = notification.getLargeIcon()
        val personIcon: Icon? = if (largeIcon == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getPersonFromBundle(notification.extras)?.icon
        } else null
        val avatarIcon: Icon? = largeIcon ?: personIcon

        serviceScope.launch {
            val senderIconPath = if (avatarIcon != null && conversationTitle.isNotBlank()) {
                avatarStorageManager.saveAvatarToDisk(
                    context = applicationContext,
                    packageName = packageName,
                    conversationTitle = conversationTitle,
                    icon = avatarIcon,
                )
            } else null

            val item = NotificationItem(
                packageName = packageName,
                title = title,
                content = content,
                timestamp = postTime,
                notificationKey = notificationKey,
                conversationTitle = conversationTitle,
                senderIconPath = senderIconPath,
            )
            saveNotificationUseCase(item)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun getPersonFromBundle(extras: Bundle): Person? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON) as? Person
        }
    }
}
