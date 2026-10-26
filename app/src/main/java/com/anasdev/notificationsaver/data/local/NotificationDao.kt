package com.anasdev.notificationsaver.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasdev.notificationsaver.data.model.NotificationData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) providing indexed Room SQLite database queries
 * for managing notification persistence, conversation deletions, deduplication, and retention pruning.
 */
@Dao
interface NotificationDao {

    /** Inserts or replaces a notification record in the local database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationData)

    /** Deletes a specific notification entity from the database. */
    @Delete
    suspend fun deleteNotification(notification: NotificationData)

    /** Deletes a notification by its unique database primary key ID. */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    /** Deletes all notifications matching a specific package name and conversation title. */
    @Query("DELETE FROM notifications WHERE packageName = :packageName AND (conversationTitle = :title OR (:title = 'General' AND (conversationTitle IS NULL OR conversationTitle = '')) OR title = :title)")
    suspend fun deleteConversationByPackageAndTitle(packageName: String, title: String)

    /** Deletes all notifications associated with an application package. */
    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteAppNotifications(packageName: String)

    /** Purges all notification records from the local database. */
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    /** Deletes all notifications older than the specified rolling millisecond cutoff timestamp. */
    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteNotificationsOlderThan(cutoffTimestamp: Long)

    /** Retrieves distinct application package names stored in the database. */
    @Query("SELECT DISTINCT packageName FROM notifications")
    suspend fun getDistinctPackages(): List<String>

    /** Checks if a matching notification was posted within a 5-second sliding window for deduplication. */
    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE packageName = :packageName AND title = :title AND content = :content AND timestamp >= :timestamp - 5000)")
    suspend fun isNotificationDuplicate(packageName: String, title: String, content: String, timestamp: Long): Boolean

    /** Emits all notification records ordered by creation timestamp descending. */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsByDate(): Flow<List<NotificationData>>

    /** Queries notifications matching a title or content search term ordered by timestamp descending. */
    @Query("SELECT * FROM notifications WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotifications(query: String): Flow<List<NotificationData>>
}
