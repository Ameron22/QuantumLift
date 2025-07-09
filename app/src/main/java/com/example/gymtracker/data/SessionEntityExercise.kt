package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Entity class to store details for each exercise performed during a workout session.
 * Includes new fields for muscle soreness tracking.
 */
@Entity(tableName = "exercise_sessions")
@TypeConverters(RecoveryFactorsConverters::class)
data class SessionEntityExercise(
    @PrimaryKey(autoGenerate = true) val exerciseSessionId: Long = 0,
    val sessionId: Long, // Foreign key to the workout session
    val exerciseId: Long, // Foreign key to the exercise
    val sets: Int, // Total number of sets
    val repsOrTime: List<Int?>, // List of reps or time for each set (nullable)
    val weight: List<Int?>, // List of weights for each set (nullable)
    val muscleGroup: String, // Muscle group targeted
    val muscleParts: String, // Specific muscle parts targeted
    val completedSets: Int, // Number of completed sets
    val notes: String, // Additional notes
    
    // New fields for muscle soreness tracking
    val eccentricFactor: Float = 1.0f, // Multiplier for eccentric component (1.0-2.0)
    val noveltyFactor: Int = 5, // How new/different the exercise is (0-10)
    val adaptationLevel: Int = 5, // How adapted you are to the exercise (0-10)
    val rpe: Int = 5, // Rate of Perceived Exertion (1-10)
    val subjectiveSoreness: Int = 5, // Post-workout soreness rating (1-10)
    val recoveryFactors: RecoveryFactors = RecoveryFactors(7, 150, 7, 5) // Default recovery factors
)