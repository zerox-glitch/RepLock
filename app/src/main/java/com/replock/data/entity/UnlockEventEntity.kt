package com.replock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One granted unlock: [packageName] stays unlocked until [unlockUntil].
 * Persisted in Room, so unlock windows survive reboots.
 */
@Entity(tableName = "unlock_events")
data class UnlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val unlockUntil: Long,
)
