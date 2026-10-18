package com.anasdev.notificationsaver.presentation.ui.components

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Shared DateFormat instance to avoid allocating SimpleDateFormat on every list item recomposition
val timeFormatter by lazy {
    SimpleDateFormat("hh:mm a", Locale.getDefault())
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
