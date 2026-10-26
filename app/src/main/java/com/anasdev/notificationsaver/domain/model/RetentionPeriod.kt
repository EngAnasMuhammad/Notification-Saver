package com.anasdev.notificationsaver.domain.model

import com.anasdev.notificationsaver.R

enum class RetentionPeriod(val days: Int, val durationMillis: Long) {
    OFF(days = -1, durationMillis = -1L),
    ONE_DAY(days = 1, durationMillis = 24L * 60L * 60L * 1000L),
    TEN_DAYS(days = 10, durationMillis = 10L * 24L * 60L * 60L * 1000L),
    THIRTY_DAYS(days = 30, durationMillis = 30L * 24L * 60L * 60L * 1000L);

    val titleResId: Int
        get() = when (this) {
            OFF -> R.string.retention_off
            ONE_DAY -> R.string.retention_1_day
            TEN_DAYS -> R.string.retention_10_days
            THIRTY_DAYS -> R.string.retention_30_days
        }
}
