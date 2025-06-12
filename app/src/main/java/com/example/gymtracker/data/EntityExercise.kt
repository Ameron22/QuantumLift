package com.example.gymtracker.data

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "exercises")
data class EntityExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Int,
    val muscle: String, // Primary muscle group
    val part: List<String>, // Specific parts of the muscle
    val gifUrl: String = "", // Path to the stored GIF file
    val difficulty: String = "Intermediate" // Difficulty level: Beginner, Intermediate, Advanced
) : Parcelable