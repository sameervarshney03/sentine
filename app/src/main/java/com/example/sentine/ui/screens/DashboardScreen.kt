package com.example.sentine.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.ui.theme.*
import com.example.sentine.ui.utils.DeviceType
import com.example.sentine.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceType: DeviceType,
    onNavigateToDetail: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val risks by viewModel.allAppRisks.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgressText by viewModel.scanProgressText.collectAsState()

    val highCount = risks.count { it.riskLevel == "HIGH" }
    val mediumCount = risks.count { it.riskLevel == "MEDIUM" }
    val safeCount = risks.count { it.riskLevel == "SAFE" || it.riskLevel == "LOW" }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedSummaryHeader(risks.size, risks.maxByOrNull { it.lastUpdated }?.lastUpdated, isScanning)

            if (deviceType == DeviceType.EXPANDED) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        RiskOverviewSection(highCount, mediumCount, safeCount)
                    }
                    Column(modifier = Modifier.weight(1.5f)) {
                        AppListSection(risks, filter, onFilterChange = { viewModel.filter.value = it }, onNavigateToDetail)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    RiskOverviewSection(highCount, mediumCount, safeCount)
                    Spacer(modifier = Modifier.height(16.dp))
                    AppListSection(risks, filter, onFilterChange = { viewModel.filter.value = it }, onNavigateToDetail)
                }
            }
        }

        if (isScanning) {
            ScanningOverlay(scanProgressText)
        }
    }
}

@Composable
fun AnimatedSummaryHeader(totalApps: Int, lastScan: Long?, isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DeepBlue, Color(0xFF0D1B2A))))
                .padding(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text(
                        text = "$totalApps Apps Monitored",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    lastScan?.let {
                        val minutes = (System.currentTimeMillis() - it) / 60000
                        Text(
                            text = "Last scan: $minutes min ago",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color.Cyan,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun RiskOverviewSection(high: Int, medium: Int, safe: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("HIGH Risk", high, RiskHigh, Icons.Default.Warning)
        StatCard("MEDIUM Risk", medium, RiskMedium, Icons.Default.Info)
        StatCard("SAFE Apps", safe, RiskLow, Icons.Default.CheckCircle)
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier.width(160.dp).height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(48.dp).align(Alignment.BottomEnd).alpha(0.15f), 
                tint = color
            )
            Column {
                Text(
                    text = count.toString(), 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Black, 
                    color = color
                )
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListSection(
    risks: List<AppRiskEntity>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Column {
        ScrollableTabRow(
            selectedTabIndex = listOf("ALL", "HIGH", "MEDIUM", "LOW", "SAFE").indexOf(currentFilter),
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            divider = {},
            indicator = {}
        ) {
            listOf("ALL", "HIGH", "MEDIUM", "LOW", "SAFE").forEach { f ->
                FilterChip(
                    selected = currentFilter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (risks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No apps found", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(risks, key = { it.packageName }) { app ->
                    AppListCard(app, onClick = { onNavigateToDetail(app.packageName) })
                }
            }
        }
    }
}

@Composable
fun AppListCard(app: AppRiskEntity, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val context = LocalContext.current

    val riskColor = when(app.riskLevel) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        "LOW" -> RiskLow
        else -> RiskSafe
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 60 }),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(context.packageManager.getApplicationIcon(app.packageName))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { app.riskScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = riskColor,
                        trackColor = riskColor.copy(alpha = 0.1f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = riskColor.copy(alpha = 0.15f), 
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = app.riskLevel, 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = riskColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning_radar")
    val radius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "radius"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(radius.dp)
                .alpha((1f - (radius / 2000f)).coerceIn(0f, 1f))
                .background(DeepBlue.copy(alpha = 0.4f), CircleShape)
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.White,
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = text, 
                color = Color.White, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
