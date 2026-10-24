package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.data.provider.AppInfoProvider
import com.anasdev.notificationsaver.domain.model.AppFilterItem
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetInstalledAppsListUseCase @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<List<AppFilterItem>> {
        return settingsRepository.blacklistedApps.map { blacklistedSet ->
            val installedApps = appInfoProvider.getInstalledApps(blacklistedSet)
            installedApps.map { app ->
                AppFilterItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    isBlacklisted = app.packageName in blacklistedSet
                )
            }.sortedWith(
                compareByDescending<AppFilterItem> { it.isBlacklisted }
                    .thenBy { it.appName.lowercase() }
            )
        }
    }
}
