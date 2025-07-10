package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.*
import com.example.gymtracker.network.ApiService
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class AuthRepository(private val context: Context) {
    
    private val tokenManager = TokenManager(context)
    private val apiService = createApiService()
    
    private fun createApiService(): ApiService {
        // Use the new Vercel backend URL for all environments
        val baseUrl = "https://quantum-lift.vercel.app/"
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        Log.d("AUTH_REPO", "Attempting login for username: $username")
        return try {
            val response = apiService.login(LoginRequest(username, password))
            Log.d("AUTH_REPO", "Login response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Log.d("AUTH_REPO", "Login successful, saving token and user info")
                    // Save token and user info
                    authResponse.token?.let { token ->
                        tokenManager.saveToken(token)
                    }
                    authResponse.user?.let { user ->
                        tokenManager.saveUserInfo(user.id.toString(), user.username)
                    }
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Login failed with code: ${response.code()}")
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Login exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        Log.d("AUTH_REPO", "Attempting registration for username: $username, email: $email")
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            Log.d("AUTH_REPO", "Registration response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Log.d("AUTH_REPO", "Registration successful, saving token and user info")
                    // Save token and user info
                    authResponse.token?.let { token ->
                        tokenManager.saveToken(token)
                    }
                    authResponse.user?.let { user ->
                        tokenManager.saveUserInfo(user.id.toString(), user.username)
                    }
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Registration failed with code: ${response.code()}")
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Registration exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<AuthResponse> {
        Log.d("AUTH_REPO", "Attempting password change")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.changePassword(
                ChangePasswordRequest(currentPassword, newPassword),
                "Bearer $token"
            )
            Log.d("AUTH_REPO", "Change password response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Log.d("AUTH_REPO", "Password change successful")
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Password change failed with code: ${response.code()}")
                Result.failure(Exception("Password change failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Password change exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(): Result<User> {
        Log.d("AUTH_REPO", "Attempting to get user profile")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getProfile("Bearer $token")
            Log.d("AUTH_REPO", "Get profile response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { profileResponse ->
                    profileResponse.user?.let { user ->
                        Log.d("AUTH_REPO", "Profile retrieved successfully for user: ${user.username}")
                        Result.success(user)
                    } ?: Result.failure(Exception("No user data in response"))
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get profile failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get profile exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun logout() {
        tokenManager.clearAuthData()
    }
    
    fun getToken(): Flow<String?> = tokenManager.token
    
    fun getUserId(): Flow<String?> = tokenManager.userId
    
    fun getUsername(): Flow<String?> = tokenManager.username
    
    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getStoredToken() != null
    }
} 