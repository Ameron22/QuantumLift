package com.example.gymtracker.data

/**
 * Data class combining soreness assessment with workout context for ML training
 */
data class SorenessTrainingData(
    val assessment: SorenessAssessment,
    val context: WorkoutContext
)
