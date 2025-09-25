package com.example.gymtracker.services

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for credential management
 */
interface CredentialManager {
    suspend fun saveCredentials(username: String, password: String)
    suspend fun getStoredUsername(): String?
    suspend fun getStoredPassword(): String?
    suspend fun getStoredCredentials(): Pair<String?, String?>
    suspend fun isAutoLoginEnabled(): Boolean
    suspend fun setAutoLoginEnabled(enabled: Boolean)
    suspend fun hasStoredCredentials(): Boolean
    suspend fun clearCredentials()
    suspend fun updatePassword(newPassword: String)
    fun getAutoLoginStatusFlow(): Flow<Boolean>
    fun getCredentialsAvailableFlow(): Flow<Boolean>
}


