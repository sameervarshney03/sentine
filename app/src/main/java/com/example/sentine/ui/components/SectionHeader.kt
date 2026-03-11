package com.example.sentine.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentine.ui.theme.AppTypography
import com.example.sentine.ui.theme.TextMuted

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = AppTypography.labelSmall,
        color = TextMuted,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    )
}
