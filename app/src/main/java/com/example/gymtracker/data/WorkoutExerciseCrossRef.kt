package com.example.gymtracker.data
import androidx.room.*

@Entity(primaryKeys = ["workoutId", "exerciseId"])
data class WorkoutExerciseCrossRef(
    val workoutId: Int,
    val exerciseId: Int
)