package com.translatelens.presentation.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.translatelens.presentation.screen.camera.CameraScreen
import com.translatelens.presentation.screen.history.HistoryScreen
import com.translatelens.presentation.screen.home.HomeScreen
import com.translatelens.presentation.screen.offline.OfflineLanguagesScreen
import com.translatelens.presentation.screen.result.ResultScreen
import com.translatelens.presentation.screen.settings.SettingsScreen
import com.translatelens.presentation.screen.splash.SplashScreen
import com.translatelens.presentation.screen.text.TextTranslateScreen
import com.translatelens.presentation.theme.TranslateLensTheme
import com.translatelens.presentation.theme.themeModeToDark

@Composable
fun AppNavGraph(
    settingsViewModel: com.translatelens.presentation.screen.settings.SettingsViewModel = hiltViewModel(),
    startImagePath: String? = null
) {
    val nav = rememberNavController()
    val themeMode by settingsViewModel.themeMode.collectAsState(initial = 0)
    val systemDark = isSystemInDarkTheme()

    TranslateLensTheme(darkTheme = themeModeToDark(themeMode, systemDark)) {
        NavHost(
            navController = nav,
            startDestination = if (startImagePath != null) Routes.result(startImagePath) else Routes.SPLASH
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onGetStarted = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onCamera = { nav.navigate(Routes.CAMERA) },
                    onText = { nav.navigate(Routes.TEXT_TRANSLATE) },
                    onOffline = { nav.navigate(Routes.OFFLINE) },
                    onHistory = { nav.navigate(Routes.HISTORY) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onImagePicked = { path -> nav.navigate(Routes.result(path)) }
                )
            }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBack = { nav.popBackStack() },
                    onCaptured = { path -> nav.navigate(Routes.result(path)) }
                )
            }
            composable(Routes.TEXT_TRANSLATE) {
                TextTranslateScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.OFFLINE) {
                OfflineLanguagesScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenOffline = { nav.navigate(Routes.OFFLINE) }
                )
            }
            composable(
                route = Routes.RESULT,
                arguments = listOf(navArgument("imagePath") { type = NavType.StringType; defaultValue = "" })
            ) {
                ResultScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
