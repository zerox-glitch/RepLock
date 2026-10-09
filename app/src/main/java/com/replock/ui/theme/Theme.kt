package com.replock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RepColorScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = RepBlack,
    secondary = ElectricGreen,
    onSecondary = RepBlack,
    background = RepBlack,
    onBackground = RepWhite,
    surface = RepSurface,
    onSurface = RepWhite,
    surfaceVariant = RepSurfaceVariant,
    onSurfaceVariant = RepGray,
    error = LockedRed,
    onError = RepWhite,
)

@Composable
fun RepLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RepColorScheme, content = content)
}
