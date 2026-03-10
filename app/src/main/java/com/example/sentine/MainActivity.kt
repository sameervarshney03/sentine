package com.example.sentine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.sentine.service.SentinelMonitorService
import com.example.sentine.ui.screens.*
import com.example.sentine.ui.theme.SentineTheme
import com.example.sentine.ui.utils.DeviceType
import com.example.sentine.ui.utils.calculateDeviceType

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPref = getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
        val isOnboarded = sharedPref.getBoolean("onboarded", false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val deviceType = calculateDeviceType(windowSizeClass)
            
            SentineTheme {
                var showOnboarding by remember { mutableStateOf(!isOnboarded) }

                if (showOnboarding) {
                    OnboardingScreen(onFinished = {
                        sharedPref.edit().putBoolean("onboarded", true).apply()
                        showOnboarding = false
                        startMonitoringService()
                    })
                } else {
                    MainContent(deviceType)
                }
            }
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, SentinelMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun MainContent(deviceType: DeviceType) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Home,
        Screen.Alerts,
        Screen.Settings
    )

    Row {
        if (deviceType == DeviceType.EXPANDED) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                items.forEach { screen ->
                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (deviceType != DeviceType.EXPANDED && currentDestination?.route != "detail/{packageName}") {
                    NavigationBar {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    DashboardScreen(
                        deviceType = deviceType,
                        onNavigateToDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    })
                }
                composable(Screen.Alerts.route) {
                    AlertsScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(
                    route = "detail/{packageName}",
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                    AppDetailScreen(
                        packageName = packageName, 
                        deviceType = deviceType,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Dashboard", Icons.Default.Shield)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
