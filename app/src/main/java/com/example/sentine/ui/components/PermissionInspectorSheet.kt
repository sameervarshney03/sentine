package com.example.sentine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sentine.ui.theme.*
import com.example.sentine.viewmodel.AppPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionInspectorSheet(
    permissions: List<AppPermission>,
    onDismissRequest: () -> Unit
) {
    val dangerousCount = permissions.count { it.category == "DANGEROUS" }
    val networkCount = permissions.count { it.category == "NETWORK" }
    val normalCount = permissions.count { it.category == "NORMAL" }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Permission Inspector",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dangerousCount > 0) PermissionStatBadge("$dangerousCount dangerous", RiskHigh)
                if (networkCount > 0) PermissionStatBadge("$networkCount network", RiskMedium)
                if (normalCount > 0) PermissionStatBadge("$normalCount normal", TextSecondary)
            }

            HorizontalDivider(color = BorderColor)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(permissions, key = { it.name }) { perm ->
                    var isExpanded by remember { mutableStateOf(false) }

                    val (icon, color) = when (perm.category) {
                        "DANGEROUS" -> Icons.Default.Security to RiskHigh
                        "NETWORK" -> Icons.Default.Public to RiskMedium
                        else -> Icons.Default.Settings to TextSecondary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (perm.category == "DANGEROUS" || perm.description.isNotEmpty()) isExpanded = !isExpanded }
                            .padding(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = perm.humanReadableName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text(text = perm.name.substringAfterLast("."), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            
                            Surface(
                                color = color.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = perm.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 52.dp, end = 8.dp)
                                    .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    if (perm.category == "DANGEROUS") {
                                        Text("Why is this dangerous?", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Text(perm.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
