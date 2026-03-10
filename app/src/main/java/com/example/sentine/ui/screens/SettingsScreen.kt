package com.example.sentine.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentine.ui.theme.DeepBlue
import com.example.sentine.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var monitoringEnabled by remember { mutableStateOf(sharedPref.getBoolean("monitoring_enabled", true)) }
    var scanFrequency by remember { mutableStateOf(sharedPref.getString("scan_frequency", "5 min") ?: "5 min") }
    var notifyHighOnly by remember { mutableStateOf(sharedPref.getBoolean("notify_high_only", false)) }
    var gracePeriod by remember { mutableStateOf(sharedPref.getInt("grace_period", 10).toFloat()) }
    var themeMode by remember { mutableStateOf(sharedPref.getString("theme_mode", "System") ?: "System") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "General") {
                ThemeSelector(
                    currentTheme = themeMode,
                    onThemeChange = { 
                        themeMode = it
                        sharedPref.edit().putString("theme_mode", it).apply()
                    }
                )
            }

            SettingsSection(title = "Monitoring") {
                SettingsSwitchItem(
                    title = "Background Monitoring",
                    desc = "Periodically scan apps for suspicious behavior",
                    checked = monitoringEnabled,
                    icon = Icons.Default.Radar,
                    onCheckedChange = { 
                        monitoringEnabled = it
                        sharedPref.edit().putBoolean("monitoring_enabled", it).apply()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Scan Frequency", style = MaterialTheme.typography.labelLarge, color = DeepBlue)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("5 min", "15 min", "30 min").forEach { freq ->
                        FilterChip(
                            selected = scanFrequency == freq,
                            onClick = { 
                                scanFrequency = freq
                                sharedPref.edit().putString("scan_frequency", freq).apply()
                            },
                            label = { Text(freq) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Active Use Grace Period: ${gracePeriod.roundToInt()} min", style = MaterialTheme.typography.labelLarge, color = DeepBlue)
                Slider(
                    value = gracePeriod,
                    onValueChange = { gracePeriod = it },
                    onValueChangeFinished = {
                        sharedPref.edit().putInt("grace_period", gracePeriod.roundToInt()).apply()
                    },
                    valueRange = 5f..30f,
                    steps = 24
                )
                Text(
                    "SentinelAI will wait this long after you use an app before flagging its background behavior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    title = "Priority Alerts Only",
                    desc = "Only notify for HIGH risk detections",
                    checked = notifyHighOnly,
                    icon = Icons.Default.NotificationsActive,
                    onCheckedChange = { 
                        notifyHighOnly = it
                        sharedPref.edit().putBoolean("notify_high_only", it).apply()
                    }
                )
            }

            SettingsSection(title = "Data Management") {
                Button(
                    onClick = { viewModel.startScan() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Immediate Scan")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Scan History")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Gray.copy(alpha = 0.5f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("SentinelAI v1.2.0", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text("Privacy-focused Behavioral Monitor", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Reset Application Data?") },
            text = { Text("This will permanently delete all scan results and security alerts. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showDeleteDialog = false
                }) {
                    Text("RESET EVERYTHING", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, desc: String, checked: Boolean, icon: ImageVector, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = DeepBlue.copy(alpha = 0.1f)
        ) {
            Icon(icon, contentDescription = null, tint = DeepBlue, modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemeSelector(currentTheme: String, onThemeChange: (String) -> Unit) {
    Column {
        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Dark", "Light").forEach { mode ->
                val isSelected = currentTheme == mode
                OutlinedCard(
                    onClick = { onThemeChange(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) DeepBlue.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (isSelected) listOf(DeepBlue, DeepBlue) else listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f))
                        )
                    )
                ) {
                    Box(modifier = Modifier.padding(8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) DeepBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
