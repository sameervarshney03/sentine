package com.example.sentine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.theme.*

@Composable
fun RiskBadge(level: String, isTrusted: Boolean = false, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseBadge")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (color, dimColor, text) = if (isTrusted) {
        Triple(Primary, PrimaryDim, "TRUSTED")
    } else {
        when (level) {
            "HIGH" -> Triple(RiskHigh, RiskHighDim, "HIGH")
            "MEDIUM" -> Triple(RiskMedium, RiskMediumDim, "MEDIUM")
            "LOW" -> Triple(RiskLow, RiskLowDim, "LOW")
            else -> Triple(RiskSafe, RiskSafeDim, "SAFE")
        }
    }

    val finalAlpha = if (level == "HIGH" && !isTrusted) pulseAlpha else 1f

    Box(
        modifier = modifier
            .alpha(finalAlpha)
            .clip(RoundedCornerShape(100.dp))
            .background(dimColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("● $text", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = AppTypography.bodyLarge.fontFamily)
    }
}
