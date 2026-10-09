package com.replock.domain

import kotlin.math.abs

/**
 * Pure pushup rep-counting state machine (no Android dependencies, unit-testable).
 *
 * Elbow angle = shoulder–elbow–wrist angle in degrees. Arms straight (pushup "up"
 * position) is ~180°, elbows bent at the bottom of a pushup is ~90° or less.
 *
 * A rep is counted when BOTH elbow angles go from > [upThreshold] (up) down to
 * < [downThreshold] (down) and back above [upThreshold] (up again).
 *
 * Anti-cheat rules:
 *  - at least [minRepIntervalMs] between counted reps
 *  - both arms must stay within [maxAsymmetry] degrees of each other, so waving
 *    one arm doesn't count; while asymmetric, the state machine is frozen
 */
class RepCounter(
    private val upThreshold: Float = 160f,
    private val downThreshold: Float = 90f,
    private val maxAsymmetry: Float = 30f,
    private val minRepIntervalMs: Long = 800L,
) {
    enum class Phase { UP, DOWN }

    var phase: Phase = Phase.UP
        private set

    var reps: Int = 0
        private set

    /** True when the latest frame had both arms moving symmetrically. */
    var formOk: Boolean = true
        private set

    private var lastRepMs: Long = 0L
    private var hasCountedRep = false

    /**
     * Feed one camera frame.
     *
     * @param leftElbowAngle  left shoulder–elbow–wrist angle in degrees
     * @param rightElbowAngle right shoulder–elbow–wrist angle in degrees
     * @param nowMs monotonic timestamp in ms (e.g. SystemClock.elapsedRealtime())
     * @return true if this frame completed and counted a new rep
     */
    fun onFrame(leftElbowAngle: Float, rightElbowAngle: Float, nowMs: Long): Boolean {
        val symmetric = abs(leftElbowAngle - rightElbowAngle) <= maxAsymmetry
        formOk = symmetric
        if (!symmetric) return false // freeze the state machine while arms disagree

        return when (phase) {
            Phase.UP -> {
                if (leftElbowAngle < downThreshold && rightElbowAngle < downThreshold) {
                    phase = Phase.DOWN
                }
                false
            }
            Phase.DOWN -> {
                if (leftElbowAngle > upThreshold && rightElbowAngle > upThreshold) {
                    phase = Phase.UP
                    val intervalOk = !hasCountedRep || (nowMs - lastRepMs) >= minRepIntervalMs
                    if (intervalOk) {
                        hasCountedRep = true
                        lastRepMs = nowMs
                        reps++
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    fun reset() {
        phase = Phase.UP
        reps = 0
        formOk = true
        lastRepMs = 0L
        hasCountedRep = false
    }
}
