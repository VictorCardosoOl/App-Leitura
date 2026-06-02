package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val IS_RTL = booleanPreferencesKey("is_rtl_manga_mode")
    private val IS_SCROLL_MODE = booleanPreferencesKey("is_scroll_mode")

    val isRtlFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_RTL] ?: false
        }
        
    val isScrollModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SCROLL_MODE] ?: false
        }

    suspend fun setRtlMode(isRtl: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_RTL] = isRtl
        }
    }
    
    suspend fun setScrollMode(isScroll: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SCROLL_MODE] = isScroll
        }
    }
}
