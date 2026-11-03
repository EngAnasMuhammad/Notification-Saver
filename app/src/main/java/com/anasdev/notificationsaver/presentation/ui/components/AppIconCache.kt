package com.anasdev.notificationsaver.presentation.ui.components

import android.content.Context
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.text.format.DateUtils
import com.anasdev.notificationsaver.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Formats time into localized clock time (e.g., "10:30 PM" or "10:30 م"). */
fun formatTimeOnly(timestamp: Long): String {
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
 * Formats timestamps into a professional relative day and time string:
 * - Today: "Today, 10:30 AM" / "اليوم، 10:30 ص"
 * - Yesterday: "Yesterday, 04:15 PM" / "أمس، 04:15 م"
 * - Older: "MMM d, hh:mm a" (e.g., "Nov 3, 10:30 AM")
 */
fun formatRelativeDateTime(timestamp: Long, context: Context): String {
    val timeStr = formatTimeOnly(timestamp)
    val isArabic = Locale.getDefault().language.equals("ar", ignoreCase = true)

    return when {
        DateUtils.isToday(timestamp) -> {
            val todayStr = context.getString(R.string.date_today)
            if (isArabic) "$todayStr، $timeStr" else "$todayStr, $timeStr"
        }
        isYesterday(timestamp) -> {
            val yesterdayStr = context.getString(R.string.date_yesterday)
            if (isArabic) "$yesterdayStr، $timeStr" else "$yesterdayStr, $timeStr"
        }
        else -> {
            val pattern = if (isArabic) "d MMM، hh:mm a" else "MMM d, hh:mm a"
            val targetLocale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
            val formatter = SimpleDateFormat(pattern, targetLocale)
            if (isArabic) {
                val dfs = formatter.dateFormatSymbols
                dfs.amPmStrings = arrayOf("ص", "م")
                formatter.dateFormatSymbols = dfs
            }
            formatter.format(Date(timestamp))
        }
    }
}

/** Formats date into a clean day section header (e.g., "Today", "Yesterday", or "Monday, Nov 3, 2026"). */
fun formatDateHeader(timestamp: Long, context: Context): String {
    val isArabic = Locale.getDefault().language.equals("ar", ignoreCase = true)

    return when {
        DateUtils.isToday(timestamp) -> context.getString(R.string.date_today)
        isYesterday(timestamp) -> context.getString(R.string.date_yesterday)
        else -> {
            val pattern = if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, MMMM d, yyyy"
            val targetLocale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
            SimpleDateFormat(pattern, targetLocale).format(Date(timestamp))
        }
    }
}

/** Checks whether a timestamp occurred on the calendar day prior to today. */
fun isYesterday(timestamp: Long): Boolean {
    val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
           messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
}

/** Backward compatible alias for existing components. */
fun formatTimestamp(timestamp: Long): String = formatTimeOnly(timestamp)

/**
 * Thread-safe memory-bounded cache for app icons and labels with LRU eviction
 * to eliminate Binder IPC queries and prevent memory leaks during LazyColumn scrolling.
 */
object AppIconCache {
    // Memory-bounded LRU cache capped at 100 app icons to prevent heap bloat and LMK eviction
    private val iconCache = LruCache<String, ImageBitmap>(100)
    private val nullIconSet = ConcurrentHashMap.newKeySet<String>()
    private val labelCache = LruCache<String, String>(250)
    private val lock = Any()

    fun getAppInfo(packageName: String, context: Context): Pair<ImageBitmap?, String> {
        val pm = context.packageManager
        val label = synchronized(lock) {
            labelCache[packageName]
        } ?: run {
            val retrieved = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName.substringAfterLast('.')
            }
            synchronized(lock) {
                labelCache.put(packageName, retrieved)
            }
            retrieved
        }

        if (nullIconSet.contains(packageName)) {
            return null to label
        }

        val cachedIcon = synchronized(lock) {
            iconCache[packageName]
        }
        if (cachedIcon != null) {
            return cachedIcon to label
        }

        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val bitmap = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            synchronized(lock) {
                iconCache.put(packageName, bitmap)
            }
            bitmap to label
        } catch (_: Exception) {
            nullIconSet.add(packageName)
            null to label
        }
    }
}
