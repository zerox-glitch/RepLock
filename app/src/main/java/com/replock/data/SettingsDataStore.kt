package com.replock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "replock_settings")

/**
 * User settings, persisted in DataStore: onboarding state, rep target, unlock
 * window, difficulty, the (stub) Pro flag, and the global blocking toggle.
 */
class SettingsDataStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REP_TARGET = intPreferencesKey("rep_target")
        val UNLOCK_WINDOW_MINUTES = intPreferencesKey("unlock_window_minutes")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")

        const val DIFFICULTY_PUSHUPS = "pushups"
        const val DIFFICULTY_SQUATS = "squats"
        const val DIFFICULTY_BOTH = "both"

        const val DEFAULT_REP_TARGET = 10
        const val DEFAULT_UNLOCK_WINDOW_MINUTES = 5
    }

    val onboardingDoneFlow: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val repTargetFlow: Flow<Int> = dataStore.data.map { it[REP_TARGET] ?: DEFAULT_REP_TARGET }
    val unlockWindowMinutesFlow: Flow<Int> =
        dataStore.data.map { it[UNLOCK_WINDOW_MINUTES] ?: DEFAULT_UNLOCK_WINDOW_MINUTES }
    val difficultyFlow: Flow<String> =
        dataStore.data.map { it[DIFFICULTY] ?: DIFFICULTY_PUSHUPS }
    val isProFlow: Flow<Boolean> = dataStore.data.map { it[IS_PRO] ?: false }
    val blockingEnabledFlow: Flow<Boolean> = dataStore.data.map { it[BLOCKING_ENABLED] ?: true }

    suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[ONBOARDING_DONE] = done }
    suspend fun setRepTarget(target: Int) = dataStore.edit { it[REP_TARGET] = target }
    suspend fun setUnlockWindowMinutes(minutes: Int) =
        dataStore.edit { it[UNLOCK_WINDOW_MINUTES] = minutes }
    suspend fun setDifficulty(difficulty: String) = dataStore.edit { it[DIFFICULTY] = difficulty }
    suspend fun setPro(pro: Boolean) = dataStore.edit { it[IS_PRO] = pro }
    suspend fun setBlockingEnabled(enabled: Boolean) = dataStore.edit { it[BLOCKING_ENABLED] = enabled }

    suspend fun isProNow(): Boolean = isProFlow.first()
    suspend fun isBlockingEnabledNow(): Boolean = blockingEnabledFlow.first()
}
