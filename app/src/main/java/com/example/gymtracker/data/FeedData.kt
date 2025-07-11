package com.example.gymtracker.data

import com.google.gson.annotations.SerializedName

// Feed post data model
data class FeedPost(
    @SerializedName("id")
    val id: String, // Changed from Int to String for UUID
    @SerializedName("postType")
    val postType: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("workoutData")
    val workoutData: WorkoutData?,
    @SerializedName("achievementData")
    val achievementData: AchievementData?,
    @SerializedName("challengeData")
    val challengeData: ChallengeData?,
    @SerializedName("privacyLevel")
    val privacyLevel: String,
    @SerializedName("likesCount")
    val likesCount: Int,
    @SerializedName("commentsCount")
    val commentsCount: Int,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("user")
    val user: FeedUser,
    @SerializedName("isLikedByUser")
    val isLikedByUser: Boolean
)

// User data for feed posts
data class FeedUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("profilePicture")
    val profilePicture: String?
)

// Workout data for workout posts
data class WorkoutData(
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("exercises")
    val exercises: List<String>?,
    @SerializedName("totalSets")
    val totalSets: Int?,
    @SerializedName("totalWeight")
    val totalWeight: Double?
)

// Achievement data for achievement posts
data class AchievementData(
    @SerializedName("type")
    val type: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("description")
    val description: String?
)

// Challenge data for challenge posts
data class ChallengeData(
    @SerializedName("challengeType")
    val challengeType: String,
    @SerializedName("participants")
    val participants: List<String>?,
    @SerializedName("goal")
    val goal: String?
)

// Comment data model
data class FeedComment(
    @SerializedName("id")
    val id: String, // Changed from Int to String for UUID
    @SerializedName("content")
    val content: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("user")
    val user: FeedUser
)

// Response for getting feed posts
data class FeedPostsResponse(
    @SerializedName("posts")
    val posts: List<FeedPost>,
    @SerializedName("pagination")
    val pagination: Pagination
)

// Response for getting comments
data class CommentsResponse(
    @SerializedName("comments")
    val comments: List<FeedComment>,
    @SerializedName("pagination")
    val pagination: Pagination
)

// Pagination data
data class Pagination(
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("hasMore")
    val hasMore: Boolean
)

// Request to create a post
data class CreatePostRequest(
    @SerializedName("postType")
    val postType: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("workoutData")
    val workoutData: WorkoutData? = null,
    @SerializedName("achievementData")
    val achievementData: AchievementData? = null,
    @SerializedName("challengeData")
    val challengeData: ChallengeData? = null,
    @SerializedName("privacyLevel")
    val privacyLevel: String = "FRIENDS"
)

// Request to add a comment
data class AddCommentRequest(
    @SerializedName("content")
    val content: String
)

// Response for post actions (like, comment, create)
data class PostActionResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("post")
    val post: PostActionData? = null,
    @SerializedName("comment")
    val comment: CommentActionData? = null,
    @SerializedName("liked")
    val liked: Boolean? = null
)

// Data for post creation response
data class PostActionData(
    @SerializedName("id")
    val id: String, // Changed from Int to String for UUID
    @SerializedName("createdAt")
    val createdAt: String
)

// Data for comment creation response
data class CommentActionData(
    @SerializedName("id")
    val id: String, // Changed from Int to String for UUID
    @SerializedName("createdAt")
    val createdAt: String
)

// Privacy settings data
data class PrivacySettings(
    @SerializedName("autoShareWorkouts")
    val autoShareWorkouts: Boolean,
    @SerializedName("autoShareAchievements")
    val autoShareAchievements: Boolean,
    @SerializedName("defaultPostPrivacy")
    val defaultPostPrivacy: String
)

// Request to update privacy settings
data class UpdatePrivacySettingsRequest(
    @SerializedName("autoShareWorkouts")
    val autoShareWorkouts: Boolean,
    @SerializedName("autoShareAchievements")
    val autoShareAchievements: Boolean,
    @SerializedName("defaultPostPrivacy")
    val defaultPostPrivacy: String
) 