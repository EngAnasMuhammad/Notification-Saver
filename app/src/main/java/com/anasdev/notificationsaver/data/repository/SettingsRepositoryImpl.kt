package com.anasdev.notificationsaver.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val BLACKLISTED_APPS_KEY = stringSetPreferencesKey("blacklisted_apps")
        val THEME_KEY = stringPreferencesKey("app_theme")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        const val RAW_PREFS = "settings_raw"
    }

    private val rawPrefs = context.getSharedPreferences(RAW_PREFS, Context.MODE_PRIVATE)

    override val blacklistedApps: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[BLACKLISTED_APPS_KEY] ?: emptySet()
    }

    override val activeTheme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val name = preferences[THEME_KEY] ?: rawPrefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val theme = runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.SYSTEM)
        // Sync to rawPrefs for synchronous startup access
        if (rawPrefs.getString("app_theme", null) != theme.name) {
            rawPrefs.edit().putString("app_theme", theme.name).apply()
        }
        theme
    }

    override val activeLanguage: Flow<AppLanguage> = dataStore.data.map { preferences ->
        val name = preferences[LANGUAGE_KEY] ?: rawPrefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        val lang = runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.SYSTEM)
        // Sync to rawPrefs for synchronous startup access
        if (rawPrefs.getString("app_language", null) != lang.name) {
            rawPrefs.edit().putString("app_language", lang.name).apply()
        }
        lang
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
        rawPrefs.edit().putString("app_theme", theme.name).apply()
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        rawPrefs.edit().putString("app_language", language.name).apply()
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }
}
