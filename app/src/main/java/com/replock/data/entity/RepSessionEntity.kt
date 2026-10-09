package com.replock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rep_sessions")
data class RepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val reps: Int,
    val exercise: String,
)
