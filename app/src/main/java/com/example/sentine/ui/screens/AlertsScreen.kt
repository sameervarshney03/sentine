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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.ui.theme.RiskHigh
import com.example.sentine.ui.theme.RiskLow
import com.example.sentine.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val groupedEvents = remember(events) {
        groupEvents(events)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Security Alerts", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearAllData() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            EmptyAlertsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedEvents.forEach { (header, items) ->
                    item {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(items, key = { it.id }) { event ->
                        AlertCard(event)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(event: RiskEventEntity) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(RiskHigh)
            )
            
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(context.packageManager.getApplicationIcon(event.packageName))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.packageName.split(".").last().uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = getRelativeTime(event.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = event.eventDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = RiskHigh.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "HIGH SEVERITY",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = RiskHigh,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAlertsState(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "scale"
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(100.dp).graphicsLayer(scaleX = scale, scaleY = scale),
                tint = RiskLow
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("All Clear!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "No suspicious activities detected recently.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

private fun groupEvents(events: List<RiskEventEntity>): Map<String, List<RiskEventEntity>> {
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000
    
    return events.groupBy {
        when {
            now - it.timestamp < dayMillis -> "TODAY"
            now - it.timestamp < 2 * dayMillis -> "YESTERDAY"
            else -> "EARLIER"
        }
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
