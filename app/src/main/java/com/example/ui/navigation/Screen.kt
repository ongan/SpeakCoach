package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Today : Screen("today", "Günün Planı", Icons.Filled.Home, "nav_today")
    object Scenarios : Screen("scenarios", "Pratik", Icons.Filled.CompassCalibration, "nav_scenarios")
    object Chat : Screen("chat", "Sohbet", Icons.AutoMirrored.Filled.Chat, "nav_chat")
    object Notebook : Screen("notebook", "Defterim", Icons.Filled.Book, "nav_notebook")
    object Analytics : Screen("analytics", "İlerleme", Icons.Filled.Insights, "nav_analytics")
    object Settings : Screen("settings", "Ayarlar", Icons.Filled.Settings, "nav_settings")
}
