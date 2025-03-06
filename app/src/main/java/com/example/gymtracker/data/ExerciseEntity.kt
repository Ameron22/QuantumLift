package com.example.gymtracker.data
import androidx.room.*

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sets: String,
    val reps: String,
    val muscle: String, // Primary muscle group
    val part: String // Specific part of the muscle

)