package com.example.gymtracker.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.services.AuthRepository
import com.example.gymtracker.data.AuthResponse
import com.example.gymtracker.data.User
import com.example.gymtracker.data.Friend
import com.example.gymtracker.data.FriendInvitationResponse
import com.example.gymtracker.data.FriendInvitation
import com.example.gymtracker.data.InvitationActionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val user: com.example.gymtracker.data.User? = null,
    val friends: List<Friend> = emptyList(),
    val pendingInvitations: List<FriendInvitation> = emptyList()
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
            if (isLoggedIn) {
                // Load user profile if logged in
                loadUserProfile()
            }
            _authState.value = _authState.value.copy(isLoggedIn = isLoggedIn)
        }
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getProfile()
            result.fold(
                onSuccess = { user ->
                    _authState.value = _authState.value.copy(user = user)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Failed to load user profile: ${exception.message}")
                }
            )
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
    
    fun changePassword(currentPassword: String, newPassword: String) {
        Log.d("AUTH_LOG", "Password change attempt")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.changePassword(currentPassword, newPassword)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Password change successful")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Password change failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Password change failed",
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
    
    fun sendFriendInvitation(recipientEmail: String) {
        Log.d("AUTH_LOG", "Sending friend invitation to: $recipientEmail")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.sendFriendInvitation(recipientEmail)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Friend invitation sent successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Send friend invitation failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to send friend invitation",
                        success = null
                    )
                }
            )
        }
    }
    
    fun loadFriendsList() {
        Log.d("AUTH_LOG", "Loading friends list")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            
            val result = authRepository.getFriendsList()
            result.fold(
                onSuccess = { friends ->
                    Log.d("AUTH_LOG", "Friends list loaded successfully, count: ${friends.size}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        friends = friends
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Failed to load friends list: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load friends list"
                    )
                }
            )
        }
    }
    
    fun loadPendingInvitations() {
        Log.d("AUTH_LOG", "Loading pending invitations")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            
            val result = authRepository.getPendingInvitations()
            result.fold(
                onSuccess = { invitations ->
                    Log.d("AUTH_LOG", "Pending invitations loaded successfully, count: ${invitations.size}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        pendingInvitations = invitations
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Failed to load pending invitations: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load pending invitations"
                    )
                }
            )
        }
    }
    
    fun acceptFriendInvitation(invitationCode: String) {
        Log.d("AUTH_LOG", "Accepting friend invitation: $invitationCode")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.acceptFriendInvitation(invitationCode)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Friend invitation accepted successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                    // Reload friends list and invitations
                    loadFriendsList()
                    loadPendingInvitations()
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Accept friend invitation failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to accept friend invitation",
                        success = null
                    )
                }
            )
        }
    }
    
    fun declineFriendInvitation(invitationCode: String) {
        Log.d("AUTH_LOG", "Declining friend invitation: $invitationCode")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.declineFriendInvitation(invitationCode)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Friend invitation declined successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                    // Reload invitations
                    loadPendingInvitations()
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Decline friend invitation failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to decline friend invitation",
                        success = null
                    )
                }
            )
        }
    }
} 