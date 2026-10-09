package com.replock.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.replock.MainActivity
import com.replock.R
import com.replock.RepLockApp
import com.replock.overlay.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that enforces the blocklist.
 *
 * Every 500ms it asks UsageStatsManager for the current foreground app
 * (queryEvents() for MOVE_TO_FOREGROUND / ACTIVITY_RESUMED, with a
 * queryUsageStats() fallback so we also re-lock when an unlock window expires
 * while the user is sitting inside a blocked app). If the foreground package
 * is on the blocklist and its unlock window has expired, it launches the
 * full-screen BlockOverlayActivity with FLAG_ACTIVITY_NEW_TASK.
 */
class LockMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var lastOverlayLaunchElapsedMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LockMonitorService created")
        startForegroundNow()
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    checkForegroundApp()
                } catch (t: Throwable) {
                    Log.e(TAG, "Error while checking foreground app", t)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val app = application as RepLockApp
        if (!app.settings.blockingEnabledFlow.first()) return

        val foreground = queryForegroundPackage() ?: return
        if (foreground == packageName) return // our own app / overlay
        if (!app.repository.isAppBlocked(foreground)) return
        if (app.repository.isUnlockedNow(foreground)) return

        launchOverlay(foreground)
    }

    /**
     * Returns the package currently in the foreground.
     *
     * Primary source: UsageStatsManager.queryEvents() — the most recent
     * MOVE_TO_FOREGROUND / ACTIVITY_RESUMED event within the last couple of
     * seconds. Fallback: the most recently used app from queryUsageStats(),
     * so the lock also re-engages when the unlock window expires while the
     * user stays inside a blocked app (no new events fire in that case).
     */
    private fun queryForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(now - EVENT_WINDOW_MS, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                type == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                if (event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }
        if (latestPackage != null) return latestPackage

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - STATS_WINDOW_MS,
            now,
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun launchOverlay(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastOverlayLaunchElapsedMs < MIN_RELAUNCH_INTERVAL_MS) return
        lastOverlayLaunchElapsedMs = now

        val appLabel = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }

        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockOverlayActivity.EXTRA_APP_NAME, appLabel)
        }
        // singleTask + NEW_TASK: if the overlay is already alive (e.g. the user
        // pressed Home), this brings it back to the front instead of stacking.
        BlockOverlayActivity.activeOverlayPackage = packageName
        startActivity(intent)
        Log.i(TAG, "Overlay launched for $packageName")
    }

    private fun startForegroundNow() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RepLock is active")
            .setContentText("Blocked apps stay locked until you do your reps")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    companion object {
        const val CHANNEL_ID = "replock_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 500L
        private const val EVENT_WINDOW_MS = 2_000L
        private const val STATS_WINDOW_MS = 10 * 60_000L
        private const val MIN_RELAUNCH_INTERVAL_MS = 1_000L
        private const val TAG = "LockMonitorService"
    }
}
