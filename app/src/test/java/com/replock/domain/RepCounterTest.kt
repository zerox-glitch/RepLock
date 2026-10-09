package com.replock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the rep-counting state machine. Pure JVM — no device needed.
 *
 * Pushups: elbow angle (shoulder–elbow–wrist) — arms straight ≈ 170°, bent ≈ 80°.
 * Squats: knee angle (hip–knee–ankle) — standing ≈ 170°, deep squat ≈ 90°.
 */
class RepCounterTest {

    // ---- Pushups (default thresholds: up > 160, down < 90) ----

    @Test
    fun `counts a rep when arms go down then back up`() {
        val counter = RepCounter()
        assertFalse(counter.onFrame(170f, 170f, nowMs = 0L))   // arms straight (up)
        assertFalse(counter.onFrame(80f, 80f, nowMs = 100L))   // elbows bent (down)
        assertTrue(counter.onFrame(170f, 170f, nowMs = 200L))  // back up -> rep!
        assertEquals(1, counter.reps)
        assertEquals(RepCounter.Phase.UP, counter.phase)
    }

    @Test
    fun `enforces minimum 800ms interval between reps`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        counter.onFrame(80f, 80f, 100L)
        assertTrue(counter.onFrame(170f, 170f, 200L)) // rep #1 at t=200

        counter.onFrame(80f, 80f, 300L)
        assertFalse(counter.onFrame(170f, 170f, 500L)) // only 300ms later -> no rep
        assertEquals(1, counter.reps)

        counter.onFrame(80f, 80f, 600L)
        assertTrue(counter.onFrame(170f, 170f, 1100L)) // 900ms after rep #1 -> rep #2
        assertEquals(2, counter.reps)
    }

    @Test
    fun `asymmetric arms are rejected and freeze the state machine`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        assertFalse(counter.onFrame(80f, 170f, 100L)) // one arm down, one up
        assertFalse(counter.formOk)
        assertFalse(counter.onFrame(170f, 80f, 200L))
        assertEquals(0, counter.reps)
    }

    @Test
    fun `partial depth does not count`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        assertFalse(counter.onFrame(120f, 120f, 100L)) // not deep enough (< 90 required)
        assertFalse(counter.onFrame(170f, 170f, 200L))
        assertEquals(0, counter.reps)
    }

    @Test
    fun `one arm not deep enough blocks the rep`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        // Symmetric enough (diff 25 ≤ 30) but right arm never goes below 90°.
        assertFalse(counter.onFrame(80f, 105f, 100L))
        assertFalse(counter.onFrame(170f, 170f, 200L))
        assertEquals(0, counter.reps)
    }

    @Test
    fun `arms must be fully extended to reset the rep`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        counter.onFrame(80f, 80f, 100L)   // down
        assertFalse(counter.onFrame(140f, 140f, 200L)) // not > 160 -> still DOWN
        assertEquals(RepCounter.Phase.DOWN, counter.phase)
        assertTrue(counter.onFrame(170f, 170f, 300L)) // fully up again -> first rep counted
        assertEquals(1, counter.reps)
    }

    @Test
    fun `counts multiple full reps`() {
        val counter = RepCounter()
        var t = 0L
        repeat(5) {
            counter.onFrame(170f, 170f, t); t += 100
            counter.onFrame(80f, 80f, t); t += 100
            counter.onFrame(170f, 170f, t); t += 900 // > 800ms gap between reps
        }
        assertEquals(5, counter.reps)
    }

    @Test
    fun `reset clears state`() {
        val counter = RepCounter()
        counter.onFrame(170f, 170f, 0L)
        counter.onFrame(80f, 80f, 100L)
        counter.onFrame(170f, 170f, 200L)
        assertEquals(1, counter.reps)
        counter.reset()
        assertEquals(0, counter.reps)
        assertEquals(RepCounter.Phase.UP, counter.phase)
        assertTrue(counter.formOk)
    }

    // ---- Squats (thresholds: standing > 150, deep < 100) ----

    @Test
    fun `counts a squat rep with squat thresholds`() {
        val counter = RepCounter(upThreshold = 150f, downThreshold = 100f)
        assertFalse(counter.onFrame(170f, 170f, 0L))   // standing
        assertFalse(counter.onFrame(90f, 90f, 100L))   // deep squat
        assertTrue(counter.onFrame(170f, 170f, 200L))  // stand up -> rep!
        assertEquals(1, counter.reps)
    }

    @Test
    fun `shallow squat does not count`() {
        val counter = RepCounter(upThreshold = 150f, downThreshold = 100f)
        counter.onFrame(170f, 170f, 0L)
        assertFalse(counter.onFrame(120f, 120f, 100L)) // not deep enough (< 100 required)
        assertFalse(counter.onFrame(170f, 170f, 200L))
        assertEquals(0, counter.reps)
    }

    @Test
    fun `one leg not deep enough blocks the squat rep`() {
        val counter = RepCounter(upThreshold = 150f, downThreshold = 100f)
        counter.onFrame(170f, 170f, 0L)
        // Symmetric enough (diff 25 ≤ 30) but right knee never goes below 100°.
        assertFalse(counter.onFrame(90f, 115f, 100L))
        assertFalse(counter.onFrame(170f, 170f, 200L))
        assertEquals(0, counter.reps)
    }
}
