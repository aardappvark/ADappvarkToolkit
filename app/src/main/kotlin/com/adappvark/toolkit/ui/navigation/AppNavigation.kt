package com.adappvark.toolkit.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.adappvark.toolkit.ui.theme.*
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
 * Main app navigation with glass-styled navigation bars
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination?.route?.replaceFirstChar { it.uppercase() } ?: "AardAppvark",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepSpace1.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // Gradient bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SolanaPurple.copy(alpha = 0.4f),
                                    SolanaGreen.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        },
        bottomBar = {
            Column {
                // Gradient top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SolanaPurple.copy(alpha = 0.3f),
                                    SolanaGreen.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) SolanaPurple else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    color = if (isSelected) SolanaPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SolanaPurple,
                                selectedTextColor = SolanaPurple,
                                indicatorColor = SolanaPurple.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        containerColor = DeepSpace1
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { it / 4 },
                    animationSpec = tween(300)
                )
            }
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
                SettingsScreen(
                    onDisconnectWallet = onDisconnectWallet
                )
            }
        }
    }
}
