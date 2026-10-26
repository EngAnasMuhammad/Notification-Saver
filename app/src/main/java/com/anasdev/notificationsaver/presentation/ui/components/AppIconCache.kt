package com.anasdev.notificationsaver.presentation.ui.components

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Formats timestamps into localized time strings (using ص/م for Arabic and AM/PM for English). */
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val locale = Locale.getDefault()
    val isArabic = locale.language.equals("ar", ignoreCase = true)

    return if (isArabic) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.forLanguageTag("ar"))
        val dfs = formatter.dateFormatSymbols
        dfs.amPmStrings = arrayOf("ص", "م")
        formatter.dateFormatSymbols = dfs
        formatter.format(date)
    } else {
        val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        formatter.format(date)
    }
}

/**
 * Thread-safe in-memory cache for app icons and labels to eliminate Binder IPC
 * queries during LazyColumn scrolling.
 */
object AppIconCache {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val nullIconSet = ConcurrentHashMap.newKeySet<String>()
    private val labelCache = ConcurrentHashMap<String, String>()

    fun getAppInfo(packageName: String, pm: PackageManager): Pair<ImageBitmap?, String> {
        val label = labelCache.getOrPut(packageName) {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName.substringAfterLast('.')
            }
        }

        if (nullIconSet.contains(packageName)) {
            return null to label
        }

        val cachedIcon = iconCache[packageName]
        if (cachedIcon != null) return cachedIcon to label

        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val bitmap = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            iconCache[packageName] = bitmap
            bitmap to label
        } catch (_: Exception) {
            nullIconSet.add(packageName)
            null to label
        }
    }
}
