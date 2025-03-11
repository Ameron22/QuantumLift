package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
//This table will store details for each exercise performed during the workout session.
@Entity(tableName = "exercise_sessions")
data class SessionEntityExercise(
    @PrimaryKey(autoGenerate = true) val exerciseSessionId: Long = 0,
    val sessionId: Long?, // Foreign key to WorkoutSessionEntity
    val exerciseId: Long?, // Foreign key to ExerciseEntity
    val sets: Int?,
    val repsOrTime: Int?, // Reps or time in seconds
    val weight: Int?,
    val muscleGroup: String,
    val muscleParts: List<String>, // Store as JSON or use a separate table
    val completedSets: Int,

    val completedRepsOrTime: Int,
    val notes: String? = null // Optional notes
)