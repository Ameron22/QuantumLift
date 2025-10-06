package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.auth0.android.jwt.JWT
import java.util.Date

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }
    
    private val credentialManager: CredentialManager by lazy {
        try {
            SecureCredentialManager(context)
        } catch (e: Exception) {
            Log.w("TOKEN_MANAGER", "SecureCredentialManager failed, using SimpleCredentialManager", e)
            SimpleCredentialManager(context)
        }
    }
    
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }
    
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    suspend fun saveUserInfo(userId: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
        }
    }
    
    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
        // Also clear secure credentials if needed
        // Note: We don't clear credentials by default to allow auto-login
    }
    
    suspend fun clearAllAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
        // Clear credentials as well
        credentialManager.clearCredentials()
    }
    
    suspend fun getStoredToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first()
    }
    
    suspend fun isTokenValid(): Boolean {
        val token = getStoredToken()
        if (token == null) {
            Log.d("TOKEN_MANAGER", "No token found")
            return false
        }
        
        return try {
            val jwt = JWT(token)
            val expiration = jwt.expiresAt
            val now = Date()
            
            if (expiration == null) {
                Log.d("TOKEN_MANAGER", "Token has no expiration date")
                return false
            }
            
            val isValid = expiration.after(now)
            Log.d("TOKEN_MANAGER", "Token valid: $isValid, expires: $expiration, now: $now")
            
            if (!isValid) {
                Log.d("TOKEN_MANAGER", "Token expired, clearing auth data")
                clearAuthData()
            }
            
            isValid
        } catch (e: Exception) {
            Log.e("TOKEN_MANAGER", "Error validating token: ${e.message}", e)
            // If token is malformed, clear it
            clearAuthData()
            false
        }
    }
    
    suspend fun getTokenExpirationTime(): Date? {
        val token = getStoredToken() ?: return null
        
        return try {
            val jwt = JWT(token)
            jwt.expiresAt
        } catch (e: Exception) {
            Log.e("TOKEN_MANAGER", "Error getting token expiration: ${e.message}", e)
            null
        }
    }
    
    // Credential management methods
    
    suspend fun getStoredCredentials(): Pair<String?, String?> {
        return credentialManager.getStoredCredentials()
    }
    
    suspend fun hasStoredCredentials(): Boolean {
        return credentialManager.hasStoredCredentials()
    }
    
    suspend fun isAutoLoginEnabled(): Boolean {
        return credentialManager.isAutoLoginEnabled()
    }
    
    suspend fun setAutoLoginEnabled(enabled: Boolean) {
        credentialManager.setAutoLoginEnabled(enabled)
    }
    
    suspend fun saveCredentials(username: String, password: String) {
        credentialManager.saveCredentials(username, password)
    }
    
    suspend fun updatePassword(newPassword: String) {
        credentialManager.updatePassword(newPassword)
    }
    
    fun getAutoLoginStatusFlow(): Flow<Boolean> {
        return credentialManager.getAutoLoginStatusFlow()
    }
    
    fun getCredentialsAvailableFlow(): Flow<Boolean> {
        return credentialManager.getCredentialsAvailableFlow()
    }
} 