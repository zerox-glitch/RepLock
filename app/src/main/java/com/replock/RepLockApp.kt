package com.replock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.ContextCompat
import com.replock.data.AppDatabase
import com.replock.data.SettingsDataStore
import com.replock.repository.RepLockRepository
import com.replock.service.LockMonitorService
import com.replock.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RepLockApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { RepLockRepository(database) }
    val settings by lazy { SettingsDataStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Auto-start the blocking service whenever the app process starts,
        // as long as the user has it enabled and granted the special permissions.
        appScope.launch {
            val blockingEnabled = settings.blockingEnabledFlow.first()
            if (blockingEnabled && PermissionUtils.hasAllBlockingPermissions(this@RepLockApp)) {
                ContextCompat.startForegroundService(
                    this@RepLockApp,
                    Intent(this@RepLockApp, LockMonitorService::class.java),
                )
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            LockMonitorService.CHANNEL_ID,
            "App blocking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps RepLock active so blocked apps stay locked"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
