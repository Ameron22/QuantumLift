package com.example.gymtracker.data

data class WorkoutShareData(
    val workoutId: String, // Unique ID for the shared workout
    val workoutTitle: String,
    val creatorId: String,
    val creatorUsername: String,
    val exercises: List<WorkoutExerciseShare>,
    val difficulty: String?,
    val createdAt: String,
    val expiresAt: String? // For auto-cleanup (7 days)
)

data class WorkoutExerciseShare(
    val exerciseId: Int?, // null for custom exercises
    val exerciseName: String,
    val isCustomExercise: Boolean,
    val customExerciseData: EntityExercise? // only for custom exercises
)

data class ShareWorkoutRequest(
    val workoutId: Int, // Local workout ID
    val workoutName: String,
    val difficulty: String?,
    val exercises: List<WorkoutExerciseShare>, // Full exercise data from local workout
    val targetUserIds: List<String>, // List of friend user IDs to share with
    val shareType: String = "DIRECT" // For now, only direct sharing
)

data class ShareWorkoutResponse(
    val success: Boolean,
    val message: String,
    val sharedWorkoutId: String?
)

data class CopyWorkoutRequest(
    val sharedWorkoutId: String,
    val targetUserId: String
)

data class CopyWorkoutResponse(
    val success: Boolean,
    val message: String,
    val newWorkoutId: Int?,
    val exercises: List<EntityExercise>?, // Full exercise data for custom exercises
    val workoutName: String?
) 