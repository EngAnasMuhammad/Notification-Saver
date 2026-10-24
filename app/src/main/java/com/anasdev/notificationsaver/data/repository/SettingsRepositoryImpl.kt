package com.anasdev.notificationsaver.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val BLACKLISTED_APPS_KEY = stringSetPreferencesKey("blacklisted_apps")
        val THEME_KEY = stringPreferencesKey("app_theme")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    override val blacklistedApps: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[BLACKLISTED_APPS_KEY] ?: emptySet()
    }

    override val activeTheme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val name = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
        runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.SYSTEM)
    }

    override val activeLanguage: Flow<AppLanguage> = dataStore.data.map { preferences ->
        val name = preferences[LANGUAGE_KEY] ?: AppLanguage.SYSTEM.name
        runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.SYSTEM)
    }

    override suspend fun toggleAppBlacklist(packageName: String, isBlacklisted: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[BLACKLISTED_APPS_KEY] ?: emptySet()
            val updated = if (isBlacklisted) {
                current + packageName
            } else {
                current - packageName
            }
            preferences[BLACKLISTED_APPS_KEY] = updated
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }
}
