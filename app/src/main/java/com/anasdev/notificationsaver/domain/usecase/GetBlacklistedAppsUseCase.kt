package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBlacklistedAppsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Set<String>> {
        return repository.blacklistedApps
    }
}
