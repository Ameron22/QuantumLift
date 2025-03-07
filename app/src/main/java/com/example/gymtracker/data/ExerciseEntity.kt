package com.example.gymtracker.data
import androidx.room.*

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sets: Int,
    val reps: Int,
    val muscle: String, // Primary muscle group
    val part: List<String> // Specific parts of the muscle

)