package com.replock.ui.picker

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.data.entity.BlockedAppEntity
import com.replock.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RepLockApp
    private val repository = app.repository
    private val settings = app.settings
    private val packageManager = application.packageManager

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    val blockedApps: StateFlow<List<BlockedAppEntity>> = repository.blockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPro: StateFlow<Boolean> = settings.isProFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Emitted when a free-tier user tries to block more than the free limit. */
    private val _paywallPrompt = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val paywallPrompt: SharedFlow<Unit> = _paywallPrompt.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            val apps = resolveInfos
                .map {
                    InstalledApp(
                        packageName = it.activityInfo.packageName,
                        label = it.loadLabel(packageManager).toString(),
                    )
                }
                .filter { it.packageName != getApplication<Application>().packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            _installedApps.value = apps
        }
    }

    fun toggle(app: InstalledApp, enabled: Boolean) {
        viewModelScope.launch {
            val currentlyEnabled = blockedApps.value.count { it.isEnabled }
            if (enabled && !isPro.value && currentlyEnabled >= FREE_BLOCKED_APPS) {
                _paywallPrompt.tryEmit(Unit) // free tier: 1 blocked app
                return@launch
            }
            repository.setAppBlocked(app.packageName, app.label, enabled)
        }
    }

    companion object {
        const val FREE_BLOCKED_APPS = 1
    }
}
