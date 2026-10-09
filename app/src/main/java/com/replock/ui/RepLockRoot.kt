package com.replock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.replock.data.SettingsDataStore
import com.replock.ui.navigation.RepLockNavHost
import com.replock.ui.onboarding.OnboardingScreen

/** Root composable: onboarding until completed, then the main app. */
@Composable
fun RepLockRoot(settings: SettingsDataStore) {
    val onboardingDone by settings.onboardingDoneFlow.collectAsState(initial = null)
    when (onboardingDone) {
        null -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        false -> OnboardingScreen(settings = settings)
        true -> RepLockNavHost()
    }
}
