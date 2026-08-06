package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Chat : Screen("chat", "Coach", Icons.AutoMirrored.Filled.Chat, "nav_chat")
    object Scenarios : Screen("scenarios", "Scenarios", Icons.Filled.CompassCalibration, "nav_scenarios")
    object Notebook : Screen("notebook", "Notebook", Icons.Filled.Book, "nav_notebook")
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.Insights, "nav_analytics")
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, "nav_settings")
}
