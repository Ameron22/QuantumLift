package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import com.example.gymtracker.data.LoginRequest
import com.example.gymtracker.data.RegisterRequest
import com.example.gymtracker.data.AuthResponse
import com.example.gymtracker.network.ApiService
import com.example.gymtracker.services.TokenManager

class AuthRepository(private val context: Context) {
    
    private val tokenManager = TokenManager(context)
    private val apiService = createApiService()
    
    private fun createApiService(): ApiService {
        // Use production URL for release builds, local IP for debug builds
        val baseUrl = if (BuildConfig.DEBUG) {
            "http://192.168.0.76:3000/" // Development
        } else {
            "https://gymtracker-production.up.railway.app/" // Production (will be updated after deployment)
        }
        
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        Log.d("AUTH_REPO", "Attempting login for username: $username")
        Log.d("AUTH_REPO", "Server URL: http://192.168.0.76:3000/")
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
            Log.e("AUTH_REPO", "Exception type: ${e.javaClass.simpleName}")
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