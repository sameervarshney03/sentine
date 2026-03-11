package com.example.sentine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentine.ui.components.SectionHeader
import com.example.sentine.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    var showFrequencySheet  by remember { mutableStateOf(false) }
    var showQuietHoursSheet by remember { mutableStateOf(false) }
    var showClearDialog     by remember { mutableStateOf(false) }

    val hasUsagePermission = remember {
        mutableStateOf(viewModel.hasUsageAccessPermission())
    }

    LaunchedEffect(Unit) {
        hasUsagePermission.value = viewModel.hasUsageAccessPermission()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color(0xFF0A0E1A)
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item {
                Text(
                    text     = "Settings",
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = Color(0xFFF1F2F6),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = 16.dp, top = 24.dp, 
                        bottom = 8.dp
                    )
                )
            }

            item { SectionHeader(title = "MONITORING") }

            item {
                SettingsToggleRow(
                    icon       = Icons.Filled.Shield,
                    iconColor  = Color(0xFF4F8EF7),
                    title      = "Background Monitoring",
                    subtitle   = if (settings.monitoringEnabled)
                                     "Active — scanning every ${settings.scanFrequencyMins} min"
                                 else "Paused — tap to enable",
                    checked    = settings.monitoringEnabled,
                    onCheckedChange = { viewModel.setMonitoringEnabled(it) }
                )
            }

            item {
                SettingsClickRow(
                    icon      = Icons.Filled.Timer,
                    iconColor = Color(0xFF4F8EF7),
                    title     = "Scan Frequency",
                    subtitle  = viewModel.frequencyLabel(settings.scanFrequencyMins),
                    value     = "${settings.scanFrequencyMins}m",
                    enabled   = settings.monitoringEnabled,
                    onClick   = { showFrequencySheet = true }
                )
            }

            item {
                SettingsToggleRow(
                    icon      = Icons.Filled.BatteryAlert,
                    iconColor = Color(0xFF7BED9F),
                    title     = "Battery Saver",
                    subtitle  = if (settings.batterySaver) "Pauses scanning when battery < 20%" else "Scans even on low battery",
                    checked   = settings.batterySaver,
                    onCheckedChange = { viewModel.setBatterySaver(it) }
                )
            }

            item { SectionHeader(title = "ALERTS") }

            item {
                SettingsToggleRow(
                    icon      = Icons.Filled.NotificationsActive,
                    iconColor = Color(0xFFFF4757),
                    title     = "High Risk Alerts",
                    subtitle  = if (settings.highRiskAlerts) "Notifying for HIGH risk apps" else "HIGH risk notifications off",
                    checked   = settings.highRiskAlerts,
                    onCheckedChange = { viewModel.setHighRiskAlerts(it) }
                )
            }

            item {
                SettingsToggleRow(
                    icon      = Icons.Filled.Notifications,
                    iconColor = Color(0xFFFFA502),
                    title     = "Medium Risk Alerts",
                    subtitle  = if (settings.mediumRiskAlerts) "Notifying for MEDIUM risk apps" else "MEDIUM risk notifications off",
                    checked   = settings.mediumRiskAlerts,
                    onCheckedChange = { viewModel.setMediumRiskAlerts(it) }
                )
            }

            item {
                SettingsClickRow(
                    icon      = Icons.Filled.DoNotDisturb,
                    iconColor = Color(0xFFA4B0BE),
                    title     = "Quiet Hours",
                    subtitle  = if (settings.quietHoursEnabled) "${settings.quietHoursStart}:00 — ${settings.quietHoursEnd}:00" else "Disabled",
                    value     = if (settings.quietHoursEnabled) "On" else "Off",
                    onClick   = { showQuietHoursSheet = true }
                )
            }

            item { SectionHeader(title = "PRIVACY") }

            item {
                SettingsClickRow(
                    icon      = Icons.Filled.AdminPanelSettings,
                    iconColor = if (hasUsagePermission.value) Color(0xFF2ED573) else Color(0xFFFF4757),
                    title     = "Usage Stats Permission",
                    subtitle  = if (hasUsagePermission.value) "Granted — monitoring active" else "Not granted — tap to enable",
                    value     = if (hasUsagePermission.value) "✓" else "!",
                    onClick   = {
                        if (!hasUsagePermission.value) {
                            viewModel.openUsageAccessSettings(context)
                        }
                    }
                )
            }

            item {
                SettingsInfoRow(
                    icon     = Icons.Filled.Lock,
                    iconColor= Color(0xFF4F8EF7),
                    title    = "Local Storage Only",
                    subtitle = "All analysis data stays on your device. Nothing is sent to any server."
                )
            }

            item {
                SettingsDangerRow(
                    icon     = Icons.Filled.DeleteForever,
                    title    = "Clear All Data",
                    subtitle = "Delete all scan results and reset settings",
                    onClick  = { showClearDialog = true }
                )
            }

            item { SectionHeader(title = "ABOUT") }

            item {
                SettingsInfoRow(
                    icon      = Icons.Filled.Info,
                    iconColor = Color(0xFF4F8EF7),
                    title     = "App Version",
                    subtitle  = "SentinelAI v1.0.0"
                )
            }

            item {
                SettingsInfoRow(
                    icon      = Icons.Filled.Analytics,
                    iconColor = Color(0xFF4F8EF7),
                    title     = "AI Model Status",
                    subtitle  = viewModel.getMLStatus()
                )
            }

            item {
                SettingsInfoRow(
                    icon      = Icons.Filled.Group,
                    iconColor = Color(0xFF4F8EF7),
                    title     = "Team",
                    subtitle  = "Gaurav · Ritesh · Sameer · Ayush"
                )
            }
        }
    }

    if (showFrequencySheet) {
        ModalBottomSheet(
            onDismissRequest = { showFrequencySheet = false },
            containerColor   = Color(0xFF111827),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text  = "Scan Frequency",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF1F2F6),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text  = "How often SentinelAI checks your apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA4B0BE)
                )

                Spacer(modifier = Modifier.height(24.dp))

                val options = listOf(
                    2    to "Every 2 minutes (charging only)",
                    5    to "Every 5 minutes  ← recommended",
                    15   to "Every 15 minutes",
                    30   to "Every 30 minutes",
                    9999 to "Manual only"
                )

                options.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setScanFrequency(minutes)
                                showFrequencySheet = false
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.scanFrequencyMins == minutes,
                            onClick  = {
                                viewModel.setScanFrequency(minutes)
                                showFrequencySheet = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor   = Color(0xFF4F8EF7),
                                unselectedColor = Color(0xFF57606F)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (settings.scanFrequencyMins == minutes)
                                        Color(0xFF4F8EF7)
                                    else
                                        Color(0xFFF1F2F6)
                        )
                    }
                    if (minutes != 9999) {
                        HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = Color(0xFF1A2235),
            title = {
                Text(
                    "Clear All Data?",
                    color = Color(0xFFF1F2F6),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will delete all scan results, risk scores, and reset all settings. This cannot be undone.",
                    color = Color(0xFFA4B0BE)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear Everything", color = Color(0xFFFF4757))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color(0xFF4F8EF7))
                }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { 
                onCheckedChange(!checked) 
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color(0xFFF1F2F6) else Color(0xFF57606F),
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA4B0BE)
            )
        }
        Switch(
            checked  = checked,
            onCheckedChange = onCheckedChange,
            enabled  = enabled,
            colors   = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Color(0xFF4F8EF7),
                uncheckedThumbColor = Color(0xFFA4B0BE),
                uncheckedTrackColor = Color(0xFF1A2235)
            )
        )
    }
    HorizontalDivider(
        color = Color(0xFF0A0E1A),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 70.dp)
    )
}

@Composable
fun SettingsClickRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color(0xFFF1F2F6) else Color(0xFF57606F),
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA4B0BE)
            )
        }
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4F8EF7),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF57606F),
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(
        color = Color(0xFF0A0E1A),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 70.dp)
    )
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF1F2F6),
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA4B0BE)
            )
        }
    }
    HorizontalDivider(
        color = Color(0xFF0A0E1A),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 70.dp)
    )
}

@Composable
fun SettingsDangerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFFFF4757).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF4757),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFF4757),
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA4B0BE)
            )
        }
    }
}
