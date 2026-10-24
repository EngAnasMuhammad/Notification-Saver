package com.anasdev.notificationsaver.domain.model

import androidx.annotation.StringRes
import com.anasdev.notificationsaver.R

enum class AppLanguage(
    @StringRes val titleResId: Int,
    val tag: String
) {
    SYSTEM(R.string.language_system, ""),
    ENGLISH(R.string.language_english, "en"),
    ARABIC(R.string.language_arabic, "ar")
}
