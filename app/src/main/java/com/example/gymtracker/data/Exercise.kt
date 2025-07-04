package com.example.gymtracker.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Exercise(
    val name: String,
    val muscle: String,
    val part: List<String>,
    val gifUrl: String = "", // URL to the exercise demonstration GIF
    val difficulty: String = "Intermediate", // Difficulty level: Beginner, Intermediate, Advanced
    val description: String = "" // Description of the exercise
) : Parcelable