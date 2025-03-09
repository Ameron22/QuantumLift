package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
//This table will store metadata for each workout session.
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val workoutId: Int?, // Foreign key to WorkoutEntity
    val startTime: Long, // Timestamp in milliseconds
    val duration: Long, // Duration in seconds
    val workoutName: String? = null // Optional workout name
)