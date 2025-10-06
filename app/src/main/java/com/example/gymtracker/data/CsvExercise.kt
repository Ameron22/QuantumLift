package com.example.gymtracker.data

data class CsvExercise(
    val id: Int,
    val title: String,
    val category: String,
    val muscleGroup: String,
    val muscles: List<String>,
    val equipment: String,
    val difficulty: String,
    val gifUrl: String,
    val useTime: Boolean,
    val description: String
) 