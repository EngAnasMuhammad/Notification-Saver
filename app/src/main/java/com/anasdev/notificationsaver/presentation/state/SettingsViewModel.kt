package com.anasdev.notificationsaver.presentation.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.notificationsaver.domain.model.AppFilterItem
import com.anasdev.notificationsaver.domain.model.AppLanguage
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import com.anasdev.notificationsaver.domain.usecase.ClearAllNotificationsUseCase
import com.anasdev.notificationsaver.domain.usecase.GetInstalledAppsListUseCase
import com.anasdev.notificationsaver.domain.usecase.ToggleAppBlacklistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    getInstalledAppsListUseCase: GetInstalledAppsListUseCase,
    private val toggleAppBlacklistUseCase: ToggleAppBlacklistUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val activeTheme: StateFlow<AppTheme> = settingsRepository.activeTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val activeLanguage: StateFlow<AppLanguage> = settingsRepository.activeLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.SYSTEM
        )

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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleBlacklist(packageName: String, isBlacklisted: Boolean) {
        viewModelScope.launch {
            toggleAppBlacklistUseCase(packageName, isBlacklisted)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            clearAllNotificationsUseCase()
        }
    }
}
