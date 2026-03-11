package com.example.sentine.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sentine.ui.components.RiskBadge
import com.example.sentine.ui.components.RiskBar
import com.example.sentine.ui.theme.*
import com.example.sentine.viewmodel.AppDetailViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun AppDetailScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: AppDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }
    
    LaunchedEffect(packageName) { viewModel.loadAppDetails(packageName) }
    
    val appState by viewModel.appRisk.collectAsState()
    val app = appState ?: return

    val iconDrawable = remember(packageName) {
        try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
    }
    
    val riskColor = if (app.isTrusted) Primary else when(app.riskLevel) {
        "HIGH" -> RiskHigh; "MEDIUM" -> RiskMedium; "LOW" -> RiskLow; else -> RiskSafe
    }

    Scaffold(
        containerColor = Color(0xFF0A0E1A),
        bottomBar = {
            val trustedBg = Color(0xFF2ED573).copy(alpha = 0.2f)
            val trustedContent = Color(0xFF2ED573)
            val untrustedBg = Color(0xFF4F8EF7).copy(alpha = 0.2f)
            val untrustedContent = Color(0xFF4F8EF7)

            Box(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (app.isTrusted) {
                    Button(
                        onClick = { viewModel.toggleTrusted(app.packageName) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = trustedBg, contentColor = trustedContent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trusted — Tap to Remove", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.toggleTrusted(app.packageName) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, untrustedContent),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = untrustedBg, contentColor = untrustedContent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = untrustedContent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Trusted", fontWeight = FontWeight.Bold, color = untrustedContent)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            
            // HERO SECTION
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                if (iconDrawable != null) {
                    AsyncImage(
                        model = iconDrawable, contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.15f)
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0E1A)))))
                
                // TOP HEADER inside Hero
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, riskColor, CircleShape).background(SurfaceVariant), contentAlignment = Alignment.Center) {
                        if (iconDrawable != null) AsyncImage(model = iconDrawable, contentDescription = null, modifier = Modifier.fillMaxSize())
                        else Text(app.appName.take(1), color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = AppTypography.bodyLarge.fontFamily)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(app.appName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = AppTypography.bodyLarge.fontFamily)
                    val displayPkg = if (app.packageName.isBlank()) "Unknown package" else app.packageName
                    Text(displayPkg, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    RiskBadge(app.riskLevel, app.isTrusted)
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                // SCORE SECTION
                var animStart by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { animStart = true }
                val scoreAnim by animateIntAsState(if (animStart) app.riskScore else 0, tween(1000, easing = FastOutSlowInEasing), "score")
                
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("RISK SCORE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = AppTypography.bodyLarge.fontFamily)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(scoreAnim.toString(), color = riskColor, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AppTypography.displayLarge.fontFamily)
                            Text(" / 100", color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp), fontFamily = AppTypography.bodyLarge.fontFamily)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val verdict = when(app.riskLevel) { "HIGH" -> "Uploading data silently in background"; "MEDIUM" -> "Unusual background patterns detected"; "LOW" -> "Minor background usage, generally safe"; else -> "Safe behavior patterns" }
                        Text("\"$verdict\"", color = TextSecondary, fontSize = 14.sp, fontStyle = FontStyle.Italic, fontFamily = AppTypography.bodyLarge.fontFamily)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val mlStatusText = when {
                                app.mlScore < 0  -> "ML: Unavailable"
                                app.mlScore == 0 -> "ML: Safe"
                                else             -> "ML: ${app.mlScore}"
                            }
                            val mlStatusColor = when {
                                app.mlScore < 0  -> Color(0xFF57606F)
                                app.mlScore > 75 -> Color(0xFFFF4757)
                                app.mlScore > 50 -> Color(0xFFFFA502)
                                else             -> Color(0xFF7BED9F)
                            }

                            ScoreChip(label = mlStatusText, color = mlStatusColor)
                            ScoreSmallChip("Anomaly", app.anomalyScore)
                            ScoreSmallChip("Rules", app.networkScore) // simplified
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // SIGNAL BREAKDOWN
                Text("What We Detected", color = TextPrimary, style = AppTypography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).border(1.dp, BorderColor, RoundedCornerShape(16.dp))) {
                    SignalRow("📡", "Network Upload", app.networkScore, riskColor)
                    HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                    SignalRow("👁", "Background Activity", app.backgroundScore, riskColor)
                    HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                    SignalRow("⚙️", "Foreground Service", app.serviceScore, riskColor)
                    HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                    SignalRow("🌙", "Screen-Off Activity", app.screenOffScore, riskColor)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // PERMISSIONS SECTION
                PermissionsCard(packageName, pm)

                Spacer(modifier = Modifier.height(24.dp))

                // REASONS
                Text("Why This Score", color = TextPrimary, style = AppTypography.titleLarge, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).border(1.dp, BorderColor, RoundedCornerShape(16.dp))) {
                    val reasons = app.reasons.split("|").filter { it.isNotBlank() }
                    if (reasons.isEmpty()) {
                        Text("No specific threat reasons detected.", color = TextMuted, modifier = Modifier.padding(16.dp), fontFamily = AppTypography.bodyLarge.fontFamily)
                    } else {
                        reasons.forEachIndexed { idx, reason ->
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                val icon = if (app.riskLevel == "HIGH") "⚠️" else if (app.riskLevel == "SAFE") "✅" else "ℹ️"
                                Text(icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(reason.trim(), color = TextPrimary, fontSize = 14.sp, fontFamily = AppTypography.bodyLarge.fontFamily)
                            }
                            if (idx < reasons.size - 1) HorizontalDivider(color = Color(0xFF0A0E1A), thickness = 1.dp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp)) // some bottom padding
            }
        }
    }
}

