package com.anasdev.notificationsaver.presentation

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasdev.notificationsaver.domain.model.AppTheme
import com.anasdev.notificationsaver.presentation.navigation.AppNavigation
import com.anasdev.notificationsaver.presentation.state.NotificationViewModel
import com.anasdev.notificationsaver.presentation.state.SettingsViewModel
import com.anasdev.notificationsaver.presentation.ui.screens.PermissionScreen
import com.anasdev.notificationsaver.presentation.ui.theme.NotificationSaverTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val activeTheme by settingsViewModel.activeTheme.collectAsStateWithLifecycle()

            var isConfigReady by remember { mutableStateOf(false) }

            // Hold splash screen briefly for initial frame rendering
            splashScreen.setKeepOnScreenCondition { !isConfigReady }

            LaunchedEffect(Unit) {
                isConfigReady = true
            }

            val isDark = when (activeTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose { }
            }

            NotificationSaverTheme(darkTheme = isDark) {
                val viewModel: NotificationViewModel = hiltViewModel()
                var notificationEnabled by remember { mutableStateOf(viewModel.checkNotificationPermission()) }
                var batteryOptimized by remember { mutableStateOf(!viewModel.isIgnoringBatteryOptimizations()) }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    notificationEnabled = viewModel.checkNotificationPermission()
                    batteryOptimized = !viewModel.isIgnoringBatteryOptimizations()
                    viewModel.rebindListenerService()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (notificationEnabled) {
                        AppNavigation(
                            pm = packageManager,
                            viewModel = viewModel,
                            isBatteryOptimized = batteryOptimized,
                        )
                    } else {
                        PermissionScreen()
                    }
                }
            }
        }
    }
}
