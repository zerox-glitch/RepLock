package com.replock.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.repository.RepLockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RepLockApp).repository
    private val settings = (application as RepLockApp).settings

    val repsToday: StateFlow<Int> = repository.repsToday
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val repsThisWeek: StateFlow<Int> = repository.repsSince(RepLockRepository.startOfWeekMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val totalReps: StateFlow<Int> = repository.totalReps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val totalUnlocks: StateFlow<Int> = repository.totalUnlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val windowMinutes: StateFlow<Int> = settings.unlockWindowMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    /** Minutes of screen time "earned back" = unlocks × unlock window. */
    val minutesEarned: StateFlow<Int> = combine(totalUnlocks, windowMinutes) { unlocks, minutes ->
        unlocks * minutes
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            _streak.value = repository.currentStreakDays()
        }
    }
}
