package com.example.gymtracker.data
import androidx.room.*

@Entity(
    tableName = "workout_exercise_cross_ref",
    primaryKeys = ["workoutId", "exerciseId"],
    indices = [Index(value = ["exerciseId"])]
)
data class CrossRefWorkoutExercise(
    val workoutId: Int,
    val exerciseId: Int,
    val order: Int = 0 // Position of the exercise in the workout
)