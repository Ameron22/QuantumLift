package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_warm_ups",
    primaryKeys = ["workoutId", "templateId"]
)
data class WorkoutWarmUp(
    val workoutId: Int, // Foreign key to EntityWorkout
    val templateId: Int, // Foreign key to WarmUpTemplate
    val isCustomized: Boolean = false, // Whether the user has modified the template
    val customDuration: Int? = null, // Custom duration if modified
    val addedAt: Long = System.currentTimeMillis()
)

