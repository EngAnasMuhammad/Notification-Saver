package com.anasdev.notificationsaver.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anasdev.notificationsaver.data.service.NotificationListenerService

/**
 * Ensures NotificationListenerService is triggered and re-evaluated by the system
 * when the device boots up or updates.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if ((action == Intent.ACTION_BOOT_COMPLETED) || (action == Intent.ACTION_MY_PACKAGE_REPLACED)) {
            NotificationListenerService.rebindService(context)
        }
    }
}
