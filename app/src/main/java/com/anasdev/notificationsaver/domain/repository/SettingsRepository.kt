package com.anasdev.notificationsaver.domain.repository

import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.domain.model.RetentionPeriod
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val blacklistedApps: Flow<Set<String>>
    val activeTheme: Flow<AppTheme>
    val activeLanguage: Flow<AppLanguage>
    val retentionPeriod: Flow<RetentionPeriod>
    val hasSeenDisclaimer: Flow<Boolean>

    suspend fun toggleAppBlacklist(packageName: String, isBlacklisted: Boolean)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setRetentionPeriod(period: RetentionPeriod)
    suspend fun setDisclaimerSeen()
}
