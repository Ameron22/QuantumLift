package com.example.gymtracker.data

import com.google.gson.annotations.SerializedName

// Workout completion request
data class WorkoutCompletionRequest(
    @SerializedName("workoutId")
    val workoutId: Int,
    @SerializedName("workoutName")
    val workoutName: String,
    @SerializedName("duration")
    val duration: Long, // Duration in milliseconds
    @SerializedName("exercises")
    val exercises: List<String>? = null,
    @SerializedName("totalSets")
    val totalSets: Int? = null,
    @SerializedName("totalWeight")
    val totalWeight: Double? = null,
    @SerializedName("shareToFeed")
    val shareToFeed: Boolean = false,
    @SerializedName("privacyLevel")
    val privacyLevel: String = "FRIENDS" // PUBLIC, FRIENDS, PRIVATE
)

// Workout completion response
data class WorkoutCompletionResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("shared")
    val shared: Boolean,
    @SerializedName("privacyLevel")
    val privacyLevel: String
)

// Workout privacy settings
data class WorkoutPrivacySettings(
    @SerializedName("autoShareWorkouts")
    val autoShareWorkouts: Boolean,
    @SerializedName("defaultPostPrivacy")
    val defaultPostPrivacy: String
)

// Update workout privacy settings request
data class UpdateWorkoutPrivacySettingsRequest(
    @SerializedName("autoShareWorkouts")
    val autoShareWorkouts: Boolean,
    @SerializedName("defaultPostPrivacy")
    val defaultPostPrivacy: String
) 