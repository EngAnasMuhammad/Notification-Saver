package com.anasdev.notificationsaver.domain.repository

import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val blacklistedApps: Flow<Set<String>>
    val activeTheme: Flow<AppTheme>
    val activeLanguage: Flow<AppLanguage>

    suspend fun toggleAppBlacklist(packageName: String, isBlacklisted: Boolean)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
}
