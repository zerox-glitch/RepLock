package com.replock.data.model

/** A launchable app on the device, shown in the app picker. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
