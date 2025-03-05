package com.example.gymtracker.data

import androidx.room.*

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
}
