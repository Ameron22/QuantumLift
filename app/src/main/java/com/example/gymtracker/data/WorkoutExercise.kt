package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_exercises")
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val exerciseId: Int,  // Foreign key to EntityExercise
    val workoutId: Int,   // Foreign key to EntityWorkout
    val sets: Int,
    val reps: Int,
    val weight: Int,
    val order: Int = 0    // Position in workout
) 