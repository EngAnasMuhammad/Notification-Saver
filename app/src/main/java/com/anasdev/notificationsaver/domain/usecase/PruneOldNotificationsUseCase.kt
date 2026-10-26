package com.anasdev.notificationsaver.domain.usecase

import com.anasdev.notificationsaver.domain.model.RetentionPeriod
import com.anasdev.notificationsaver.domain.repository.NotificationRepository
import com.anasdev.notificationsaver.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PruneOldNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        val period = settingsRepository.retentionPeriod.first()
        if (period != RetentionPeriod.OFF && period.durationMillis > 0L) {
            val cutoffTimestamp = System.currentTimeMillis() - period.durationMillis
            notificationRepository.pruneOldNotifications(cutoffTimestamp)
        }
    }
}
