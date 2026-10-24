package com.anasdev.notificationsaver.domain.model

import androidx.annotation.StringRes
import com.anasdev.notificationsaver.R

enum class AppTheme(@StringRes val titleResId: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark)
}
