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

    @Update
    suspend fun updateExercise(exercise: EntityExercise)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<EntityExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: EntityWorkout): Long

    @Insert
    suspend fun insertWorkoutExerciseCrossRef(crossRef: CrossRefWorkoutExercise)

    @Delete
    suspend fun deleteWorkoutExerciseCrossRef(crossRef: CrossRefWorkoutExercise)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): EntityExercise?

    @Transaction
    @Query("SELECT * FROM EntityWorkout WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Int): List<WorkoutWithExercises>

    @Query("SELECT * FROM EntityWorkout")
    suspend fun getAllWorkouts(): List<EntityWorkout>

    @Insert
    suspend fun insertWorkoutSession(entity: SessionWorkoutEntity): Long

    @Update
    suspend fun updateWorkoutSession(entity: SessionWorkoutEntity)

    @Insert
    suspend fun insertExerciseSession(entity: SessionEntityExercise)

    @Query("SELECT * FROM exercise_sessions")
    fun getAllExerciseSessions(): Flow<List<SessionEntityExercise>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkoutSessionsFlow(): Flow<List<SessionWorkoutEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllWorkoutSessions(): List<SessionWorkoutEntity>

    @Query("UPDATE workout_sessions Set endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateWorkoutSessionEndTime(sessionId: Long, endTime: Long)

    @Query("SELECT ws.sessionId, ws.workoutId, ws.startTime, ws.endTime, ws.workoutName, es.muscleGroup, es.sets, es.repsOrTime, es.weight " +
            "FROM workout_sessions ws INNER JOIN exercise_sessions es " +
            "ON ws.sessionId = es.sessionId " +
            "GROUP BY ws.sessionId, es.muscleGroup " +
            "ORDER BY ws.startTime DESC")
    fun getAllWorkoutSessionsWithMuscleStress(): Flow<List<SessionWorkoutWithMusclesStress>>

    @Query("SELECT * FROM workout_sessions WHERE workoutId = :workoutId ORDER BY startTime DESC")
    suspend fun getWorkoutSessionsByWorkoutId(workoutId: Int): List<SessionWorkoutEntity>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWorkoutSession(sessionId: Long): SessionWorkoutEntity?

    @Query("SELECT COUNT(DISTINCT sessionId) FROM workout_sessions")
    suspend fun getTotalWorkoutCount(): Int

    @Query("SELECT startTime FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getWorkoutDates(): List<Long>

    @Query("SELECT MAX(weight) FROM exercise_sessions WHERE exerciseId IN (SELECT id FROM exercises WHERE name = :exerciseName)")
    suspend fun getMaxWeightForExercise(exerciseName: String): Float

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllWorkoutSessionsOrderedByDate(): List<SessionWorkoutEntity>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionWorkoutById(sessionId: Long): SessionWorkoutEntity?

    @Query("SELECT * FROM workout_exercise_cross_ref WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun getWorkoutExerciseCrossRef(workoutId: Int, exerciseId: Int): CrossRefWorkoutExercise?
}