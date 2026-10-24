package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import javax.inject.Inject

class ToggleAppBlacklistUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(packageName: String, isBlacklisted: Boolean) {
        repository.toggleAppBlacklist(packageName, isBlacklisted)
    }
}
