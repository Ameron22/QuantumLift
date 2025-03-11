package com.example.gymtracker.data
import androidx.room.*

@Entity(
    primaryKeys = ["workoutId", "exerciseId"],
    indices = [Index(value = ["exerciseId"])]
)
data class CrossRefWorkoutExercise(
    val workoutId: Int,
    val exerciseId: Int
)