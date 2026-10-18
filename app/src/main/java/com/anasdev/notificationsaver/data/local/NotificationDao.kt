package com.anasdev.notificationsaver.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasdev.notificationsaver.data.model.NotificationData
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationData)

    @Delete
    suspend fun deleteNotification(notification: NotificationData)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE packageName = :packageName AND title = :title AND content = :content AND timestamp >= :timestamp - 5000)")
    suspend fun isNotificationDuplicate(packageName: String, title: String, content: String, timestamp: Long): Boolean

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsByDate(): Flow<List<NotificationData>>

    @Query("SELECT * FROM notifications WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotifications(query: String): Flow<List<NotificationData>>
}