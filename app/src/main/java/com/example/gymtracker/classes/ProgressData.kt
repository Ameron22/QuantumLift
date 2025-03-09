package com.example.gymtracker.classes

data class ProgressData(
    val durationOverTime: List<Pair<Long, Long>>, // (timestamp, duration)
    val setsRepsByExercise: Map<String, Int>, // (exerciseName, totalReps)
    val muscleActivation: Map<String, Int> // (muscleGroup, activationCount)
)