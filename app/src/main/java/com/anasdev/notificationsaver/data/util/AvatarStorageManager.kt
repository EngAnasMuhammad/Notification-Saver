package com.anasdev.notificationsaver.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.graphics.createBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages thread-safe extraction, compression, deduplication, and cleanup of sender avatar icons
 * stored in application cache storage (`cacheDir/avatars_v2`).
 */
@Singleton
class AvatarStorageManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    private val avatarsDir: File by lazy {
        File(appContext.cacheDir, "avatars_v2").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Converts a notification [Icon] into a compressed WebP file saved to application cache storage.
     * Skips Bitmap decoding and re-compression if the file already exists on disk.
     * Returns the absolute file path on success, or null on failure.
     */
    suspend fun saveAvatarToDisk(
        context: Context,
        packageName: String,
        conversationTitle: String,
        icon: Icon
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "${packageName}_${conversationTitle.hashCode()}.webp"
            val file = File(avatarsDir, fileName)

            // Fast exit: Skip decoding/compressing if avatar file is already cached on disk
            if (file.exists() && file.length() > 0) {
                return@withContext file.absolutePath
            }

            val drawable = icon.loadDrawable(context) ?: return@withContext null

            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
                val b = createBitmap(width, height)
                val canvas = Canvas(b)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                b
            }

            FileOutputStream(file).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a specific avatar file by its path if it exists.
     */
    suspend fun deleteAvatarFile(filePath: String?) = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes multiple avatar files given a collection of file paths.
     */
    suspend fun deleteAvatarFiles(filePaths: Collection<String>) = withContext(Dispatchers.IO) {
        for (path in filePaths) {
            if (path.isNotBlank()) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Deletes all avatar files stored in the cache directory.
     */
    suspend fun clearAllAvatars() = withContext(Dispatchers.IO) {
        try {
            if (avatarsDir.exists()) {
                avatarsDir.listFiles()?.forEach { it.delete() }
            }
            // Also clean legacy filesDir/avatars_v2 if it exists
            val legacyDir = File(appContext.filesDir, "avatars_v2")
            if (legacyDir.exists()) {
                legacyDir.listFiles()?.forEach { it.delete() }
                legacyDir.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

