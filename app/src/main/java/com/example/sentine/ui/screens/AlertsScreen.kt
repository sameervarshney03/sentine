package com.example.sentine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.components.EmptyState
import com.example.sentine.ui.components.RiskBadge
import com.example.sentine.ui.components.SectionHeader
import com.example.sentine.ui.theme.*

@Composable
fun AlertsScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alerts", style = AppTypography.headlineMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.clip(CircleShape).background(RiskHigh).padding(horizontal=8.dp, vertical=2.dp)) {
                        Text("0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { /* Clear All */ }) {
                    Text("Clear All", color = TextMuted, fontSize = 14.sp)
                }
            }
        }
        
        HorizontalDivider(color = DividerColor)
        
        // Demonstrating empty state since we haven't implemented a real alerts table
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            iconColor = SuccessColor,
            title = "All Clear",
            subtitle = "No alerts in the last 7 days",
            modifier = Modifier.weight(1f)
        )
    }
}
