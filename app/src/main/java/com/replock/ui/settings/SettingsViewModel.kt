package com.replock.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = (application as RepLockApp).settings

    val repTarget: StateFlow<Int> = settings.repTargetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsDataStore.DEFAULT_REP_TARGET)
    val unlockWindowMinutes: StateFlow<Int> = settings.unlockWindowMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsDataStore.DEFAULT_UNLOCK_WINDOW_MINUTES)
    val difficulty: StateFlow<String> = settings.difficultyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsDataStore.DIFFICULTY_PUSHUPS)
    val isPro: StateFlow<Boolean> = settings.isProFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setRepTarget(target: Int) = viewModelScope.launch { settings.setRepTarget(target) }
    fun setUnlockWindowMinutes(minutes: Int) =
        viewModelScope.launch { settings.setUnlockWindowMinutes(minutes) }
    fun setDifficulty(difficulty: String) =
        viewModelScope.launch { settings.setDifficulty(difficulty) }
}
