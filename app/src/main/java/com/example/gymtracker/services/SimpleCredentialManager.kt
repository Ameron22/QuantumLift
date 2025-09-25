package com.example.gymtracker.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * SimpleCredentialManager provides a fallback credential storage solution
 * using regular SharedPreferences when EncryptedSharedPreferences is not available.
 * 
 * Note: This is less secure than EncryptedSharedPreferences but ensures functionality.
 */
class SimpleCredentialManager(private val context: Context) : CredentialManager {
    
    companion object {
        private const val PREFS_NAME = "simple_credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled"
        private const val TAG = "SimpleCredentialManager"
    }
    
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save user credentials
     */
    override suspend fun saveCredentials(username: String, password: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving credentials for user: $username")
                
                preferences.edit()
                    .putString(KEY_USERNAME, username)
                    .putString(KEY_PASSWORD, password)
                    .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
                    .apply()
                
                Log.d(TAG, "Credentials saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save credentials", e)
                throw e
            }
        }
    }
    
    /**
     * Retrieve stored username
     */
    override suspend fun getStoredUsername(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val username = preferences.getString(KEY_USERNAME, null)
                Log.d(TAG, "Retrieved username: ${if (username != null) "***" else "null"}")
                username
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve username", e)
                null
            }
        }
    }
    
    /**
     * Retrieve stored password
     */
    override suspend fun getStoredPassword(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val password = preferences.getString(KEY_PASSWORD, null)
                Log.d(TAG, "Retrieved password: ${if (password != null) "***" else "null"}")
                password
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve password", e)
                null
            }
        }
    }
    
    /**
     * Get stored credentials as a pair
     */
    override suspend fun getStoredCredentials(): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val username = preferences.getString(KEY_USERNAME, null)
                val password = preferences.getString(KEY_PASSWORD, null)
                Log.d(TAG, "Retrieved credentials: username=${if (username != null) "***" else "null"}, password=${if (password != null) "***" else "null"}")
                Pair(username, password)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve credentials", e)
                Pair(null, null)
            }
        }
    }
    
    /**
     * Check if auto-login is enabled
     */
    override suspend fun isAutoLoginEnabled(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val enabled = preferences.getBoolean(KEY_AUTO_LOGIN_ENABLED, false)
                Log.d(TAG, "Auto-login enabled: $enabled")
                enabled
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check auto-login status", e)
                false
            }
        }
    }
    
    /**
     * Enable or disable auto-login
     */
    override suspend fun setAutoLoginEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                preferences.edit()
                    .putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled)
                    .apply()
                Log.d(TAG, "Auto-login set to: $enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set auto-login status", e)
            }
        }
    }
    
    /**
     * Check if credentials are stored
     */
    override suspend fun hasStoredCredentials(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val username = preferences.getString(KEY_USERNAME, null)
                val password = preferences.getString(KEY_PASSWORD, null)
                val hasCredentials = !username.isNullOrBlank() && !password.isNullOrBlank()
                Log.d(TAG, "Has stored credentials: $hasCredentials")
                hasCredentials
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check stored credentials", e)
                false
            }
        }
    }
    
    /**
     * Clear all stored credentials
     */
    override suspend fun clearCredentials() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing stored credentials")
                preferences.edit()
                    .remove(KEY_USERNAME)
                    .remove(KEY_PASSWORD)
                    .remove(KEY_AUTO_LOGIN_ENABLED)
                    .apply()
                Log.d(TAG, "Credentials cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credentials", e)
            }
        }
    }
    
    /**
     * Update only the password (when user changes password)
     */
    override suspend fun updatePassword(newPassword: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating stored password")
                preferences.edit()
                    .putString(KEY_PASSWORD, newPassword)
                    .apply()
                Log.d(TAG, "Password updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update password", e)
            }
        }
    }
    
    /**
     * Flow to observe auto-login status changes
     */
    override fun getAutoLoginStatusFlow(): Flow<Boolean> = flow {
        try {
            emit(isAutoLoginEnabled())
        } catch (e: Exception) {
            Log.e(TAG, "Error in auto-login status flow", e)
            emit(false)
        }
    }
    
    /**
     * Flow to observe credential availability
     */
    override fun getCredentialsAvailableFlow(): Flow<Boolean> = flow {
        try {
            emit(hasStoredCredentials())
        } catch (e: Exception) {
            Log.e(TAG, "Error in credentials availability flow", e)
            emit(false)
        }
    }
}
