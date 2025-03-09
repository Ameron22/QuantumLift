package com.example.gymtracker.classes

data class WorkoutSessionWithMusclesStress(
    val sessionId: Long,
    val workoutId: Int?,
    val startTime: Long,
    val duration: Long,
    val workoutName: String?,
    val muscleGroup: String, // Muscle group name
    val totalStress: Int // Total stress for this muscle group
)