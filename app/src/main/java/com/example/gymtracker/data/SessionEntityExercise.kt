package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
//This table will store details for each exercise performed during the workout session.
@Entity(tableName = "exercise_sessions")
data class SessionEntityExercise(
    @PrimaryKey(autoGenerate = true) val exerciseSessionId: Long = 0,
    val sessionId: Long, // Foreign key to the workout session
    val exerciseId: Long, // Foreign key to the exercise
    val sets: Int, // Total number of sets
    val repsOrTime: List<Int?>, // List of reps or time for each set (nullable)
    val weight: List<Int?>, // List of weights for each set (nullable)
    val muscleGroup: String, // Muscle group targeted
    val muscleParts: List<String>, // Specific muscle parts targeted
    val completedSets: Int, // Number of completed sets
    val notes: String // Additional notes
)