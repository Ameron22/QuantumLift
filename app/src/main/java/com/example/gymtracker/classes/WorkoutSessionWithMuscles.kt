package com.example.gymtracker.classes

import com.example.gymtracker.screens.Exercise

data class WorkoutSessionWithMuscles(
    val sessionId: Long,
    val workoutId: Int?,
    val startTime: Long,
    val duration: Long,
    val workoutName: String?,
    val muscleGroups: Map<String, Int>,// List of muscle groups

)