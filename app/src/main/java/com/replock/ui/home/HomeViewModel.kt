package com.replock.ui.home

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.data.SettingsDataStore
import com.replock.repository.RepLockRepository
import com.replock.service.LockMonitorService
import com.replock.util.PermissionUtils
import com.replock.util.bucketByDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RepLockApp
    private val repository = app.repository
    private val settings = app.settings

    init {
        // Safety net: make sure the monitor service is running whenever Home is
        // shown with blocking enabled and permissions granted. Covers the first
        // launch right after onboarding and any process restart. Starting an
        // already-running service is a no-op.
        viewModelScope.launch {
            if (settings.blockingEnabledFlow.first() &&
                PermissionUtils.hasAllBlockingPermissions(getApplication())
            ) {
                ContextCompat.startForegroundService(
                    getApplication(),
                    Intent(getApplication(), LockMonitorService::class.java),
                )
            }
        }
    }

    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val blockingEnabled: StateFlow<Boolean> = settings.blockingEnabledFlow
        .stateIn(viewModelScope, sharing, true)
    val repsToday: StateFlow<Int> = repository.repsToday
        .stateIn(viewModelScope, sharing, 0)
    val unlocksToday: StateFlow<Int> = repository.unlocksTodayFlow()
        .stateIn(viewModelScope, sharing, 0)
    val isPro: StateFlow<Boolean> = settings.isProFlow
        .stateIn(viewModelScope, sharing, false)
    val enabledBlockedCount: StateFlow<Int> = repository.enabledBlockedCount
        .stateIn(viewModelScope, sharing, 0)
    val unlockWindowMinutes: StateFlow<Int> = settings.unlockWindowMinutesFlow
        .stateIn(viewModelScope, sharing, SettingsDataStore.DEFAULT_UNLOCK_WINDOW_MINUTES)
    val repTarget: StateFlow<Int> = settings.repTargetFlow
        .stateIn(viewModelScope, sharing, SettingsDataStore.DEFAULT_REP_TARGET)

    /** Package names of enabled blocked apps, for the icon row on Home. */
    val blockedPackages: StateFlow<List<String>> = repository.blockedApps
        .map { apps -> apps.filter { it.isEnabled }.map { it.packageName } }
        .stateIn(viewModelScope, sharing, emptyList())

    /** Reps per day for the last 7 days (oldest first); the last entry is today. */
    val repsLast7: StateFlow<List<Int>> = repository
        .repSessionsSince(RepLockRepository.startOfDaysAgoMillis(7))
        .map { sessions -> bucketByDay(sessions.map { it.timestamp to it.reps }, 7) }
        .stateIn(viewModelScope, sharing, emptyList())

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
