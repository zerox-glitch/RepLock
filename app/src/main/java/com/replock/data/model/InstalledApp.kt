package com.replock.data.model

/** Coarse app category, derived from the app's Play/Android category. */
enum class AppCategory(val label: String) {
    Social("Social"),
    Entertainment("Entertainment"),
    Other("Other"),
}

/** A launchable app on the device, shown in the app picker. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: AppCategory = AppCategory.Other,
)
