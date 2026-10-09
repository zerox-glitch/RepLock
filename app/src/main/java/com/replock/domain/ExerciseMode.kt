package com.replock.domain

/** What the overlay counts to unlock an app: pushups, squats, or reps of either. */
enum class ExerciseMode(val label: String) {
    Pushups("Pushups"),
    Squats("Squats"),
    Both("Pushups + Squats"),
}
