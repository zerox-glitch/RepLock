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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RepLockApp).repository
    private val settings = (application as RepLockApp).settings

    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val repsToday: StateFlow<Int> = repository.repsToday.stateIn(viewModelScope, sharing, 0)
    val repsThisWeek: StateFlow<Int> = repository
        .repsSince(RepLockRepository.startOfWeekMillis())
        .stateIn(viewModelScope, sharing, 0)
    val totalReps: StateFlow<Int> = repository.totalReps.stateIn(viewModelScope, sharing, 0)
    val totalUnlocks: StateFlow<Int> = repository.totalUnlocks.stateIn(viewModelScope, sharing, 0)

    private val windowMinutes: StateFlow<Int> = settings.unlockWindowMinutesFlow
        .stateIn(viewModelScope, sharing, 5)

    private val sessions14 = repository
        .repSessionsSince(RepLockRepository.startOfDaysAgoMillis(14))
    private val unlocks14 = repository
        .unlockEventsSince(RepLockRepository.startOfDaysAgoMillis(14))

    /** Reps per day, last 14 days (oldest first). */
    val repsLast14: StateFlow<List<Int>> = sessions14
        .map { list -> bucketByDay(list.map { it.timestamp to it.reps }, 14) }
        .stateIn(viewModelScope, sharing, emptyList())

    /** Unlocks per day, last 14 days (oldest first). */
    val unlocksLast14: StateFlow<List<Int>> = unlocks14
        .map { list -> bucketByDay(list.map { it.timestamp to 1 }, 14) }
        .stateIn(viewModelScope, sharing, emptyList())

    val repsLast7: StateFlow<List<Int>> = repsLast14
        .map { it.takeLast(7) }
        .stateIn(viewModelScope, sharing, emptyList())

    val unlocksLast7: StateFlow<List<Int>> = unlocksLast14
        .map { it.takeLast(7) }
        .stateIn(viewModelScope, sharing, emptyList())

    /** 1 on days with any rep activity, else 0 — the streak sparkline. */
    val activeDaysLast7: StateFlow<List<Int>> = repsLast7
        .map { days -> days.map { if (it > 0) 1 else 0 } }
        .stateIn(viewModelScope, sharing, emptyList())

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    /** Minutes of screen time "earned back" = unlocks × unlock window. */
    val minutesEarned: StateFlow<Int> = combine(totalUnlocks, windowMinutes) { unlocks, minutes ->
        unlocks * minutes
    }.stateIn(viewModelScope, sharing, 0)

    init {
        viewModelScope.launch {
            _streak.value = repository.currentStreakDays()
        }
    }
}
