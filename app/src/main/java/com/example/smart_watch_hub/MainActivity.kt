package com.example.smart_watch_hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smart_watch_hub.ui.screens.history.HistoryScreen
import com.example.smart_watch_hub.ui.screens.livedata.LiveDataScreen
import com.example.smart_watch_hub.ui.screens.scan.ScanScreen
import com.example.smart_watch_hub.ui.screens.sleep.SleepScheduleScreen
import com.example.smart_watch_hub.ui.theme.Smart_Watch_HUBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Smart_Watch_HUBTheme {
                MainScreen()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MainScreen() {
    val navController = rememberNavController()
    // Track current route from NavController's back stack
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "scan"

    Scaffold(
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.BatteryChargingFull, contentDescription = "Scan") },
                    label = { Text("Scan") },
                    selected = currentRoute == "scan",
                    onClick = {
                        navController.navigate("scan") {
                            popUpTo("scan") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Live Data") },
                    label = { Text("Live Data") },
                    selected = currentRoute == "livedata",
                    onClick = {
                        navController.navigate("livedata") {
                            popUpTo("scan") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentRoute == "history",
                    onClick = {
                        navController.navigate("history") {
                            popUpTo("scan") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Sleep") },
                    label = { Text("Sleep") },
                    selected = currentRoute == "sleep",
                    onClick = {
                        navController.navigate("sleep") {
                            popUpTo("scan") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "scan",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("scan") {
                ScanScreen(
                    onDeviceConnected = {
                        navController.navigate("livedata") {
                            popUpTo("scan") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("livedata") {
                LiveDataScreen()
            }

            composable("history") {
                HistoryScreen()
            }

            composable("sleep") {
                SleepScheduleScreen()
            }
        }
    }
}