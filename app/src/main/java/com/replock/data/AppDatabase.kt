package com.replock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.replock.data.dao.BlockedAppDao
import com.replock.data.dao.RepSessionDao
import com.replock.data.dao.UnlockEventDao
import com.replock.data.entity.BlockedAppEntity
import com.replock.data.entity.RepSessionEntity
import com.replock.data.entity.UnlockEventEntity

@Database(
    entities = [BlockedAppEntity::class, RepSessionEntity::class, UnlockEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun repSessionDao(): RepSessionDao
    abstract fun unlockEventDao(): UnlockEventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "replock.db",
                ).build().also { instance = it }
            }
    }
}
