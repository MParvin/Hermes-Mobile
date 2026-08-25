package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainNavigationTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    CHAT("chat", "Terminal", Icons.Default.Home),
    TOOLS("tools", "Tool Matrix", Icons.Default.Build),
    MEMORY("memory", "Memory & Skills", Icons.Default.Favorite),
    AUTOMATIONS("automations", "Goals & Cron", Icons.Default.DateRange),
    SETTINGS("settings", "Gateway & Keys", Icons.Default.Settings)
}
