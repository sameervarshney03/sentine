package com.example.sentine.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.ui.components.AppIcon
import com.example.sentine.ui.components.EmptyState
import com.example.sentine.ui.components.RiskBadge
import com.example.sentine.ui.components.RiskBar
import com.example.sentine.ui.theme.*
import com.example.sentine.ui.utils.DeviceType
import com.example.sentine.viewmodel.DashboardViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    deviceType: DeviceType,
    initialTab: Int = 0,
    onNavigateToDetail: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    settingsViewModel: com.example.sentine.viewmodel.SettingsViewModel = viewModel()
) {
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    val apps by viewModel.apps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    var selectedFilter by remember { 
        mutableStateOf(
            when (initialTab) {
                1 -> "High"
                2 -> "Medium"
                3 -> "Safe"
                else -> "All"
            }
        ) 
    }
    var sortOption by remember { mutableStateOf("Risk Score") }

    val filteredApps = remember(apps, selectedFilter, sortOption) {
        val filtered = when (selectedFilter) {
            "High" -> apps.filter { it.riskLevel == "HIGH" }
            "Medium" -> apps.filter { it.riskLevel == "MEDIUM" }
            "Low" -> apps.filter { it.riskLevel == "LOW" }
            "Safe" -> apps.filter { it.riskLevel == "SAFE" }
            "Trusted" -> apps.filter { it.isTrusted }
            else -> apps
        }
        when (sortOption) {
            "App Name (A-Z)" -> filtered.sortedBy { it.appName.lowercase() }
            "Recently Active" -> filtered.sortedByDescending { it.lastUpdated }
            else -> filtered.sortedByDescending { it.riskScore }
        }
    }

    val threatCount = apps.count { it.riskLevel == "HIGH" || it.riskLevel == "MEDIUM" }
    val settings by settingsViewModel.settings.collectAsState()

    val scanTimeAgo = remember(settings.lastScanTimestamp) {
        if (settings.lastScanTimestamp == 0L) {
            "Never scanned"
        } else {
            val diff = System.currentTimeMillis() - settings.lastScanTimestamp
            when {
                diff < 60_000L              -> "Just now"
                diff < 3_600_000L           -> "${diff / 60_000}m ago"
                diff < 86_400_000L          -> "${diff / 3_600_000}h ago"
                else                        -> "Yesterday"
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0E1A)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SentinelAI", style = AppTypography.headlineMedium, color = TextPrimary)
                        }
                        Text("${apps.size} apps monitored • $threatCount threats found", style = AppTypography.labelSmall, color = TextSecondary, modifier = Modifier.padding(top=4.dp))
                        Text("Last scan: $scanTimeAgo", style = AppTypography.labelSmall, color = TextSecondary)
                    }

                    if (isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...", color = Primary, fontSize = 13.sp)
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Reverse),
                            label = "scale"
                        )
                        OutlinedButton(
                            onClick = { viewModel.scanAllApps() },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                            modifier = Modifier.scale(scale)
                        ) {
                            Text("Scan Now", fontSize = 13.sp)
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)

            // SCAN BANNER
            AnimatedVisibility(
                visible = isScanning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryDim)
                        .drawBehind { drawRect(color = Primary, topLeft = Offset(0f, 0f), size = Size(3.dp.toPx(), size.height)) }
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("◉ $statusText", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(scanProgress * 100).toInt()}%", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Primary, trackColor = Color(0x334F8EF7)
                    )
                }
            }

            // RISK SUMMARY STRIP
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat("HIGH", apps.count { it.riskLevel == "HIGH" }, RiskHigh, selectedFilter == "High") { selectedFilter = "High" }
                SummaryStat("MED", apps.count { it.riskLevel == "MEDIUM" }, RiskMedium, selectedFilter == "Medium") { selectedFilter = "Medium" }
                SummaryStat("LOW", apps.count { it.riskLevel == "LOW" }, RiskLow, selectedFilter == "Low") { selectedFilter = "Low" }
                SummaryStat("SAFE", apps.count { it.riskLevel == "SAFE" }, RiskSafe, selectedFilter == "Safe") { selectedFilter = "Safe" }
            }
            HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)

            // FILTER & SORT ROW
            var sortMenuExpanded by remember { mutableStateOf(false) }
            val filterOptions = listOf("All", "High", "Medium", "Low", "Safe", "Trusted")
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { filter ->
                        val color = when(filter) {
                            "High" -> RiskHigh; "Medium" -> RiskMedium; "Low" -> RiskLow; "Safe" -> RiskSafe; else -> Primary
                        }
                        val bg = if (selectedFilter == filter) color.copy(alpha = 0.2f) else Color.Transparent
                        val tc = if (selectedFilter == filter) color else TextMuted
                        Box(
                            modifier = Modifier
                                .height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                                .clickable { selectedFilter = filter }.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(filter, color = tc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Filled.SwapVert, contentDescription = "Sort", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        listOf("Risk Score", "App Name (A-Z)", "Recently Active").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = TextPrimary) },
                                onClick = { sortOption = opt; sortMenuExpanded = false }
                            )
                        }
                    }
                }
            }

            // APP LIST
            if (apps.isEmpty() && !isScanning) {
                EmptyState(
                    icon = Icons.Filled.Shield, iconColor = TextMuted,
                    title = "Nothing scanned yet", subtitle = "Tap Scan Now to protect your device",
                    action = { Button(onClick = { viewModel.scanAllApps() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Scan Now", color = Color.White) } }
                )
            } else if (filteredApps.isEmpty() && !isScanning) {
                EmptyState(icon = Icons.Filled.CheckCircle, iconColor = SuccessColor, title = "All clear here", subtitle = "No ${selectedFilter.uppercase()} apps in this filter")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(filteredApps, key = { _, it -> it.packageName }) { index, app ->
                        AppRow(app, index) { onNavigateToDetail(app.packageName) }
                        HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, count: Int, color: Color, isActive: Boolean, onClick: () -> Unit) {
    val animCount by animateIntAsState(targetValue = count, animationSpec = tween(800), label = "count")
    val lineAlpha by animateFloatAsState(targetValue = if (isActive) 1f else 0f, label = "line")
    
    Column(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(animCount.toString(), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = AppTypography.bodyLarge.fontFamily)
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTypography.bodyLarge.fontFamily)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(32.dp).height(2.dp).alpha(lineAlpha).background(color))
    }
}

@Composable
fun AppRow(app: AppRiskEntity, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 30L); visible = true }
    
    val alphaAnim by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "rowAlpha")
    val offsetAnim by animateDpAsState(if (visible) 0.dp else 20.dp, tween(500), label = "rowOffset")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.Transparent)
            .drawBehind {
                if (app.riskLevel == "HIGH") {
                    drawRect(color = RiskHigh, topLeft = Offset(0f, 0f), size = Size(3.dp.toPx(), size.height))
                }
            }
            .padding(16.dp, 12.dp)
            .alpha(alphaAnim)
            .offset(y = offsetAnim),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, app.appName, 44.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextPrimary, style = AppTypography.titleLarge, fontSize=15.sp)
                        if (app.isTrusted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    val displayPkg = if (app.packageName.isBlank()) "Unknown package" else app.packageName
                    Text(displayPkg, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    RiskBadge(app.riskLevel, app.isTrusted)
                    val riskColor = if (app.isTrusted) Primary else when(app.riskLevel) { "HIGH" -> RiskHigh; "MEDIUM" -> RiskMedium; "LOW" -> RiskLow; else -> RiskSafe }
                    Text(app.riskScore.toString(), color = riskColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = AppTypography.bodyLarge.fontFamily, modifier = Modifier.padding(top=2.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            RiskBar(app.riskScore, app.riskLevel, app.isTrusted)
        }
    }
}
