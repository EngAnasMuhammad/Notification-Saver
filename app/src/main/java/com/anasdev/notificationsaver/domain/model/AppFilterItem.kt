package com.anasdev.notificationsaver.domain.model

data class AppFilterItem(
    val packageName: String,
    val appName: String,
    val isBlacklisted: Boolean
)
