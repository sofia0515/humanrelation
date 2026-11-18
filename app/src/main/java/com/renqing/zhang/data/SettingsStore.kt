package com.renqing.zhang.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("renqing_settings")

data class Settings(
    val remindOn: Boolean = true,
    val thresholdYuan: Int = 500,
    val familyMute: Boolean = true,
    val lockOn: Boolean = false,
    val onboardingDone: Boolean = false,
    val userName: String = "",
    val userPhone: String = "",
) {
    val thresholdFen: Long get() = thresholdYuan.coerceAtLeast(0).toLong() * 100L
}

class SettingsStore(private val context: Context) {
    private val remindOn = booleanPreferencesKey("remindOn")
    private val threshold = intPreferencesKey("threshold")
    private val familyMute = booleanPreferencesKey("familyMute")
    private val lockOn = booleanPreferencesKey("lockOn")
    private val onboardingDone = booleanPreferencesKey("onboardingDone")
    private val userName = stringPreferencesKey("userName")
    private val userPhone = stringPreferencesKey("userPhone")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            remindOn = prefs[remindOn] ?: true,
            thresholdYuan = prefs[threshold] ?: 500,
            familyMute = prefs[familyMute] ?: true,
            lockOn = prefs[lockOn] ?: false,
            onboardingDone = prefs[onboardingDone] ?: false,
            userName = prefs[userName] ?: "",
            userPhone = prefs[userPhone] ?: "",
        )
    }

    suspend fun setRemind(on: Boolean) = edit { it[remindOn] = on }
    suspend fun setThreshold(yuan: Int) = edit { it[threshold] = yuan.coerceAtLeast(0) }
    suspend fun setFamilyMute(on: Boolean) = edit { it[familyMute] = on }
    suspend fun setLock(on: Boolean) = edit { it[lockOn] = on }
    suspend fun setOnboardingDone() = edit { it[onboardingDone] = true }
    suspend fun setProfile(name: String, phone: String) = edit {
        it[userName] = name.trim()
        it[userPhone] = phone.trim()
    }

    suspend fun applyBackup(remind: Boolean, thresholdYuan: Int, familyMuteOn: Boolean, name: String, phone: String) {
        edit {
            it[remindOn] = remind
            it[threshold] = thresholdYuan
            it[familyMute] = familyMuteOn
            it[userName] = name
            it[userPhone] = phone
        }
    }

    private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
