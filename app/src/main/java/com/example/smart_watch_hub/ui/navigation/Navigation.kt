package com.example.smart_watch_hub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smart_watch_hub.ui.screens.history.HistoryScreen
import com.example.smart_watch_hub.ui.screens.livedata.LiveDataScreen
import com.example.smart_watch_hub.ui.screens.scan.ScanScreen
import com.example.smart_watch_hub.ui.screens.sleep.SleepScheduleScreen

/**
 * Main navigation setup for Smart Watch Hub app.
 *
 * Screens:
 * - Scan: Device discovery and connection
 * - LiveData: Real-time metrics display with 5-min aggregation
 * - History: Historical data with charts (6h, 1d, 1w)
 * - Settings: App configuration and device management (Phase 8.1)
 * - OTA: Firmware update screen (Phase 7.4)
 */
@Composable
fun SmartWatchHubNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Scan.route
    ) {
        composable(Screen.Scan.route) {
            ScanScreen(
                onDeviceConnected = {
                    navController.navigate(Screen.LiveData.route) {
                        popUpTo(Screen.Scan.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.LiveData.route) {
            LiveDataScreen()
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Sleep.route) {
            SleepScheduleScreen()
        }

        // TODO: Phase 8.1
        // composable(Screen.Settings.route) {
        //     SettingsScreen()
        // }

        // TODO: Phase 7.4
        // composable(Screen.OTA.route) {
        //     OtaScreen()
        // }
    }
}

/**
 * Navigation screen definitions.
 */
sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object LiveData : Screen("livedata")
    object History : Screen("history")
    object Sleep : Screen("sleep")
    object Settings : Screen("settings")
    object OTA : Screen("ota")
}

/**
 * Bottom navigation items.
 *
 * Used by main activity to provide tab-based navigation.
 */
enum class BottomNavItem(val route: String, val label: String, val icon: Int) {
    SCAN("scan", "Scan", android.R.drawable.ic_dialog_map),
    LIVE_DATA("livedata", "Live Data", android.R.drawable.ic_menu_recent_history),
    HISTORY("history", "History", android.R.drawable.ic_menu_agenda),
    SLEEP("sleep", "Sleep", android.R.drawable.ic_menu_info_details),
    SETTINGS("settings", "Settings", android.R.drawable.ic_menu_preferences)
}
