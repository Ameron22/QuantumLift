package com.example.gymtracker.data

import androidx.room.*
import com.example.gymtracker.classes.SessionWorkoutWithMusclesStress
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // Add this method to check if the database is empty
    @Query("SELECT COUNT(*) FROM EntityWorkout")
    suspend fun getWorkoutCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: EntityExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: EntityWorkout): Long

    @Insert
    suspend fun insertWorkoutExerciseCrossRef(crossRef: CrossRefWorkoutExercise)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): EntityExercise?

    @Transaction
    @Query("SELECT * FROM EntityWorkout WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Int): List<WorkoutWithExercises>

    @Query("SELECT * FROM EntityWorkout")
    suspend fun getAllWorkouts(): List<EntityWorkout>

    @Insert
    fun insertWorkoutSession(entity: SessionWorkoutEntity): Long

    @Insert
    fun insertExerciseSession(entity: SessionEntityExercise)

    @Query("SELECT * FROM exercise_sessions")
    suspend fun getAllExerciseSessions(): List<SessionEntityExercise>

    @Query("UPDATE workout_sessions Set duration = :duration WHERE sessionId = :sessionId")
    suspend fun updateWorkoutSessionDuration(sessionId: Long, duration: Long)

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllWorkoutSessions(): List<SessionWorkoutEntity>

    @Query("SELECT ws.sessionId, ws.workoutId, ws.startTime, ws.duration, ws.workoutName, es.muscleGroup , es.sets , es.repsOrTime, es.weight " +
            "FROM workout_sessions ws INNER JOIN exercise_sessions es " +
            "ON ws.sessionId = es.sessionId " +
            "GROUP BY ws.sessionId, es.muscleGroup")
    fun getAllWorkoutSessionsWithMuscleStress(): Flow <List<SessionWorkoutWithMusclesStress>>

    @Query("SELECT * FROM workout_sessions WHERE workoutId = :workoutId")
    suspend fun getWorkoutSessionsByWorkoutId(workoutId: Int): List<SessionWorkoutEntity>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWorkoutSession(sessionId: Long): SessionWorkoutEntity?
}