@Composable
fun PermissionsCard(packageName: String, pm: PackageManager) {
    val packageInfo = remember(packageName) {
        try { pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS) } catch (e: Exception) { null }
    }
    val requestedPermissions = packageInfo?.requestedPermissions?.toList() ?: emptyList()

    val dangerousKeywords = listOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "WRITE_CONTACTS", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS", "READ_SMS", "RECEIVE_SMS", "SEND_SMS", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "GET_ACCOUNTS", "BODY_SENSORS", "ACTIVITY_RECOGNITION", "BLUETOOTH_SCAN", "NEARBY_WIFI_DEVICES", "USE_BIOMETRIC")
    val networkKeywords = listOf("INTERNET", "CHANGE_WIFI_STATE", "ACCESS_WIFI_STATE", "ACCESS_NETWORK_STATE", "CHANGE_NETWORK_STATE", "NFC", "BLUETOOTH", "BLUETOOTH_ADMIN", "BLUETOOTH_CONNECT", "RECEIVE_BOOT_COMPLETED", "FOREGROUND_SERVICE", "REQUEST_INSTALL_PACKAGES", "PACKAGE_USAGE_STATS")

    val dangerousPerms = mutableListOf<String>()
    val networkPerms = mutableListOf<String>()
    val normalPerms = mutableListOf<String>()

    requestedPermissions.forEach { perm ->
        if (dangerousKeywords.any { perm.contains(it) }) dangerousPerms.add(perm)
        else if (networkKeywords.any { perm.contains(it) }) networkPerms.add(perm)
        else normalPerms.add(perm)
    }

    val colorDangerous = Color(0xFFFF4757)
    val colorNetwork = Color(0xFFFFA502)
    val colorNormal = Color(0xFFA4B0BE)

    var normalExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2235))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // HEADER ROW
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Permissions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text("${dangerousPerms.size} dangerous", color = colorDangerous, fontSize = 12.sp)
            Text(" • ", color = TextMuted, fontSize = 12.sp)
            Text("${networkPerms.size} network", color = colorNetwork, fontSize = 12.sp)
            Text(" • ", color = TextMuted, fontSize = 12.sp)
            Text("${normalPerms.size} normal", color = colorNormal, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (requestedPermissions.isEmpty()) {
            Text("No permissions requested", color = TextMuted, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            // DANGEROUS SECTION
            Text("DANGEROUS", color = colorDangerous, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=12.dp, bottom=4.dp))
            if (dangerousPerms.isEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅ No dangerous permissions", color = Color(0xFF2ED573), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                dangerousPerms.forEach { perm ->
                    PermissionRow(getHumanReadablePermission(perm), getPermissionIcon(perm), colorDangerous)
                }
            }

            // NETWORK SECTION
            if (networkPerms.isNotEmpty()) {
                Text("NETWORK", color = colorNetwork, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=12.dp, bottom=4.dp))
                networkPerms.forEach { perm ->
                    PermissionRow(getHumanReadablePermission(perm), getPermissionIcon(perm), colorNetwork)
                }
            }

            // NORMAL PERMISSIONS SECTION
            if (normalPerms.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top=12.dp, bottom=4.dp).clickable { normalExpanded = !normalExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NORMAL", color = colorNormal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (normalExpanded) "[Show less ▴]" else "[Show ${normalPerms.size} more ▾]", color = TextMuted, fontSize = 12.sp)
                }
                
                AnimatedVisibility(visible = normalExpanded) {
                    Column {
                        normalPerms.forEach { perm ->
                            PermissionRow(getHumanReadablePermission(perm), getPermissionIcon(perm), colorNormal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(name: String, icon: ImageVector, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = Color(0x08FFFFFF), thickness = 1.dp)
    }
}

fun getHumanReadablePermission(perm: String): String {
    return when {
        perm.contains("CAMERA") -> "Camera"
        perm.contains("RECORD_AUDIO") -> "Microphone"
        perm.contains("ACCESS_FINE_LOCATION") -> "Precise Location"
        perm.contains("ACCESS_COARSE_LOCATION") -> "Approximate Location"
        perm.contains("READ_CONTACTS") -> "Read Contacts"
        perm.contains("WRITE_CONTACTS") -> "Edit Contacts"
        perm.contains("READ_SMS") -> "Read SMS Messages"
        perm.contains("SEND_SMS") -> "Send SMS Messages"
        perm.contains("READ_CALL_LOG") -> "Call History"
        perm.contains("READ_PHONE_STATE") -> "Phone State & Number"
        perm.contains("READ_EXTERNAL_STORAGE") -> "Read Files & Media"
        perm.contains("WRITE_EXTERNAL_STORAGE") -> "Write Files & Media"
        perm.contains("INTERNET") -> "Internet Access"
        perm.contains("FOREGROUND_SERVICE") -> "Background Service"
        perm.contains("RECEIVE_BOOT_COMPLETED") -> "Auto-start on Boot"
        perm.contains("REQUEST_INSTALL_PACKAGES") -> "Install Other Apps"
        perm.contains("PACKAGE_USAGE_STATS") -> "App Usage Access"
        perm.contains("BODY_SENSORS") -> "Body Sensors"
        perm.contains("USE_BIOMETRIC") -> "Biometric Data"
        perm.contains("ACCESS_WIFI_STATE") -> "Wi-Fi Information"
        perm.contains("BLUETOOTH_SCAN") -> "Nearby Devices Scan"
        perm.contains("GET_ACCOUNTS") -> "Device Accounts"
        else -> perm.substringAfter("android.permission.")
                .replace("_", " ")
                .lowercase()
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                .trim()
    }
}

fun getPermissionIcon(perm: String): ImageVector {
    return when {
        perm.contains("CAMERA") -> Icons.Filled.CameraAlt
        perm.contains("RECORD_AUDIO") -> Icons.Filled.Mic
        perm.contains("LOCATION") -> Icons.Filled.LocationOn
        perm.contains("CONTACTS") -> Icons.Filled.Contacts
        perm.contains("SMS") -> Icons.Filled.Sms
        perm.contains("PHONE") || perm.contains("CALL") -> Icons.Filled.Phone
        perm.contains("STORAGE") -> Icons.Filled.Folder
        perm.contains("INTERNET") -> Icons.Filled.Language
        perm.contains("BOOT_COMPLETED") || perm.contains("SERVICE") -> Icons.Filled.Settings
        perm.contains("INSTALL") -> Icons.Filled.GetApp
        perm.contains("BIOMETRIC") -> Icons.Filled.Fingerprint
        perm.contains("SENSOR") || perm.contains("ACTIVITY") -> Icons.Filled.Sensors
        perm.contains("WIFI") -> Icons.Filled.Wifi
        perm.contains("BLUETOOTH") -> Icons.Filled.Bluetooth
        else -> Icons.Filled.Security
    }
}

@Composable
fun ScoreSmallChip(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x0AFFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, color = TextMuted, fontSize = 11.sp, fontFamily = AppTypography.bodyLarge.fontFamily)
        Text(score.toString(), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = AppTypography.bodyLarge.fontFamily)
    }
}

@Composable
fun ScoreChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SignalRow(emoji: String, label: String, score: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = TextPrimary, style = AppTypography.bodyLarge, modifier = Modifier.weight(1f))
        Text(score.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = AppTypography.bodyLarge.fontFamily)
    }
}
