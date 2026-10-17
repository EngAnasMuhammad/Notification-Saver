package com.example.notificationsaver.data.mapper

import com.example.notificationsaver.data.model.NotificationData
import com.example.notificationsaver.domain.model.NotificationItem

fun NotificationData.toDomainModel(): NotificationItem {
    return NotificationItem(
        id = id,
        packageName = packageName,
        title = title,
        content = content,
        timestamp = timestamp,
        notificationKey = notificationKey
    )
}

fun NotificationItem.toEntity(): NotificationData {
    return NotificationData(
        id = id,
        packageName = packageName,
        title = title,
        content = content,
        timestamp = timestamp,
        notificationKey = notificationKey
    )
}
