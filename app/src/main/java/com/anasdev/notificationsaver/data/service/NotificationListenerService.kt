package com.anasdev.notificationsaver.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.anasdev.notificationsaver.R
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
                .setContentTitle(getString(R.string.service_notification_title))
                .setContentText(getString(R.string.service_notification_sub))
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
        packageName: String,
        notification: Notification,
        rawTitle: String,
        rawText: String,
        rawBigText: String,
        rawConversationTitle: String?
    ): String {
        // Step A — Instagram Specific Handling
        if (packageName == "com.instagram.android") {
            try {
                val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
                if (messagingStyle != null) {
                    val myName = messagingStyle.user.name?.toString()?.trim()
                    val senderName = messagingStyle.messages.lastOrNull()?.person?.name?.toString()?.trim()
                    if (!senderName.isNullOrBlank() && senderName != myName && !senderName.equals("General", ignoreCase = true)) {
                        return senderName
                    }
                }
            } catch (_: Exception) {}

            val cleanTitle = rawTitle.trim()
            if (cleanTitle.contains(":")) {
                val sender = cleanTitle.substringAfterLast(":").trim()
                if (sender.isNotBlank()) {
                    return sender
                }
            }

            val textToScan = if (rawBigText.isNotBlank()) rawBigText.trim() else rawText.trim()
            if (textToScan.contains(":")) {
                val prefix = textToScan.substringBefore(":").trim()
                val isNotUrl = !prefix.contains("http", ignoreCase = true)
                val isNotClockTime = !prefix.matches(Regex(".*\\d+:\\d+.*"))
                if (prefix.isNotBlank() && prefix.length in 1..30 && isNotUrl && isNotClockTime && !prefix.equals("General", ignoreCase = true)) {
                    return prefix
                }
            }

            return cleanTitle.ifBlank { "General" }
        }

        // Step B — Standard MessagingStyle Apps (Telegram, WhatsApp, Signal, SMS, Discord, etc.)
        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null) {
                // Group or Channel Check (keeps Telegram Channels and WhatsApp Groups under channel/group title)
                val convTitle = messagingStyle.conversationTitle?.toString()?.trim()
                if (!convTitle.isNullOrBlank()) {
                    return convTitle
                }

                // 1-on-1 Direct Message Check
                val myName = messagingStyle.user.name?.toString()?.trim()
                val lastSender = messagingStyle.messages.lastOrNull()?.person?.name?.toString()?.trim()
                if (!lastSender.isNullOrBlank() && lastSender != myName) {
                    return lastSender
                }
            }
        } catch (_: Exception) {}

        if (!rawConversationTitle.isNullOrBlank()) {
            return rawConversationTitle.trim()
        }

        // Step C — All Other Apps (System, Games, Utilities) - No colon splitting to preserve clock times like "10:30"
        return rawTitle.trim().ifBlank { "General" }
    }

    private fun extractAvatarIcon(notification: Notification): Icon? {
        // Priority 1: LargeIcon (Telegram channels/groups/DMs, WhatsApp, Instagram avatar)
        val largeIcon = notification.getLargeIcon()
        if (largeIcon != null) {
            return largeIcon
        }

        // Priority 2: MessagingStyle last sender icon (ensuring it is NOT the device owner / self)
        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null) {
                val myName = messagingStyle.user.name?.toString()?.trim()
                val lastSender = messagingStyle.messages.lastOrNull()?.person
                if (lastSender != null) {
                    val senderName = lastSender.name?.toString()?.trim()
                    if (senderName != null && senderName != myName) {
                        val iconCompat = lastSender.icon
                        if (iconCompat != null) {
                            return iconCompat.toIcon(this)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: Return null (NEVER fall back to EXTRA_MESSAGING_PERSON / device owner "Me")
        return null
    }

    private fun processAndSaveNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Stage 3 Check: Immediate Blacklist Gate
        if (blockedPackagesCache.contains(packageName)) {
            return
        }

        val notification = sbn.notification
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

        val senderName = extractSenderName(packageName, notification, rawTitle, rawText, rawBigText, rawConversationTitle)
        val conversationTitle = senderName.ifBlank { "General" }
        val title = senderName.ifBlank { rawTitle.trim().ifBlank { "General" } }

        var content = if (rawBigText.isNotBlank()) rawBigText.trim() else rawText.trim()

        if (title.isBlank() && content.isBlank()) return

        // Single-prefix cleanup at ingestion time
        val prefixWithSpace = "$senderName: "
        val prefixWithoutSpace = "$senderName:"
        val cleanedContent = when {
            content.startsWith(prefixWithSpace, ignoreCase = true) -> content.substring(prefixWithSpace.length).trim()
            content.startsWith(prefixWithoutSpace, ignoreCase = true) -> content.substring(prefixWithoutSpace.length).trim()
            else -> content
        }
        content = cleanedContent.ifBlank { content }

        val dedupKey = Objects.hash(packageName, title, content)
        val now = System.currentTimeMillis()

        // Bounded eviction: remove stale entries older than 5,000ms or if size exceeds 100
        recentNotificationsCache.entries.removeIf { (now - it.value) > 5_000L || recentNotificationsCache.size > 100 }

        val previous = recentNotificationsCache[dedupKey]
        if (previous != null && (now - previous) < 5_000L) {
            return // Burst duplicate within 5 seconds, drop immediately
        }
        recentNotificationsCache[dedupKey] = now

        // Extract avatar Icon strictly following Priority 1 (getLargeIcon) -> Priority 2 (MessagingStyle lastSender != me) -> null
        val avatarIcon: Icon? = extractAvatarIcon(notification)

        val originalMessageTime = if (notification.`when` > 0L) notification.`when` else sbn.postTime

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
                timestamp = originalMessageTime,
                notificationKey = notificationKey,
                conversationTitle = conversationTitle,
                senderIconPath = senderIconPath,
            )
            saveNotificationUseCase(item)
        }
    }
}
