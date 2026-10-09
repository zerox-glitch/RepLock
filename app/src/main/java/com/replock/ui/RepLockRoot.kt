package com.replock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.replock.data.SettingsDataStore
import com.replock.ui.navigation.RepLockNavHost
import com.replock.ui.onboarding.OnboardingScreen

/**
 * Root composable: brief splash while settings load, then onboarding until
 * completed, then the main app.
 *
 * `null` means "settings not loaded yet". The flow itself is non-null
 * (defaults to false), so a fresh install lands on onboarding — and because
 * we COLLECT the flow (not read it once), completing onboarding flips the
 * root to the main app immediately.
 */
@Composable
fun RepLockRoot(settings: SettingsDataStore) {
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        settings.onboardingDoneFlow.collect { onboardingDone = it }
    }
    when (onboardingDone) {
        null -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        false -> OnboardingScreen(settings = settings)
        true -> RepLockNavHost()
    }
}
