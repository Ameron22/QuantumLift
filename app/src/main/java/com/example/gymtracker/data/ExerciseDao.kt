package com.example.gymtracker.data

import androidx.room.*
import com.example.gymtracker.classes.SessionWorkoutWithMusclesStress
import kotlinx.coroutines.flow.Flow

// Data class to combine exercise info with workout-specific data
data class ExerciseWithWorkoutData(
    val exercise: EntityExercise,
    val workoutExercise: WorkoutExercise
)

@Dao
interface ExerciseDao {
    // Add this method to check if the database is empty
    @Query("SELECT COUNT(*) FROM EntityWorkout")
    suspend fun getWorkoutCount(): Int

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: EntityExercise): Long

    @Update
    suspend fun updateExercise(exercise: EntityExercise)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<EntityExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: EntityWorkout): Long

    @Update
    suspend fun updateWorkout(workout: EntityWorkout)

    @Delete
    suspend fun deleteWorkout(workout: EntityWorkout)

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

    // --- WorkoutExercise methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    suspend fun getWorkoutExercisesForWorkout(workoutId: Int): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExerciseById(id: Int): WorkoutExercise?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun getWorkoutExercise(workoutId: Int, exerciseId: Int): WorkoutExercise?

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercisesForWorkout(workoutId: Int)

    @Query("SELECT MAX(`order`) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getMaxExerciseOrder(workoutId: Int): Int?

    @Query("UPDATE workout_exercises SET `order` = :newOrder WHERE id = :workoutExerciseId")
    suspend fun updateWorkoutExerciseOrder(workoutExerciseId: Int, newOrder: Int)

    // New method to get exercises with their workout-specific data
    @Transaction
    suspend fun getExercisesWithWorkoutData(workoutId: Int): List<ExerciseWithWorkoutData> {
        val workoutExercises = getWorkoutExercisesForWorkout(workoutId)
        val exercises = getAllExercises()
        
        return workoutExercises.mapNotNull { we ->
            val exercise = exercises.find { it.id == we.exerciseId }
            exercise?.let { ExerciseWithWorkoutData(it, we) }
        }
    }

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutSessionById(sessionId: Long)
    
    @Query("DELETE FROM exercise_sessions WHERE sessionId = :sessionId")
    suspend fun deleteExerciseSessionsBySessionId(sessionId: Long)
    
    @Query("SELECT * FROM exercise_sessions WHERE sessionId = :sessionId")
    suspend fun getExerciseSessionsForSession(sessionId: Long): List<SessionEntityExercise>
}