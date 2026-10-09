package com.replock.ui.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One achievement row. [progress] is 0..1; [done] when it reaches 1. */
data class Milestone(
    val title: String,
    val subtitle: String,
    val progress: Float,
) {
    val done: Boolean get() = progress >= 1f
}

data class RewardsUi(
    val level: Int = 1,
    val levelTitle: String = "Rookie",
    val xpIntoLevel: Int = 0,
    val xpPerLevel: Int = RewardsViewModel.XP_PER_LEVEL,
    val totalXp: Int = 0,
    val isPro: Boolean = false,
    val milestones: List<Milestone> = emptyList(),
)

/**
 * XP is derived from real history only: each rep is worth [XP_PER_REP] and each
 * unlock [XP_PER_UNLOCK]. Level = xp / [XP_PER_LEVEL] + 1.
 */
class RewardsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RepLockApp
    private val repository = app.repository
    private val settings = app.settings
    private val sharing = SharingStarted.WhileSubscribed(5_000)

    private val streak = MutableStateFlow(0)

    init {
        viewModelScope.launch { streak.value = repository.currentStreakDays() }
    }

    val ui: StateFlow<RewardsUi> = combine(
        repository.totalReps,
        repository.totalUnlocks,
        streak,
        settings.isProFlow,
    ) { reps, unlocks, streakDays, isPro ->
        val xp = reps * XP_PER_REP + unlocks * XP_PER_UNLOCK
        val level = xp / XP_PER_LEVEL + 1
        RewardsUi(
            level = level,
            levelTitle = levelTitle(level),
            xpIntoLevel = xp % XP_PER_LEVEL,
            xpPerLevel = XP_PER_LEVEL,
            totalXp = xp,
            isPro = isPro,
            milestones = listOf(
                Milestone("First rep", "Complete your first rep", fraction(reps, 1)),
                Milestone("Century", "Reach 100 total reps", fraction(reps, 100)),
                Milestone("First unlock", "Earn your first app unlock", fraction(unlocks, 1)),
                Milestone("3-day streak", "Work out 3 days in a row", fraction(streakDays, 3)),
                Milestone("Pro member", "Unlimited blocked apps and unlocks", if (isPro) 1f else 0f),
            ),
        )
    }.stateIn(viewModelScope, sharing, RewardsUi())

    private fun fraction(value: Int, target: Int): Float =
        (value.toFloat() / target).coerceIn(0f, 1f)

    companion object {
        const val XP_PER_REP = 10
        const val XP_PER_UNLOCK = 50
        const val XP_PER_LEVEL = 300

        fun levelTitle(level: Int): String = when (level) {
            1 -> "Rookie"
            2 -> "Focus Starter"
            3 -> "Focus Warrior"
            4 -> "Discipline Pro"
            else -> "Focus Legend"
        }
    }
}
