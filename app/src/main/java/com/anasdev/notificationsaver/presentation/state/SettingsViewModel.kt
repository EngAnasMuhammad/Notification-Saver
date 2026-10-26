package com.anasdev.notificationsaver.presentation.state

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.notificationsaver.domain.model.AppFilterItem
import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.domain.model.RetentionPeriod
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import com.anasdev.notificationsaver.domain.usecase.ClearAllNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.GetInstalledAppsListUseCase
import com.anasdev.notificationsaver.domain.usecase.PruneOldNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.ToggleAppBlacklistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing user preferences including Theme, Language, App Blacklisting,
 * Auto-Delete Retention Periods, and First-Launch Privacy Disclaimers.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    getInstalledAppsListUseCase: GetInstalledAppsListUseCase,
    private val toggleAppBlacklistUseCase: ToggleAppBlacklistUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
    private val pruneOldNotificationsUseCase: PruneOldNotificationsUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    /** StateFlow emitting the current app search filter query. */
    val searchQuery: StateFlow<String> = _searchQuery

    /** StateFlow emitting the currently selected application theme. */
    val activeTheme: StateFlow<AppTheme> = settingsRepository.activeTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    /** StateFlow emitting the currently selected application language. */
    val activeLanguage: StateFlow<AppLanguage> = settingsRepository.activeLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.SYSTEM
        )

    /** StateFlow emitting the active auto-delete retention period policy. */
    val retentionPeriod: StateFlow<RetentionPeriod> = settingsRepository.retentionPeriod
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RetentionPeriod.OFF
        )

    /** StateFlow emitting whether the user has acknowledged the privacy disclaimer dialog. */
    val hasSeenDisclaimer: StateFlow<Boolean> = settingsRepository.hasSeenDisclaimer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** StateFlow emitting the list of installed applications filtered by search query. */
    val appList: StateFlow<List<AppFilterItem>> = combine(
        getInstalledAppsListUseCase(),
        _searchQuery
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Updates the app search filter query string. */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Toggles blacklisted/disabled status for an application package. */
    fun onToggleBlacklist(packageName: String, isBlacklisted: Boolean) {
        viewModelScope.launch {
            toggleAppBlacklistUseCase(packageName, isBlacklisted)
        }
    }

    /** Updates the application theme setting. */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    /** Updates the application language setting and applies it to AppCompatDelegate. */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            val tag = language.tag
            if (tag.isEmpty()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    /** Updates the auto-delete retention policy and triggers immediate pruning. */
    fun setRetentionPeriod(period: RetentionPeriod) {
        viewModelScope.launch {
            settingsRepository.setRetentionPeriod(period)
            pruneOldNotificationsUseCase()
        }
    }

    /** Marks the first-launch privacy disclaimer as acknowledged by the user. */
    fun markDisclaimerSeen() {
        viewModelScope.launch {
            settingsRepository.setDisclaimerSeen()
        }
    }

    /** Permanently clears all notification records from the local database. */
    fun clearAllNotifications() {
        viewModelScope.launch {
            clearAllNotificationsUseCase()
        }
    }
}
