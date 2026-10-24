package com.anasdev.notificationsaver.presentation.navigation

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anasdev.notificationsaver.presentation.state.NotificationViewModel
import com.anasdev.notificationsaver.presentation.ui.screens.AppFilterScreen
import com.anasdev.notificationsaver.presentation.ui.screens.DetailScreen
import com.anasdev.notificationsaver.presentation.ui.screens.HomeScreen
import com.anasdev.notificationsaver.presentation.ui.screens.SettingsScreen

object NavRoutes {
    const val HOME = "home"
    const val DETAIL = "detail/{conversationKey}"
    const val SETTINGS = "settings"
    const val APP_FILTER = "app_filter"

    fun createDetailRoute(conversationKey: String): String {
        return "detail/${Uri.encode(conversationKey)}"
    }
}

@Composable
fun AppNavigation(
    pm: PackageManager,
    viewModel: NotificationViewModel,
    isBatteryOptimized: Boolean,
) {
    val navController = rememberNavController()
    var lastNavTime by remember { mutableLongStateOf(0L) }

    val spec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
    val fadeSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = spec
            ) + fadeIn(fadeSpec)
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = spec
            ) + fadeOut(fadeSpec)
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spec
            ) + fadeIn(fadeSpec)
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spec
            ) + fadeOut(fadeSpec)
        },
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                pm = pm,
                viewModel = viewModel,
                isBatteryOptimized = isBatteryOptimized,
                onConversationClick = { conversationKey ->
                    val now = System.currentTimeMillis()
                    if (now - lastNavTime >= 1000L) {
                        lastNavTime = now
                        navController.navigate(NavRoutes.createDetailRoute(conversationKey))
                    }
                },
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        composable(
            route = NavRoutes.DETAIL,
            arguments = listOf(
                navArgument("conversationKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedKey = backStackEntry.arguments?.getString("conversationKey") ?: ""
            val conversationKey = Uri.decode(encodedKey)

            DetailScreen(
                conversationKey = conversationKey,
                pm = pm,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onOpenAppFilterClick = { navController.navigate(NavRoutes.APP_FILTER) }
            )
        }

        composable(NavRoutes.APP_FILTER) {
            AppFilterScreen(
                pm = pm,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
