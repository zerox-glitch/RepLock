package com.replock.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.repository.RepLockRepository
import com.replock.util.bucketByDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class StatsRange(val label: String, val days: Int) {
    Week("Week", 7),
    Month("Month", 30),
    AllTime("All Time", 90),
}

/** Everything the Stats screen shows for the selected range. */
data class StatsUi(
    val range: StatsRange = StatsRange.Week,
    val reps: Int = 0,
    val repsDelta: Int = 0,
    val minutesEarned: Int = 0,
    val minutesDelta: Int = 0,
    val repSeries: List<Int> = emptyList(),
    val unlockSeries: List<Int> = emptyList(),
    val last7Reps: List<Int> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RepLockApp
    private val repository = app.repository
    private val settings = app.settings
    private val sharing = SharingStarted.WhileSubscribed(5_000)

    private val _range = MutableStateFlow(StatsRange.Week)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    fun setRange(range: StatsRange) {
        _range.value = range
    }

    /** History is loaded for two times the longest range so the previous period gives the delta. */
    private val sessions = repository.repSessionsSince(RepLockRepository.startOfDaysAgoMillis(HISTORY_DAYS))
    private val unlocks = repository.unlockEventsSince(RepLockRepository.startOfDaysAgoMillis(HISTORY_DAYS))
    private val windowMinutes = settings.unlockWindowMinutesFlow

    val ui: StateFlow<StatsUi> = combine(sessions, unlocks, _range, windowMinutes) { s, u, range, window ->
        val n = range.days
        val repsBuckets = bucketByDay(s.map { it.timestamp to it.reps }, 2 * n)
        val unlockBuckets = bucketByDay(u.map { it.timestamp to 1 }, 2 * n)

        val repsCurrent = repsBuckets.takeLast(n)
        val repsPrevious = repsBuckets.take(n)
        val unlocksCurrent = unlockBuckets.takeLast(n)
        val unlocksPrevious = unlockBuckets.take(n)

        StatsUi(
            range = range,
            reps = repsCurrent.sum(),
            repsDelta = repsCurrent.sum() - repsPrevious.sum(),
            minutesEarned = unlocksCurrent.sum() * window,
            minutesDelta = (unlocksCurrent.sum() - unlocksPrevious.sum()) * window,
            repSeries = repsCurrent,
            unlockSeries = unlocksCurrent,
            last7Reps = repsBuckets.takeLast(7),
        )
    }.stateIn(viewModelScope, sharing, StatsUi())

    companion object {
        const val HISTORY_DAYS = 180
    }
}
