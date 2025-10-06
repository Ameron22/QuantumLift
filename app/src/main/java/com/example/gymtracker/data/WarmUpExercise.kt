package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warm_up_exercises")
data class WarmUpExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val templateId: Int, // Foreign key to WarmUpTemplate
    val exerciseId: Int, // Foreign key to EntityExercise
    val order: Int = 0, // Position in the warm-up sequence
    val sets: Int = 1, // Number of sets for this exercise
    val reps: Int = 10, // Number of reps (for rep-based exercises)
    val duration: Int = 30, // Duration in seconds (for time-based exercises)
    val isTimeBased: Boolean = false, // Whether this exercise is time-based or rep-based
    val restBetweenSets: Int = 0, // Rest time between sets in seconds
    val weight: Int = 0, // Weight in kg/lbs for this exercise
    val notes: String = "" // Additional notes for this exercise
)

