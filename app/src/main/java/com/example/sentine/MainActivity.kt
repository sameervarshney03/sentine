package com.example.sentine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sentine.service.SentinelMonitorService
import com.example.sentine.ui.screens.*
import com.example.sentine.ui.theme.*
import com.example.sentine.ui.utils.DeviceType
import com.example.sentine.ui.utils.calculateDeviceType
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isReviewHighRisk = intent?.action == "com.example.sentine.ACTION_REVIEW_HIGH_RISK"
        val initialTab = if (isReviewHighRisk) 1 else 0

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val deviceType = calculateDeviceType(windowSizeClass)
            
            val settingsViewModel: com.example.sentine.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settings by settingsViewModel.settings.collectAsState()
            
            val startDestination = if (settings.onboardingCompleted) "dashboard" else "onboarding"
            
            SentineTheme {
                MainApp(deviceType, initialTab, startDestination, settingsViewModel) {
                    startMonitoringService()
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
        
        val workRequest = PeriodicWorkRequestBuilder<com.example.sentine.worker.SentinelWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().build())
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SentinelSmartScan",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun MainApp(
    deviceType: DeviceType, 
    initialTab: Int, 
    startDestination: String, 
    settingsViewModel: com.example.sentine.viewmodel.SettingsViewModel,
    onStartService: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/") ?: "dashboard"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0E1A),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentRoute in listOf("dashboard", "alerts", "settings")) {
                CustomBottomNav(currentRoute, navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            composable("onboarding") {
                OnboardingScreen(onFinished = {
                    settingsViewModel.setOnboardingCompleted()
                    onStartService()
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
            composable("dashboard") { DashboardScreen(deviceType = deviceType, initialTab = initialTab, onNavigateToDetail = { pkg -> navController.navigate("detail/$pkg") }) }
            composable("alerts") { AlertsScreen() }
            composable("settings") { SettingsScreen() }
            composable("detail/{pkg}") { backStackEntry -> 
                val pkg = backStackEntry.arguments?.getString("pkg") ?: ""
                AppDetailScreen(packageName = pkg, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun CustomBottomNav(currentRoute: String, navController: NavHostController) {
    val items = listOf(
        BottomNavItem("dashboard", Icons.Filled.Shield, Icons.Outlined.Shield),
        BottomNavItem("alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, badgeCount = 0),
        BottomNavItem("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = screenWidth / items.size
    
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    
    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex + (itemWidth / 2) - 12.dp,
        animationSpec = tween(300),
        label = "indicator"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .background(Surface)
            .drawBehind { drawRect(color = BorderColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val icon = if (isSelected) item.activeIcon else item.inactiveIcon
                val tint = if (isSelected) Primary else TextMuted
                
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                        if (item.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(RiskHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.badgeCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = indicatorOffset, y = (-8).dp)
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Primary)
        )
    }
}

data class BottomNavItem(val route: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector, val badgeCount: Int = 0)
