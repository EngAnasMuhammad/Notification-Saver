package com.anasdev.notificationsaver.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages thread-safe extraction and compression of sender avatar icons onto local disk storage (`filesDir/avatars_v2`).
 */
@Singleton
class AvatarStorageManager @Inject constructor() {

    /**
     * Converts a notification [Icon] into a compressed WebP file saved to local internal storage.
     * Returns the absolute file path on success, or null on failure.
     */
    suspend fun saveAvatarToDisk(
        context: Context,
        packageName: String,
        conversationTitle: String,
        icon: Icon
    ): String? = withContext(Dispatchers.IO) {
        try {
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

            val avatarsDir = File(context.filesDir, "avatars_v2")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }

            val fileName = "${packageName}_${conversationTitle.hashCode()}.webp"
            val file = File(avatarsDir, fileName)

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
}
