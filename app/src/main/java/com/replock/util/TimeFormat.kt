package com.replock.util

/**
 * Formats a duration in minutes for display: "5 min", "45 min", "1 hr",
 * "1 hr 30 min", "3 hr".
 */
fun formatUnlockWindow(minutes: Int): String {
    if (minutes <= 0) return "0 min"
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0) "$hours hr" else "$hours hr $mins min"
}
