package com.adappvark.toolkit.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adappvark.toolkit.ui.screens.HomeScreen
import com.adappvark.toolkit.ui.screens.UninstallScreen
import com.adappvark.toolkit.ui.screens.ReinstallScreen
import com.adappvark.toolkit.ui.screens.SettingsScreen
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

/**
 * Navigation routes
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Uninstall : Screen("uninstall", "Uninstall", Icons.Filled.Delete)
    object Reinstall : Screen("reinstall", "Reinstall", Icons.Filled.Download)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

/**
 * Main app navigation with bottom nav bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    activityResultSender: ActivityResultSender,
    onDisconnectWallet: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Home,
        Screen.Uninstall,
        Screen.Reinstall,
        Screen.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDestination?.route?.replaceFirstChar { it.uppercase() } ?: "AardAppvark"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onDisconnectWallet = onDisconnectWallet)
            }
            composable(Screen.Uninstall.route) {
                UninstallScreen(activityResultSender = activityResultSender)
            }
            composable(Screen.Reinstall.route) {
                ReinstallScreen(activityResultSender = activityResultSender)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onDisconnectWallet = onDisconnectWallet)
            }
        }
    }
}
