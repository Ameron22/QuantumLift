package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_alternatives",
    indices = [
        Index(value = ["originalExerciseId"]),
        Index(value = ["alternativeExerciseId"]),
        Index(value = ["workoutExerciseId"])
    ]
)
data class ExerciseAlternative(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalExerciseId: Int,  // The exercise being replaced
    val alternativeExerciseId: Int,  // The alternative exercise
    val workoutExerciseId: Int,  // The specific workout exercise instance
    val order: Int = 0,  // Order of alternatives
    val isActive: Boolean = false  // Whether this alternative is currently selected
)

