package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
//This table will store metadata for each workout session.
@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["workoutId"])
    ]
)
@TypeConverters(Converter::class)
data class SessionWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val workoutId: Int?, // Foreign key to WorkoutEntity
    val startTime: Long, // Timestamp in milliseconds
    val endTime: Long, // Timestamp in milliseconds (0 if not completed)
    val workoutName: String? = null // Optional workout name
)

// Add new data class for temporary recovery factors
data class TempRecoveryFactors(
    val sleepQuality: Int = 0,
    val proteinIntake: Int = 0,
    val hydration: Int = 0,
    val stressLevel: Int = 0
)