package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NotebookScreen
import com.example.ui.screens.ScenariosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SpeakCoachTheme
import com.example.ui.viewmodel.CoachViewModel

import com.example.ui.screens.TodayScreen

class MainActivity : ComponentActivity() {

    private val viewModel: CoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeakCoachTheme {
                MainAppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppShell(viewModel: CoachViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiState by viewModel.uiState.collectAsState()

    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showProfileSelectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isOnboardingCompleted, uiState.profiles) {
        if (!uiState.isOnboardingCompleted && uiState.profiles.isEmpty()) {
            showOnboardingDialog = true
        }
    }

    if (showOnboardingDialog) {
        com.example.ui.components.OnboardingDialog(
            viewModel = viewModel,
            onDismiss = { showOnboardingDialog = false }
        )
    }

    if (showProfileSelectionDialog) {
        com.example.ui.components.ProfileSelectionDialog(
            profiles = uiState.profiles,
            activeProfileId = uiState.activeProfileId,
            onSelectProfile = { profile ->
                viewModel.selectProfile(profile)
                showProfileSelectionDialog = false
            },
            onAddNewProfile = {
                showProfileSelectionDialog = false
                showOnboardingDialog = true
            },
            onDeleteProfile = { profile ->
                viewModel.deleteProfile(profile)
            },
            onDismiss = if (uiState.profiles.isNotEmpty()) {
                { showProfileSelectionDialog = false }
            } else null
        )
    }

    val navigationItems = listOf(
        Screen.Today,
        Screen.Scenarios,
        Screen.Chat,
        Screen.Notebook,
        Screen.Analytics,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                navigationItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToScenarios = { navController.navigate(Screen.Scenarios.route) },
                    onNavigateToNotebook = { navController.navigate(Screen.Notebook.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenProfileSelection = { showProfileSelectionDialog = true }
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onOpenProfileSelection = {
                        showProfileSelectionDialog = true
                    }
                )
            }
            composable(Screen.Scenarios.route) {
                ScenariosScreen(
                    viewModel = viewModel,
                    onScenarioStart = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    }
                )
            }
            composable(Screen.Notebook.route) {
                NotebookScreen(viewModel = viewModel)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
