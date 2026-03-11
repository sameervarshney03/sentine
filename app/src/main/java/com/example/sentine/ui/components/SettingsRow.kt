package com.example.sentine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.theme.*

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconColor: Color = Primary,
    title: String,
    subtitle: String,
    isFirst: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isFirst) {
                    drawLine(
                        color = BorderColor,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontFamily = AppTypography.bodyLarge.fontFamily)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, fontFamily = AppTypography.labelSmall.fontFamily)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        trailing()
    }
}
