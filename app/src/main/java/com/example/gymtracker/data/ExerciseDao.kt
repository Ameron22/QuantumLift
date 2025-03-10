package com.example.gymtracker.data

import androidx.room.*
import com.example.gymtracker.classes.WorkoutSessionWithMusclesStress
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // Add this method to check if the database is empty
    @Query("SELECT COUNT(*) FROM WorkoutEntity")
    suspend fun getWorkoutCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertWorkoutExerciseCrossRef(crossRef: WorkoutExerciseCrossRef)

    @Transaction
    @Query("SELECT * FROM WorkoutEntity WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Int): List<WorkoutWithExercises>

    @Query("SELECT * FROM WorkoutEntity")
    suspend fun getAllWorkouts(): List<WorkoutEntity>

    @Insert
    fun insertWorkoutSession(entity: WorkoutSessionEntity): Long

    @Insert
    fun insertExerciseSession(entity: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions")
    suspend fun getAllExerciseSessions(): List<ExerciseSessionEntity>

    @Query("UPDATE workout_sessions Set duration = :duration WHERE sessionId = :sessionId")
    suspend fun updateWorkoutSessionDuration(sessionId: Long, duration: Long)

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllWorkoutSessions(): List<WorkoutSessionEntity>
    @Query("SELECT ws.sessionId, ws.workoutId, ws.startTime, ws.duration, ws.workoutName, es.muscleGroup , es.sets , es.repsOrTime, es.weight " +
            "FROM workout_sessions ws INNER JOIN exercise_sessions es " +
            "ON ws.sessionId = es.sessionId " +
            "GROUP BY ws.sessionId, es.muscleGroup")
    fun getAllWorkoutSessionsWithMuscleStress(): Flow <List<WorkoutSessionWithMusclesStress>>
}