package com.anasdev.notificationsaver.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.anasdev.notificationsaver.presentation.state.NotificationViewModel
import com.anasdev.notificationsaver.presentation.ui.screens.NotificationScreen
import com.anasdev.notificationsaver.presentation.ui.screens.PermissionScreen
import com.anasdev.notificationsaver.presentation.ui.theme.NotificationSaverTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationSaverTheme {
                val viewModel: NotificationViewModel = hiltViewModel()
                var notificationEnabled by remember { mutableStateOf(viewModel.checkNotificationPermission()) }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    notificationEnabled = viewModel.checkNotificationPermission()
                    viewModel.rebindListenerService()
                }

                if (notificationEnabled) {
                    NotificationScreen(pm = packageManager, viewModel = viewModel)
                } else {
                    PermissionScreen()
                }
            }
        }
    }
}
