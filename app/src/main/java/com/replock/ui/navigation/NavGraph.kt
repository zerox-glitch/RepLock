package com.replock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.replock.ui.rewards.RewardsScreen
import com.replock.ui.settings.SettingsScreen
import com.replock.ui.stats.StatsScreen
import com.replock.ui.theme.RepBlack

@Composable
fun RepLockNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    fun go(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = RepBlack,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Screen.Paywall.route) {
                RepLockBottomBar(currentRoute = currentRoute, onNavigate = ::go)
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
                    onOpenAppPicker = { go(Screen.AppPicker) },
                    onOpenSettings = { go(Screen.Settings) },
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) },
                )
            }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.AppPicker.route) {
                AppPickerScreen(
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                )
            }
            composable(Screen.Rewards.route) {
                RewardsScreen(onOpenPaywall = { navController.navigate(Screen.Paywall.route) })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                )
            }
            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onSubscribed = { navController.popBackStack() },
                    onDismiss = { navController.popBackStack() },
                )
            }
        }
    }
}
