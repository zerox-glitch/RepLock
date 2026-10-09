package com.replock.overlay

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.data.SettingsDataStore
import com.replock.domain.ExerciseMode
import com.replock.domain.RepCounter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class OverlayState { Counting, Unlocked, Paywall }

/** UI state for the skeleton overlay drawn on top of the camera preview. */
data class PoseUiState(
    val skeleton: List<Pair<PoseAnalyzer.Point, PoseAnalyzer.Point>> = emptyList(),
    val inFrame: Boolean = false,
    val formOk: Boolean = true,
)

/**
 * Drives the blocking overlay: counts reps from pose frames (pushups via elbow
 * angle, squats via knee angle, or reps of either in "both" mode), and when
 * the target is hit either grants the unlock window or shows the paywall
 * (free tier: 3 unlocks per day).
 */
class OverlayViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = (application as RepLockApp).repository
    private val settings: SettingsDataStore = (application as RepLockApp).settings

    val packageName: String = savedState[BlockOverlayActivity.EXTRA_PACKAGE_NAME] ?: ""
    val appName: String = savedState[BlockOverlayActivity.EXTRA_APP_NAME] ?: packageName

    private val _target = MutableStateFlow(SettingsDataStore.DEFAULT_REP_TARGET)
    val target: StateFlow<Int> = _target.asStateFlow()

    private val _exerciseMode = MutableStateFlow(ExerciseMode.Pushups)
    val exerciseMode: StateFlow<ExerciseMode> = _exerciseMode.asStateFlow()

    private val _reps = MutableStateFlow(0)
    val reps: StateFlow<Int> = _reps.asStateFlow()

    private val _pose = MutableStateFlow(PoseUiState())
    val pose: StateFlow<PoseUiState> = _pose.asStateFlow()

    private val _state = MutableStateFlow(OverlayState.Counting)
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    /** Emitted once per counted rep — the activity turns this into a haptic tick. */
    private val _repHaptics = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val repHaptics: SharedFlow<Unit> = _repHaptics.asSharedFlow()

    // Pushups: elbow angle (shoulder–elbow–wrist). Straight > 160°, bent < 90°.
    private val pushupCounter = RepCounter(upThreshold = 160f, downThreshold = 90f)
    // Squats: knee angle (hip–knee–ankle). Standing > 150°, deep squat < 100°.
    private val squatCounter = RepCounter(upThreshold = 150f, downThreshold = 100f)

    private var targetReachedHandled = false

    init {
        viewModelScope.launch {
            _target.value = settings.repTargetFlow.first()
            _exerciseMode.value = when (settings.difficultyFlow.first()) {
                SettingsDataStore.DIFFICULTY_SQUATS -> ExerciseMode.Squats
                SettingsDataStore.DIFFICULTY_BOTH -> ExerciseMode.Both
                else -> ExerciseMode.Pushups
            }
        }
    }

    fun onPoseResult(result: PoseAnalyzer.PoseFrameResult) {
        val mode = _exerciseMode.value
        val inFrame = when (mode) {
            ExerciseMode.Pushups -> result.upperBodyOk
            ExerciseMode.Squats -> result.lowerBodyOk
            ExerciseMode.Both -> result.upperBodyOk && result.lowerBodyOk
        }

        if (!inFrame) {
            _pose.value = PoseUiState(
                skeleton = result.skeleton,
                inFrame = false,
                formOk = false,
            )
            return
        }

        val now = SystemClock.elapsedRealtime()
        val counted = when (mode) {
            ExerciseMode.Pushups ->
                pushupCounter.onFrame(result.leftElbowAngle, result.rightElbowAngle, now)
            ExerciseMode.Squats ->
                squatCounter.onFrame(result.leftKneeAngle, result.rightKneeAngle, now)
            ExerciseMode.Both -> {
                val p = pushupCounter.onFrame(result.leftElbowAngle, result.rightElbowAngle, now)
                val s = squatCounter.onFrame(result.leftKneeAngle, result.rightKneeAngle, now)
                p || s
            }
        }

        _reps.value = pushupCounter.reps + squatCounter.reps
        _pose.value = PoseUiState(
            skeleton = result.skeleton,
            inFrame = true,
            formOk = when (mode) {
                ExerciseMode.Pushups -> pushupCounter.formOk
                ExerciseMode.Squats -> squatCounter.formOk
                ExerciseMode.Both -> pushupCounter.formOk && squatCounter.formOk
            },
        )

        if (counted) {
            _repHaptics.tryEmit(Unit)
            if (_reps.value >= _target.value) onTargetReached()
        }
    }

    private fun onTargetReached() {
        if (targetReachedHandled) return
        targetReachedHandled = true
        viewModelScope.launch {
            // The user did the work — always record the session.
            val exerciseLabel = when (_exerciseMode.value) {
                ExerciseMode.Pushups -> "pushups"
                ExerciseMode.Squats -> "squats"
                ExerciseMode.Both -> "mixed"
            }
            repository.recordRepSession(reps = _reps.value, exercise = exerciseLabel)

            val isPro = settings.isProFlow.first()
            val unlocksToday = repository.unlocksTodayCount()
            if (!isPro && unlocksToday >= FREE_UNLOCKS_PER_DAY) {
                _state.value = OverlayState.Paywall
            } else {
                grantUnlock()
            }
        }
    }

    /** STUB: hardcoded paywall. A real RevenueCat/Play Billing purchase would happen here. */
    fun purchasePro() {
        viewModelScope.launch {
            settings.setPro(true)
            grantUnlock()
        }
    }

    private suspend fun grantUnlock() {
        val windowMinutes = settings.unlockWindowMinutesFlow.first()
        repository.recordUnlock(packageName, windowMinutes)
        _state.value = OverlayState.Unlocked
    }

    companion object {
        const val FREE_UNLOCKS_PER_DAY = 3
    }
}
