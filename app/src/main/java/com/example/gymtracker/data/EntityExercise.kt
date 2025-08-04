package com.example.gymtracker.data

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "exercises")
data class EntityExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val category: String = "", // Exercise category (e.g., Strength, Stretching, etc.)
    val muscle: String, // Primary muscle group
    val parts: String, // JSON string of specific parts of the muscle
    val equipment: String = "", // Equipment required for the exercise
    val difficulty: String = "Intermediate", // Difficulty level
    val gifUrl: String = "",
    val useTime: Boolean = false // True if exercise is time-based, false if rep-based
) : Parcelable