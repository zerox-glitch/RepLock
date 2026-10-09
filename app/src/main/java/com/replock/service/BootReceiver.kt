package com.replock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.replock.data.SettingsDataStore
import com.replock.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Restarts the monitor service after a reboot. Unlock windows are persisted in
 * Room, so previously granted unlocks keep working. If the platform refuses a
 * background foreground-service start, the service starts the next time the
 * user opens RepLock (see RepLockApp.onCreate).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsDataStore(context)
                if (settings.isBlockingEnabledNow() &&
                    PermissionUtils.hasAllBlockingPermissions(context)
                ) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, LockMonitorService::class.java),
                    )
                    Log.i(TAG, "LockMonitorService restarted after boot")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Could not restart service after boot", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
