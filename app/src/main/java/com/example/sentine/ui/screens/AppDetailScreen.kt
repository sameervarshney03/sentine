package com.example.sentine.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.ui.theme.*
import com.example.sentine.ui.utils.DeviceType
import com.example.sentine.viewmodel.AppDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    deviceType: DeviceType,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = viewModel()
) {
    val appRisk by viewModel.appRisk.collectAsState()
    val events by viewModel.getEvents(packageName).collectAsState(initial = emptyList())
    val context = LocalContext.current

    LaunchedEffect(packageName) {
        viewModel.loadAppDetails(packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            appRisk?.let { risk ->
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleTrusted(packageName) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskLow)
                        ) {
                            Icon(if (risk.isTrusted) Icons.Default.GppBad else Icons.Default.VerifiedUser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (risk.isTrusted) "Revoke Trust" else "Mark Trusted")
                        }
                        Button(
                            onClick = { /* View Full Logs */ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Full Logs")
                        }
                    }
                }
            }
        }
    ) { padding ->
        appRisk?.let { risk ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Blurred background
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(context.packageManager.getApplicationIcon(risk.packageName))
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .blur(40.dp)
                        .alpha(0.3f),
                    contentScale = ContentScale.Crop
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        DetailedHeader(risk)
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AnimatedCircularGauge(score = risk.riskScore)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        Text("Signal Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (deviceType == DeviceType.EXPANDED) {
                        item {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DetailedSignalCard(Modifier.weight(1f), "Network", risk.networkScore, "Upload rate detection", Icons.Default.NetworkCheck)
                                    DetailedSignalCard(Modifier.weight(1f), "Background", risk.backgroundScore, "Silent activity monitor", Icons.Default.History)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DetailedSignalCard(Modifier.weight(1f), "Service", risk.serviceScore, "Active foreground tasks", Icons.Default.SettingsSuggest)
                                    DetailedSignalCard(Modifier.weight(1f), "Screen-Off", risk.screenOffScore, "Dark processing detection", Icons.Default.ScreenLockPortrait)
                                }
                            }
                        }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DetailedSignalCard(Modifier.fillMaxWidth(), "Network Activity", risk.networkScore, "High upload rates relative to background usage.", Icons.Default.NetworkCheck)
                                DetailedSignalCard(Modifier.fillMaxWidth(), "Background Usage", risk.backgroundScore, "Activity detected when the app was not in foreground.", Icons.Default.History)
                                DetailedSignalCard(Modifier.fillMaxWidth(), "Foreground Service", risk.serviceScore, "Persistent service running in background.", Icons.Default.SettingsSuggest)
                                DetailedSignalCard(Modifier.fillMaxWidth(), "Screen-Off Activity", risk.screenOffScore, "Data or processing occurring while screen is off.", Icons.Default.ScreenLockPortrait)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Recent Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (events.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "No suspicious events recorded in the last 24 hours.",
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(events) { event ->
                            TimelineItem(event)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun DetailedHeader(risk: AppRiskEntity) {
    val context = LocalContext.current
    val riskColor = when (risk.riskLevel) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        "LOW" -> RiskLow
        else -> RiskSafe
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = riskColor.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, riskColor)
            ) {}
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(context.packageManager.getApplicationIcon(risk.packageName))
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(risk.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(risk.packageName, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            color = riskColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "${risk.riskLevel} RISK",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnimatedCircularGauge(score: Int) {
    val animatedScore = animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "score_anim"
    )

    val riskColor = when {
        animatedScore.value > 75 -> RiskHigh
        animatedScore.value > 50 -> RiskMedium
        animatedScore.value > 25 -> RiskLow
        else -> RiskSafe
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(180.dp)) {
                // Background Track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                // Score Arc
                drawArc(
                    color = riskColor,
                    startAngle = 135f,
                    sweepAngle = (animatedScore.value / 100f) * 270f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = animatedScore.value.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = riskColor
                )
                Text("RISK SCORE", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }
        }
        Text(
            text = when {
                score > 75 -> "Critical behavioral risk detected"
                score > 50 -> "Elevated background activity"
                score > 25 -> "Minor anomalies observed"
                else -> "App behavior appears normal"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = riskColor
        )
    }
}

@Composable
fun DetailedSignalCard(modifier: Modifier, title: String, score: Int, desc: String, icon: ImageVector) {
    val riskColor = when {
        score > 70 -> RiskHigh
        score > 40 -> RiskMedium
        else -> RiskLow
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("$score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = riskColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = riskColor,
                trackColor = riskColor.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun TimelineItem(event: RiskEventEntity) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(DeepBlue)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(DeepBlue.copy(alpha = 0.2f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = DeepBlue
            )
            Text(
                text = event.eventDetail,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
