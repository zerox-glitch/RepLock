package com.replock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replock.data.entity.UnlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockEventDao {
    @Insert
    suspend fun insert(event: UnlockEventEntity)

    @Query("SELECT * FROM unlock_events WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(packageName: String): UnlockEventEntity?

    @Query("SELECT COUNT(*) FROM unlock_events WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM unlock_events WHERE timestamp >= :since")
    fun countSinceFlow(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM unlock_events")
    fun totalCount(): Flow<Int>

    @Query("SELECT * FROM unlock_events WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun eventsSince(since: Long): Flow<List<UnlockEventEntity>>
}
