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
} 