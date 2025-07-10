package com.example.gymtracker.data

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class AuthResponse(
    val message: String,
    val user: User? = null,
    val token: String? = null,
    val error: String? = null
)

data class ProfileResponse(
    val user: User? = null,
    val error: String? = null
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val createdAt: String
) 