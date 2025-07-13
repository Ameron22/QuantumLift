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
import com.example.gymtracker.data.FeedPost
import com.example.gymtracker.data.FeedComment
import com.example.gymtracker.data.CreatePostRequest
import com.example.gymtracker.data.PostActionResponse
import com.example.gymtracker.data.PrivacySettings
import com.example.gymtracker.data.UpdatePrivacySettingsRequest
import com.example.gymtracker.data.CopyWorkoutRequest
import com.example.gymtracker.data.CopyWorkoutResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val user: com.example.gymtracker.data.User? = null,
    val friends: List<Friend> = emptyList(),
    val pendingInvitations: List<FriendInvitation> = emptyList(),
    val feedPosts: List<FeedPost> = emptyList(),
    val privacySettings: PrivacySettings? = null
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
                    clearSuccessAfterDelay()
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
                    clearSuccessAfterDelay()
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
    
    fun clearSuccessAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Clear after 0.5 seconds
            clearSuccess()
        }
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
    
    // Feed methods
    
    fun loadFeedPosts(page: Int = 1) {
        Log.d("AUTH_LOG", "Loading feed posts, page: $page")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            
            val result = authRepository.getFeedPosts(page)
            result.fold(
                onSuccess = { posts ->
                    Log.d("AUTH_LOG", "Feed posts loaded successfully, count: ${posts.size}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        feedPosts = if (page == 1) posts else _authState.value.feedPosts + posts
                    )
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Failed to load feed posts: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load feed posts"
                    )
                }
            )
        }
    }
    
    fun createPost(request: CreatePostRequest) {
        Log.d("AUTH_LOG", "Creating post: ${request.postType}")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.createPost(request)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Post created successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                    // Reload feed posts
                    loadFeedPosts()
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Create post failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to create post",
                        success = null
                    )
                }
            )
        }
    }
    
    fun likePost(postId: String) {
        Log.d("AUTH_LOG", "Liking/unliking post: $postId")
        viewModelScope.launch {
            val result = authRepository.likePost(postId)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Post like/unlike successful")
                    // Update the post in the feed
                    val updatedPosts = _authState.value.feedPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likesCount = if (response.liked == true) post.likesCount + 1 else post.likesCount - 1,
                                isLikedByUser = response.liked ?: post.isLikedByUser
                            )
                        } else {
                            post
                        }
                    }
                    _authState.value = _authState.value.copy(feedPosts = updatedPosts)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Like post failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Failed to like post"
                    )
                }
            )
        }
    }
    
    fun addComment(postId: String, content: String) {
        Log.d("AUTH_LOG", "Adding comment to post: $postId")
        viewModelScope.launch {
            val result = authRepository.addComment(postId, content)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Comment added successfully")
                    // Update the post comment count
                    val updatedPosts = _authState.value.feedPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = post.commentsCount + 1)
                        } else {
                            post
                        }
                    }
                    _authState.value = _authState.value.copy(feedPosts = updatedPosts)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Add comment failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Failed to add comment"
                    )
                }
            )
        }
    }
    
    fun deletePost(postId: String) {
        Log.d("AUTH_LOG", "Deleting post: $postId")
        viewModelScope.launch {
            val result = authRepository.deletePost(postId)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Post deleted successfully")
                    // Remove the post from the feed
                    val updatedPosts = _authState.value.feedPosts.filter { it.id != postId }
                    _authState.value = _authState.value.copy(feedPosts = updatedPosts)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Delete post failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Failed to delete post"
                    )
                }
            )
        }
    }
    
    fun loadPrivacySettings() {
        Log.d("AUTH_LOG", "Loading privacy settings")
        viewModelScope.launch {
            val result = authRepository.getPrivacySettings()
            result.fold(
                onSuccess = { settings ->
                    Log.d("AUTH_LOG", "Privacy settings loaded successfully")
                    _authState.value = _authState.value.copy(privacySettings = settings)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Failed to load privacy settings: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Failed to load privacy settings"
                    )
                }
            )
        }
    }
    
    fun updatePrivacySettings(request: UpdatePrivacySettingsRequest) {
        Log.d("AUTH_LOG", "Updating privacy settings")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val result = authRepository.updatePrivacySettings(request)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Privacy settings updated successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                    // Reload privacy settings
                    loadPrivacySettings()
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Update privacy settings failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to update privacy settings",
                        success = null
                    )
                }
            )
        }
    }
    
    fun getComments(postId: String): Result<List<FeedComment>> {
        return runBlocking {
            authRepository.getComments(postId)
        }
    }
    
    // Workout sharing methods
    
    fun copyWorkout(sharedWorkoutId: String, onSuccess: (Int, String) -> Unit, onError: (String) -> Unit) {
        Log.d("AUTH_LOG", "Copying workout: $sharedWorkoutId")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )
            
            val request = CopyWorkoutRequest(
                sharedWorkoutId = sharedWorkoutId,
                targetUserId = _authState.value.user?.id ?: ""
            )
            
            val result = authRepository.copyWorkout(request)
            result.fold(
                onSuccess = { response ->
                    Log.d("AUTH_LOG", "Workout copied successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        success = response.message,
                        error = null
                    )
                    
                    if (response.success && response.newWorkoutId != null && response.workoutName != null) {
                        onSuccess(response.newWorkoutId, response.workoutName)
                    } else {
                        onError(response.message ?: "Failed to copy workout")
                    }
                    
                    // Remove the shared workout post from feed
                    val updatedPosts = _authState.value.feedPosts.filter { post ->
                        !(post.postType == "WORKOUT_SHARED" && 
                          post.workoutShareData?.workoutId == sharedWorkoutId)
                    }
                    _authState.value = _authState.value.copy(feedPosts = updatedPosts)
                },
                onFailure = { exception ->
                    Log.e("AUTH_LOG", "Copy workout failed: ${exception.message}", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to copy workout",
                        success = null
                    )
                    onError(exception.message ?: "Failed to copy workout")
                }
            )
        }
    }
} 