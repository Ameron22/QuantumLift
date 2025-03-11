package com.example.gymtracker.data
import androidx.room.*

data class WorkoutWithExercises(
    @Embedded val workout: EntityWorkout,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            CrossRefWorkoutExercise::class,
            parentColumn = "workoutId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<EntityExercise>
)