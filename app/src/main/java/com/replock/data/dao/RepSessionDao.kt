package com.replock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replock.data.entity.RepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepSessionDao {
    @Insert
    suspend fun insert(session: RepSessionEntity)

    @Query("SELECT COALESCE(SUM(reps), 0) FROM rep_sessions WHERE timestamp >= :since")
    fun totalRepsSince(since: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(reps), 0) FROM rep_sessions")
    fun totalReps(): Flow<Int>

    @Query("SELECT timestamp FROM rep_sessions ORDER BY timestamp DESC")
    suspend fun allTimestamps(): List<Long>

    @Query("SELECT * FROM rep_sessions WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun sessionsSince(since: Long): Flow<List<RepSessionEntity>>
}
