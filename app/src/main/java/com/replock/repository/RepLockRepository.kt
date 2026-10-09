package com.replock.repository

import com.replock.data.AppDatabase
import com.replock.data.entity.BlockedAppEntity
import com.replock.data.entity.RepSessionEntity
import com.replock.data.entity.UnlockEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single repository layer between the UI/ViewModels/services and the
 * Room database. All blocking state, unlock windows and rep history flow
 * through here.
 */
class RepLockRepository(private val db: AppDatabase) {
    private val blockedAppDao = db.blockedAppDao()
    private val repSessionDao = db.repSessionDao()
    private val unlockEventDao = db.unlockEventDao()

    // ---- Blocklist ----

    val blockedApps: Flow<List<BlockedAppEntity>> = blockedAppDao.getAll()
    val enabledBlockedCount: Flow<Int> = blockedAppDao.enabledCount()

    suspend fun isAppBlocked(packageName: String): Boolean = blockedAppDao.isBlocked(packageName)

    suspend fun setAppBlocked(packageName: String, appName: String, enabled: Boolean) {
        val existing = blockedAppDao.get(packageName)
        if (existing == null) {
            blockedAppDao.upsert(
                BlockedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = enabled,
                    addedAt = System.currentTimeMillis(),
                )
            )
        } else {
            blockedAppDao.upsert(existing.copy(appName = appName, isEnabled = enabled))
        }
    }

    suspend fun removeApp(packageName: String) = blockedAppDao.delete(packageName)

    // ---- Rep history ----

    suspend fun recordRepSession(reps: Int, exercise: String) {
        repSessionDao.insert(
            RepSessionEntity(
                timestamp = System.currentTimeMillis(),
                reps = reps,
                exercise = exercise,
            )
        )
    }

    val repsToday: Flow<Int> = repSessionDao.totalRepsSince(startOfTodayMillis())
    val totalReps: Flow<Int> = repSessionDao.totalReps()

    fun repsSince(millis: Long): Flow<Int> = repSessionDao.totalRepsSince(millis)

    /** Consecutive days with at least one rep session, ending today (or yesterday if today has none yet). */
    suspend fun currentStreakDays(): Int {
        val timestamps = repSessionDao.allTimestamps()
        if (timestamps.isEmpty()) return 0
        val days = timestamps
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedSet()
            .toList()
        var streak = 0
        var cursor = LocalDate.now()
        if (!days.contains(cursor)) cursor = cursor.minusDays(1)
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    // ---- Unlock windows ----

    /** Records that [packageName] is unlocked for [windowMinutes] starting now. */
    suspend fun recordUnlock(packageName: String, windowMinutes: Int) {
        val now = System.currentTimeMillis()
        unlockEventDao.insert(
            UnlockEventEntity(
                packageName = packageName,
                timestamp = now,
                unlockUntil = now + windowMinutes * 60_000L,
            )
        )
    }

    /** True while the latest unlock window for [packageName] is still open. */
    suspend fun isUnlockedNow(packageName: String): Boolean {
        val latest = unlockEventDao.getLatest(packageName) ?: return false
        return latest.unlockUntil > System.currentTimeMillis()
    }

    suspend fun unlocksTodayCount(): Int = unlockEventDao.countSince(startOfTodayMillis())

    fun unlocksTodayFlow(): Flow<Int> = unlockEventDao.countSinceFlow(startOfTodayMillis())

    val totalUnlocks: Flow<Int> = unlockEventDao.totalCount()

    companion object {
        fun startOfTodayMillis(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        fun startOfWeekMillis(): Long =
            LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
