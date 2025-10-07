package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

class SyncPreferencesManager(private val context: Context) {
    
    companion object {
        private val LAST_BODY_SYNC_KEY = longPreferencesKey("last_body_sync_timestamp")
    }
    
    /**
     * Get the last sync timestamp for body data.
     * Returns null if never synced before (new phone scenario).
     */
    suspend fun getLastBodySyncTimestamp(): Long? {
        val timestamp = context.syncDataStore.data.map { preferences ->
            preferences[LAST_BODY_SYNC_KEY]
        }.first()
        
        Log.d("SyncPreferencesManager", "Last body sync timestamp: ${timestamp?.let { "timestamp=$it (${java.util.Date(it)})" } ?: "null (never synced)"}")
        return timestamp
    }
    
    /**
     * Save the last sync timestamp for body data.
     * Call this after successful sync from cloud.
     */
    suspend fun saveLastBodySyncTimestamp(timestamp: Long) {
        context.syncDataStore.edit { preferences ->
            preferences[LAST_BODY_SYNC_KEY] = timestamp
        }
        Log.d("SyncPreferencesManager", "Saved last body sync timestamp: $timestamp (${java.util.Date(timestamp)})")
    }
    
    /**
     * Clear the last sync timestamp (e.g., on logout or data reset).
     */
    suspend fun clearLastBodySyncTimestamp() {
        context.syncDataStore.edit { preferences ->
            preferences.remove(LAST_BODY_SYNC_KEY)
        }
        Log.d("SyncPreferencesManager", "Cleared last body sync timestamp")
    }
    
    /**
     * Flow to observe the last sync timestamp.
     */
    val lastBodySyncTimestamp: Flow<Long?> = context.syncDataStore.data.map { preferences ->
        preferences[LAST_BODY_SYNC_KEY]
    }
}

