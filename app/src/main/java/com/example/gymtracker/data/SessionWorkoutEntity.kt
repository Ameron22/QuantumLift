package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
//This table will store metadata for each workout session.
@Entity(tableName = "workout_sessions")
@TypeConverters(Converter::class)
data class SessionWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val workoutId: Int?, // Foreign key to WorkoutEntity
    val startTime: Long, // Timestamp in milliseconds
    val duration: Long, // Duration in seconds
    val workoutName: String? = null, // Optional workout name
    val recoveryFactors: RecoveryFactors? = null, // Recovery factors for the workout session
    val tempRecoveryFactors: String? = null  // Add this field to store temporary recovery factors
)

// Add new data class for temporary recovery factors
data class TempRecoveryFactors(
    val sleepQuality: Int = 0,
    val proteinIntake: Int = 0,
    val hydration: Int = 0,
    val stressLevel: Int = 0
)