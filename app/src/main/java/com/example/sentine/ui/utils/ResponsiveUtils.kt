package com.example.sentine.ui.utils

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class DeviceType { COMPACT, MEDIUM, EXPANDED }

@Composable
fun calculateDeviceType(windowSizeClass: WindowSizeClass?): DeviceType {
    if (windowSizeClass != null) {
        return when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> DeviceType.COMPACT
            WindowWidthSizeClass.Medium -> DeviceType.MEDIUM
            WindowWidthSizeClass.Expanded -> DeviceType.EXPANDED
            else -> DeviceType.COMPACT
        }
    }
    
    // Fallback using configuration if WindowSizeClass is not provided
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp < 600 -> DeviceType.COMPACT
        config.screenWidthDp < 840 -> DeviceType.MEDIUM
        else -> DeviceType.EXPANDED
    }
}
