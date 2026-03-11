package com.example.sentine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sentine.ui.theme.AppTypography
import com.example.sentine.ui.theme.Primary

@Composable
fun AppIcon(packageName: String, appName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val iconDrawable = remember(packageName) {
        try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
    }
    var iconFailed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (iconFailed || iconDrawable == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Primary),
                contentAlignment = Alignment.Center
            ) {
                val initial = if (appName.isNotBlank()) appName.take(1).uppercase() else "?"
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4f).sp,
                    fontFamily = AppTypography.bodyLarge.fontFamily
                )
            }
        } else {
            AsyncImage(
                model = iconDrawable,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize(),
                onError = { iconFailed = true }
            )
        }
    }
}
