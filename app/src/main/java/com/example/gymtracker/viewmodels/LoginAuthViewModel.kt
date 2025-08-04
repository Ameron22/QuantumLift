package com.example.gymtracker.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.services.AuthRepository
import com.example.gymtracker.data.AuthResponse
import com.example.gymtracker.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginAuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val user: User? = null
)

class LoginAuthViewModel(private val context: Context) : ViewModel() {
    
    private val authRepository = AuthRepository(context)
    
    private val _authState = MutableStateFlow(LoginAuthState())
    val authState: StateFlow<LoginAuthState> = _authState.asStateFlow()
    
    // Don't check login status on init - LoginActivity should only handle login/register
    
    fun login(username: String, password: String) {
        Log.d("LOGIN_AUTH_LOG", "Login attempt for username: $username")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.login(username, password)
            result.fold(
                onSuccess = { response ->
                    Log.d("LOGIN_AUTH_LOG", "Login successful for user: ${response.user?.username}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = response.user,
                        success = response.message,
                        error = null
                    )
                    clearSuccessAfterDelay()
                },
                onFailure = { exception ->
                    Log.e("LOGIN_AUTH_LOG", "Login failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Login failed",
                        success = null
                    )
                }
            )
        }
    }
    
    fun register(username: String, email: String, password: String) {
        Log.d("LOGIN_AUTH_LOG", "Registration attempt for username: $username, email: $email")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.register(username, email, password)
            result.fold(
                onSuccess = { response ->
                    Log.d("LOGIN_AUTH_LOG", "Registration successful for user: ${response.user?.username}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = response.user,
                        success = response.message,
                        error = null
                    )
                    clearSuccessAfterDelay()
                },
                onFailure = { exception ->
                    Log.e("LOGIN_AUTH_LOG", "Registration failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registration failed",
                        success = null
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _authState.value = _authState.value.copy(success = null)
    }
    
    private fun clearSuccessAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Clear after 0.5 seconds
            clearSuccess()
        }
    }
} 