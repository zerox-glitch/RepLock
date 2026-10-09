package com.replock.overlay

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.replock.RepLockApp
import com.replock.data.SettingsDataStore
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
 * Drives the blocking overlay: counts reps from pose frames, and when the
 * target is hit either grants the unlock window or shows the paywall
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

    private val _reps = MutableStateFlow(0)
    val reps: StateFlow<Int> = _reps.asStateFlow()

    private val _phase = MutableStateFlow(RepCounter.Phase.UP)
    val phase: StateFlow<RepCounter.Phase> = _phase.asStateFlow()

    private val _pose = MutableStateFlow(PoseUiState())
    val pose: StateFlow<PoseUiState> = _pose.asStateFlow()

    private val _state = MutableStateFlow(OverlayState.Counting)
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    /** Emitted once per counted rep — the activity turns this into a haptic tick. */
    private val _repHaptics = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val repHaptics: SharedFlow<Unit> = _repHaptics.asSharedFlow()

    private val counter = RepCounter()
    private var targetReachedHandled = false

    init {
        viewModelScope.launch {
            _target.value = settings.repTargetFlow.first()
        }
    }

    fun onPoseResult(result: PoseAnalyzer.PoseFrameResult) {
        _pose.value = PoseUiState(
            skeleton = result.skeleton,
            inFrame = result.inFrame,
            formOk = result.formOk,
        )
        if (!result.inFrame) return

        val counted = counter.onFrame(
            result.leftElbowAngle,
            result.rightElbowAngle,
            SystemClock.elapsedRealtime(),
        )
        _phase.value = counter.phase
        _reps.value = counter.reps

        if (counted) {
            _repHaptics.tryEmit(Unit)
            if (counter.reps >= _target.value) onTargetReached()
        }
    }

    private fun onTargetReached() {
        if (targetReachedHandled) return
        targetReachedHandled = true
        viewModelScope.launch {
            // The user did the work — always record the session.
            repository.recordRepSession(reps = counter.reps, exercise = "pushups")

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
