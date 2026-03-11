package com.example.sentine.ui.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sentine.ui.theme.*

@Composable
fun RiskBar(score: Int, level: String, isTrusted: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    val progress by animateFloatAsState(
        targetValue = if (visible) (score / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label = "progressBar"
    )

    val gradient = if (isTrusted) {
        listOf(Primary, Primary.copy(alpha = 0.7f))
    } else {
        when (level) {
            "HIGH" -> listOf(RiskHigh, Color(0xFFFF6B81))
            "MEDIUM" -> listOf(RiskMedium, Color(0xFFFFD32A))
            "LOW" -> listOf(RiskLow, Color(0xFFA8E6CF))
            else -> listOf(RiskSafe, RiskSafe)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x08FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(gradient))
        )
    }
}
