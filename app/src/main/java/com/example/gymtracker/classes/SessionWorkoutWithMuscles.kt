package com.example.gymtracker.classes

data class SessionWorkoutWithMuscles(
    val sessionId: Long,
    val workoutId: Int?,
    val startTime: Long,
    val endTime: Long,
    val workoutName: String?,
    val muscleGroups: Map<String, Int>,// List of muscle groups

)