package com.replock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.replock.ui.home.HomeScreen
import com.replock.ui.paywall.PaywallScreen
import com.replock.ui.picker.AppPickerScreen
import com.replock.ui.settings.SettingsScreen
import com.replock.ui.stats.StatsScreen
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepSurfaceVariant

@Composable
fun RepLockNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val items = listOf(Screen.Home, Screen.AppPicker, Screen.Settings, Screen.Stats)
    Scaffold(
        modifier = modifier,
        containerColor = RepBlack,
        bottomBar = {
            NavigationBar(containerColor = RepBlack) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Home -> Icons.Filled.Home
                                    Screen.AppPicker -> Icons.Filled.Block
                                    Screen.Settings -> Icons.Filled.Settings
                                    Screen.Stats -> Icons.Filled.ShowChart
                                    else -> Icons.Filled.Home
                                },
                                contentDescription = screen.title,
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricGreen,
                            selectedTextColor = ElectricGreen,
                            unselectedIconColor = com.replock.ui.theme.RepGray,
                            unselectedTextColor = com.replock.ui.theme.RepGray,
                            indicatorColor = RepSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenAppPicker = { navController.navigate(Screen.AppPicker.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenStats = { navController.navigate(Screen.Stats.route) },
                )
            }
            composable(Screen.AppPicker.route) {
                AppPickerScreen(
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                )
            }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onSubscribed = { navController.popBackStack() },
                    onDismiss = { navController.popBackStack() },
                )
            }
        }
    }
}
