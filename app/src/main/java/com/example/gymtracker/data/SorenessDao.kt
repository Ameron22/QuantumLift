package com.example.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for soreness assessment and workout context data
 */
@Dao
interface SorenessDao {
    
    // Soreness Assessment methods
    @Insert
    suspend fun insertSorenessAssessment(assessment: SorenessAssessment): Long
    
    @Update
    suspend fun updateSorenessAssessment(assessment: SorenessAssessment)
    
    @Delete
    suspend fun deleteSorenessAssessment(assessment: SorenessAssessment)
    
    @Query("SELECT * FROM soreness_assessments WHERE sessionId = :sessionId")
    suspend fun getAssessmentsForSession(sessionId: Long): List<SorenessAssessment>
    
    @Query("SELECT * FROM soreness_assessments WHERE exerciseId = :exerciseId")
    suspend fun getAssessmentsForExercise(exerciseId: Int): List<SorenessAssessment>
    
    @Query("SELECT * FROM soreness_assessments WHERE assessmentDay = :day")
    suspend fun getAssessmentsByDay(day: Int): List<SorenessAssessment>
    
    @Query("SELECT * FROM soreness_assessments WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getAssessmentsInRange(startTime: Long, endTime: Long): List<SorenessAssessment>
    
    @Query("SELECT * FROM soreness_assessments ORDER BY timestamp DESC")
    fun getAllAssessments(): Flow<List<SorenessAssessment>>
    
    // Workout Context methods (temporarily commented out for debugging)
    // @Insert
    // suspend fun insertWorkoutContext(context: WorkoutContext): Long
    
    // @Update
    // suspend fun updateWorkoutContext(context: WorkoutContext)
    
    // @Delete
    // suspend fun deleteWorkoutContext(context: WorkoutContext)
    
    // @Query("SELECT * FROM workout_context WHERE sessionId = :sessionId")
    // suspend fun getWorkoutContext(sessionId: Long): WorkoutContext?
    
    // @Query("SELECT * FROM workout_context WHERE exerciseId = :exerciseId")
    // suspend fun getWorkoutContextsForExercise(exerciseId: Int): List<WorkoutContext>
    
    // @Query("SELECT * FROM workout_context ORDER BY timestamp DESC")
    // fun getAllWorkoutContexts(): Flow<List<WorkoutContext>>
    
    // Combined queries for ML training data (temporarily commented out)
    // @Transaction
    // @Query("SELECT * FROM soreness_assessments sa JOIN workout_context wc ON sa.sessionId = wc.sessionId AND sa.exerciseId = wc.exerciseId")
    // suspend fun getTrainingData(): List<SorenessTrainingData>
    
    @Query("SELECT COUNT(*) FROM soreness_assessments")
    suspend fun getAssessmentCount(): Int
    
    // @Query("SELECT COUNT(*) FROM workout_context")
    // suspend fun getWorkoutContextCount(): Int
    
    // Delete old data (for cleanup)
    @Query("DELETE FROM soreness_assessments WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAssessments(cutoffTime: Long)
    
    // @Query("DELETE FROM workout_context WHERE timestamp < :cutoffTime")
    // suspend fun deleteOldWorkoutContexts(cutoffTime: Long)
}

