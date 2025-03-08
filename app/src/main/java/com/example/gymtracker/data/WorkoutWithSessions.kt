package com.example.gymtracker.data

import androidx.room.Embedded
import androidx.room.Relation
//A helper class to represent a workout session with its associated exercises.
data class WorkoutWithSessions(
    @Embedded val workoutSession: WorkoutSessionEntity,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val exercises: List<ExerciseSessionEntity>
)