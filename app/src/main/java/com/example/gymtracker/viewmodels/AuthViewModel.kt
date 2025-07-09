package com.example.gymtracker.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.services.AuthRepository
import com.example.gymtracker.data.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val user: com.example.gymtracker.data.User? = null
)

class AuthViewModel(private val context: Context) : ViewModel() {
    
    private val authRepository = AuthRepository(context)
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            _authState.value = _authState.value.copy(isLoggedIn = isLoggedIn)
        }
    }
    
    fun login(username: String, password: String) {
        Log.d("AUTH_LOG", "Login attempt for username: $username")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.login(username, password)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Login successful for user: ${response.user?.username}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = response.user,
                        success = response.message,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Login failed: ${exception.message}", exception)
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
        Log.d("AUTH_LOG", "Registration attempt for username: $username, email: $email")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.register(username, email, password)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Registration successful for user: ${response.user?.username}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = response.user,
                        success = response.message,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Registration failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registration failed",
                        success = null
                    )
                }
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState()
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _authState.value = _authState.value.copy(success = null)
    }
} 