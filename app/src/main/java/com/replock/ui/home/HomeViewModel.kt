package com.replock.ui.home

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.service.LockMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RepLockApp
    private val repository = app.repository
    private val settings = app.settings

    val blockingEnabled: StateFlow<Boolean> = settings.blockingEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val repsToday: StateFlow<Int> = repository.repsToday
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val unlocksToday: StateFlow<Int> = repository.unlocksTodayFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val isPro: StateFlow<Boolean> = settings.isProFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val enabledBlockedCount: StateFlow<Int> = repository.enabledBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setBlockingEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LockMonitorService::class.java),
                )
            } else {
                context.stopService(Intent(context, LockMonitorService::class.java))
            }
        }
    }
}
