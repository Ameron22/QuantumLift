package com.example.gymtracker.data

import androidx.room.*

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertWorkoutExerciseCrossRef(crossRef: WorkoutExerciseCrossRef)

    @Transaction
    @Query("SELECT * FROM WorkoutEntity WHERE id = :workoutId")
    fun getWorkoutWithExercises(workoutId: Int): List<WorkoutWithExercises>
    @Query("SELECT * FROM WorkoutEntity")
    suspend  fun getAllWorkouts(): List<WorkoutEntity>
}