package com.example.gymtracker.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import com.example.gymtracker.data.LoginRequest
import com.example.gymtracker.data.RegisterRequest
import com.example.gymtracker.data.AuthResponse
import com.example.gymtracker.data.ChangePasswordRequest
import com.example.gymtracker.data.ProfileResponse
import com.example.gymtracker.data.SendFriendInvitationRequest
import com.example.gymtracker.data.FriendInvitationResponse
import com.example.gymtracker.data.FriendsListResponse
import com.example.gymtracker.data.InvitationsListResponse
import com.example.gymtracker.data.InvitationActionResponse
import com.example.gymtracker.data.FeedPostsResponse
import com.example.gymtracker.data.CommentsResponse
import com.example.gymtracker.data.CreatePostRequest
import com.example.gymtracker.data.AddCommentRequest
import com.example.gymtracker.data.PostActionResponse
import com.example.gymtracker.data.PrivacySettings
import com.example.gymtracker.data.UpdatePrivacySettingsRequest
import com.example.gymtracker.data.UpdateWorkoutPrivacySettingsRequest
import com.example.gymtracker.data.WorkoutCompletionRequest
import com.example.gymtracker.data.WorkoutCompletionResponse
import com.example.gymtracker.data.WorkoutPrivacySettings
import com.example.gymtracker.data.ShareWorkoutRequest
import com.example.gymtracker.data.ShareWorkoutResponse
import com.example.gymtracker.data.CopyWorkoutRequest
import com.example.gymtracker.data.CopyWorkoutResponse
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Query
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
    
    /**
     * Change password endpoint
     * @param changePasswordRequest The password change data
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Body changePasswordRequest: ChangePasswordRequest,
        @Header("Authorization") authorization: String
    ): Response<AuthResponse>
    
    /**
     * Get user profile endpoint
     * @param authorization Bearer token for authentication
     * @return Response containing user profile data
     */
    @GET("api/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") authorization: String
    ): Response<ProfileResponse>
    
    /**
     * Send friend invitation endpoint
     * @param request The friend invitation request
     * @param authorization Bearer token for authentication
     * @return Response containing invitation details
     */
    @POST("api/friends/invite")
    suspend fun sendFriendInvitation(
        @Body request: SendFriendInvitationRequest,
        @Header("Authorization") authorization: String
    ): Response<FriendInvitationResponse>
    
    /**
     * Get friends list endpoint
     * @param authorization Bearer token for authentication
     * @return Response containing list of friends
     */
    @GET("api/friends/list")
    suspend fun getFriendsList(
        @Header("Authorization") authorization: String
    ): Response<FriendsListResponse>
    
    /**
     * Get pending invitations endpoint
     * @param authorization Bearer token for authentication
     * @return Response containing list of pending invitations
     */
    @GET("api/friends/invitations")
    suspend fun getPendingInvitations(
        @Header("Authorization") authorization: String
    ): Response<InvitationsListResponse>
    
    /**
     * Accept friend invitation endpoint
     * @param invitationCode The invitation code to accept
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/friends/accept/{invitationCode}")
    suspend fun acceptFriendInvitation(
        @retrofit2.http.Path("invitationCode") invitationCode: String,
        @Header("Authorization") authorization: String
    ): Response<InvitationActionResponse>
    
    /**
     * Decline friend invitation endpoint
     * @param invitationCode The invitation code to decline
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/friends/decline/{invitationCode}")
    suspend fun declineFriendInvitation(
        @retrofit2.http.Path("invitationCode") invitationCode: String,
        @Header("Authorization") authorization: String
    ): Response<InvitationActionResponse>
    
    // Feed API endpoints
    
    /**
     * Get feed posts endpoint
     * @param page Page number for pagination
     * @param limit Number of posts per page
     * @param authorization Bearer token for authentication
     * @return Response containing feed posts
     */
    @GET("api/feed/posts")
    suspend fun getFeedPosts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): Response<FeedPostsResponse>
    
    /**
     * Create a new post endpoint
     * @param request Post creation request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/feed/posts")
    suspend fun createPost(
        @Body request: CreatePostRequest,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    /**
     * Like/unlike a post endpoint
     * @param postId The post ID to like/unlike
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/feed/posts/{postId}/like")
    suspend fun likePost(
        @retrofit2.http.Path("postId") postId: String,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    /**
     * Get comments for a post endpoint
     * @param postId The post ID to get comments for
     * @param page Page number for pagination
     * @param limit Number of comments per page
     * @param authorization Bearer token for authentication
     * @return Response containing comments
     */
    @GET("api/feed/posts/{postId}/comments")
    suspend fun getComments(
        @retrofit2.http.Path("postId") postId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): Response<CommentsResponse>
    
    /**
     * Add a comment to a post endpoint
     * @param postId The post ID to comment on
     * @param request Comment request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/feed/posts/{postId}/comments")
    suspend fun addComment(
        @retrofit2.http.Path("postId") postId: String,
        @Body request: AddCommentRequest,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    /**
     * Delete a post endpoint
     * @param postId The post ID to delete
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @DELETE("api/feed/posts/{postId}")
    suspend fun deletePost(
        @retrofit2.http.Path("postId") postId: String,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    /**
     * Get user privacy settings endpoint
     * @param authorization Bearer token for authentication
     * @return Response containing privacy settings
     */
    @GET("api/feed/privacy-settings")
    suspend fun getPrivacySettings(
        @Header("Authorization") authorization: String
    ): Response<PrivacySettings>
    
    /**
     * Update user privacy settings endpoint
     * @param request Privacy settings update request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @PUT("api/feed/privacy-settings")
    suspend fun updatePrivacySettings(
        @Body request: UpdatePrivacySettingsRequest,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    // Workout API endpoints
    
    /**
     * Complete workout and optionally share to feed endpoint
     * @param request Workout completion request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/workouts/complete")
    suspend fun completeWorkout(
        @Body request: WorkoutCompletionRequest,
        @Header("Authorization") authorization: String
    ): Response<WorkoutCompletionResponse>
    
    /**
     * Get workout privacy settings endpoint
     * @param authorization Bearer token for authentication
     * @return Response containing workout privacy settings
     */
    @GET("api/workouts/privacy-settings")
    suspend fun getWorkoutPrivacySettings(
        @Header("Authorization") authorization: String
    ): Response<WorkoutPrivacySettings>
    
    /**
     * Update workout privacy settings endpoint
     * @param request Workout privacy settings update request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @PUT("api/workouts/privacy-settings")
    suspend fun updateWorkoutPrivacySettings(
        @Body request: UpdateWorkoutPrivacySettingsRequest,
        @Header("Authorization") authorization: String
    ): Response<PostActionResponse>
    
    // Workout sharing API endpoints
    
    /**
     * Share workout with friends endpoint
     * @param request Share workout request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/workouts/share")
    suspend fun shareWorkout(
        @Body request: ShareWorkoutRequest,
        @Header("Authorization") authorization: String
    ): Response<ShareWorkoutResponse>
    
    /**
     * Copy shared workout endpoint
     * @param request Copy workout request
     * @param authorization Bearer token for authentication
     * @return Response containing success/error message
     */
    @POST("api/workouts/copy")
    suspend fun copyWorkout(
        @Body request: CopyWorkoutRequest,
        @Header("Authorization") authorization: String
    ): Response<CopyWorkoutResponse>
} 