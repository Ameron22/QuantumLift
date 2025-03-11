package com.example.gymtracker.classes

data class SessionWorkoutWithMusclesStress(
    val sessionId: Long,
    val workoutId: Int?,
    val startTime: Long,
    val duration: Long,
    val workoutName: String?,
    val muscleGroup: String, // Muscle group name
    val sets: Int, // Number of sets
    val repsOrTime: Int, // Reps or time per set
    val weight: Int // Weight used
)