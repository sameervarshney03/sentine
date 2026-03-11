package com.example.sentine.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.theme.AppTypography
import com.example.sentine.ui.theme.TextPrimary
import com.example.sentine.ui.theme.TextSecondary

@Composable
fun EmptyState(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontFamily = AppTypography.bodyLarge.fontFamily
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            fontFamily = AppTypography.bodyLarge.fontFamily
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(32.dp))
            action()
        }
    }
}
