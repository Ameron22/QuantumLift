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
    
    suspend fun sendFriendInvitation(recipientEmail: String): Result<FriendInvitationResponse> {
        Log.d("AUTH_REPO", "Attempting to send friend invitation to: $recipientEmail")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.sendFriendInvitation(
                SendFriendInvitationRequest(recipientEmail),
                "Bearer $token"
            )
            Log.d("AUTH_REPO", "Send friend invitation response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { invitationResponse ->
                    Log.d("AUTH_REPO", "Friend invitation sent successfully")
                    Result.success(invitationResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Send friend invitation failed with code: ${response.code()}")
                Result.failure(Exception("Failed to send friend invitation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Send friend invitation exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getFriendsList(): Result<List<Friend>> {
        Log.d("AUTH_REPO", "Attempting to get friends list")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getFriendsList("Bearer $token")
            Log.d("AUTH_REPO", "Get friends list response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { friendsResponse ->
                    Log.d("AUTH_REPO", "Friends list retrieved successfully, count: ${friendsResponse.friends.size}")
                    Result.success(friendsResponse.friends)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get friends list failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get friends list: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get friends list exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPendingInvitations(): Result<List<com.example.gymtracker.data.FriendInvitation>> {
        Log.d("AUTH_REPO", "Attempting to get pending invitations")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getPendingInvitations("Bearer $token")
            Log.d("AUTH_REPO", "Get pending invitations response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { invitationsResponse ->
                    Log.d("AUTH_REPO", "Pending invitations retrieved successfully, count: ${invitationsResponse.invitations.size}")
                    Result.success(invitationsResponse.invitations)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get pending invitations failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get pending invitations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get pending invitations exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun acceptFriendInvitation(invitationCode: String): Result<InvitationActionResponse> {
        Log.d("AUTH_REPO", "Attempting to accept friend invitation: $invitationCode")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.acceptFriendInvitation(invitationCode, "Bearer $token")
            Log.d("AUTH_REPO", "Accept friend invitation response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Friend invitation accepted successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Accept friend invitation failed with code: ${response.code()}")
                Result.failure(Exception("Failed to accept friend invitation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Accept friend invitation exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun declineFriendInvitation(invitationCode: String): Result<InvitationActionResponse> {
        Log.d("AUTH_REPO", "Attempting to decline friend invitation: $invitationCode")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.declineFriendInvitation(invitationCode, "Bearer $token")
            Log.d("AUTH_REPO", "Decline friend invitation response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Friend invitation declined successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Decline friend invitation failed with code: ${response.code()}")
                Result.failure(Exception("Failed to decline friend invitation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Decline friend invitation exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Feed methods
    
    suspend fun getFeedPosts(page: Int = 1, limit: Int = 20): Result<List<FeedPost>> {
        Log.d("AUTH_REPO", "Attempting to get feed posts, page: $page")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getFeedPosts(page, limit, "Bearer $token")
            Log.d("AUTH_REPO", "Get feed posts response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { feedResponse ->
                    Log.d("AUTH_REPO", "Feed posts retrieved successfully, count: ${feedResponse.posts.size}")
                    Result.success(feedResponse.posts)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get feed posts failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get feed posts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get feed posts exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun createPost(request: CreatePostRequest): Result<PostActionResponse> {
        Log.d("AUTH_REPO", "Attempting to create post: ${request.postType}")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.createPost(request, "Bearer $token")
            Log.d("AUTH_REPO", "Create post response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Post created successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Create post failed with code: ${response.code()}")
                Result.failure(Exception("Failed to create post: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Create post exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun likePost(postId: String): Result<PostActionResponse> {
        Log.d("AUTH_REPO", "Attempting to like/unlike post: $postId")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.likePost(postId, "Bearer $token")
            Log.d("AUTH_REPO", "Like post response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Post like/unlike successful")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Like post failed with code: ${response.code()}")
                Result.failure(Exception("Failed to like post: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Like post exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getComments(postId: String, page: Int = 1, limit: Int = 20): Result<List<FeedComment>> {
        Log.d("AUTH_REPO", "Attempting to get comments for post: $postId")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getComments(postId, page, limit, "Bearer $token")
            Log.d("AUTH_REPO", "Get comments response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { commentsResponse ->
                    Log.d("AUTH_REPO", "Comments retrieved successfully, count: ${commentsResponse.comments.size}")
                    Result.success(commentsResponse.comments)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get comments failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get comments: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get comments exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun addComment(postId: String, content: String): Result<PostActionResponse> {
        Log.d("AUTH_REPO", "Attempting to add comment to post: $postId")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.addComment(postId, AddCommentRequest(content), "Bearer $token")
            Log.d("AUTH_REPO", "Add comment response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Comment added successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Add comment failed with code: ${response.code()}")
                Result.failure(Exception("Failed to add comment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Add comment exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String): Result<PostActionResponse> {
        Log.d("AUTH_REPO", "Attempting to delete post: $postId")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.deletePost(postId, "Bearer $token")
            Log.d("AUTH_REPO", "Delete post response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Post deleted successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Delete post failed with code: ${response.code()}")
                Result.failure(Exception("Failed to delete post: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Delete post exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPrivacySettings(): Result<PrivacySettings> {
        Log.d("AUTH_REPO", "Attempting to get privacy settings")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getPrivacySettings("Bearer $token")
            Log.d("AUTH_REPO", "Get privacy settings response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { settings ->
                    Log.d("AUTH_REPO", "Privacy settings retrieved successfully")
                    Result.success(settings)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Get privacy settings failed with code: ${response.code()}")
                Result.failure(Exception("Failed to get privacy settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Get privacy settings exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): Result<PostActionResponse> {
        Log.d("AUTH_REPO", "Attempting to update privacy settings")
        return try {
            val token = tokenManager.getStoredToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.updatePrivacySettings(request, "Bearer $token")
            Log.d("AUTH_REPO", "Update privacy settings response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { actionResponse ->
                    Log.d("AUTH_REPO", "Privacy settings updated successfully")
                    Result.success(actionResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Log.e("AUTH_REPO", "Update privacy settings failed with code: ${response.code()}")
                Result.failure(Exception("Failed to update privacy settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Update privacy settings exception: ${e.message}", e)
            Result.failure(e)
        }
    }
} 