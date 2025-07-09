package com.example.gymtracker.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.gymtracker.data.LoginRequest
import com.example.gymtracker.data.RegisterRequest
import com.example.gymtracker.data.AuthResponse
import kotlin.jvm.JvmSuppressWildcards

/**
 * API service interface for authentication endpoints
 * This interface is used by Retrofit to generate HTTP client code
 */
@JvmSuppressWildcards
interface ApiService {
    /**
     * Login endpoint
     * @param loginRequest The login credentials
     * @return Response containing authentication data
     */
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>
    
    /**
     * Register endpoint
     * @param registerRequest The registration data
     * @return Response containing authentication data
     */
    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>
} 