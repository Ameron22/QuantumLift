package com.example.gymtracker.data

import com.google.gson.annotations.SerializedName

// Request to send friend invitation
data class SendFriendInvitationRequest(
    @SerializedName("recipientEmail")
    val recipientEmail: String
)

// Response for friend invitation
data class FriendInvitationResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("invitation")
    val invitation: FriendInvitation? = null
)

// Response for getting friends list
data class FriendsListResponse(
    @SerializedName("friends")
    val friends: List<Friend>
)

// Friend data model
data class Friend(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("friendshipDate")
    val friendshipDate: String
)

// Friend invitation data model
data class FriendInvitation(
    @SerializedName("id")
    val id: Int,
    @SerializedName("invitationCode")
    val invitationCode: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("expiresAt")
    val expiresAt: String,
    @SerializedName("senderUsername")
    val senderUsername: String,
    @SerializedName("senderEmail")
    val senderEmail: String
)

// Response for getting invitations list
data class InvitationsListResponse(
    @SerializedName("invitations")
    val invitations: List<FriendInvitation>
)

// Response for accepting/declining invitation
data class InvitationActionResponse(
    @SerializedName("message")
    val message: String
) 