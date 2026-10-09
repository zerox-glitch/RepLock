package com.replock.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object AppPicker : Screen("app_picker", "Blocked Apps")
    data object Settings : Screen("settings", "Settings")
    data object Stats : Screen("stats", "Stats")
    data object Paywall : Screen("paywall", "RepLock Pro")
}
