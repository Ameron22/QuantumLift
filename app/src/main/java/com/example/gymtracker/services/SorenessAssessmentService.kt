package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to handle soreness assessment workflow
 * Coordinates between workout completion, data collection, and notifications
 */
class SorenessAssessmentService(
    private val context: Context
) {
    companion object {
        private const val TAG = "SorenessAssessmentService"
    }
    
    private val sorenessDao: SorenessDao
    private val exerciseDao: ExerciseDao
    private val notificationManager: SorenessNotificationManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        val database = AppDatabase.getDatabase(context)
        sorenessDao = database.sorenessDao()
        exerciseDao = database.exerciseDao()
        notificationManager = SorenessNotificationManager(context)
    }
    
    /**
     * Handles workout completion - schedules soreness assessments
     */
    suspend fun handleWorkoutCompletion(
        sessionId: Long,
        exercises: List<WorkoutExerciseWithDetails>
    ) {
        try {
            Log.d(TAG, "Handling workout completion for session $sessionId with ${exercises.size} exercises")
            
            // 1. Extract muscle groups from exercises
            val muscleGroups = extractMuscleGroups(exercises)
            Log.d(TAG, "Extracted muscle groups: $muscleGroups")
            
            // 2. Create workout context for each exercise
            exercises.forEach { exercise ->
                createWorkoutContext(sessionId, exercise)
            }
            
            // 3. Schedule soreness assessment notifications
            notificationManager.scheduleSorenessAssessment(sessionId, muscleGroups)
            
            Log.d(TAG, "Successfully handled workout completion for session $sessionId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling workout completion for session $sessionId", e)
        }
    }
    
    /**
     * Extracts unique muscle groups from exercises
     */
    private fun extractMuscleGroups(exercises: List<WorkoutExerciseWithDetails>): List<String> {
        return exercises.flatMap { exercise ->
            exercise.entityExercise.muscle.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct()
    }
    
    /**
     * Creates workout context data for ML training
     */
    private suspend fun createWorkoutContext(
        sessionId: Long,
        exercise: WorkoutExerciseWithDetails
    ) {
        try {
            // Calculate volume and intensity metrics
            val totalSets = exercise.workoutExercise.sets
            val totalReps = totalSets * (exercise.workoutExercise.reps ?: 0)
            val totalVolume = totalSets * (exercise.workoutExercise.reps ?: 0) * (exercise.workoutExercise.weight ?: 0)
            
            // Get days since last workout for this muscle group
            val daysSinceLastMuscleGroup = getDaysSinceLastMuscleGroup(exercise.entityExercise.muscle)
            
            // Create workout context
            val workoutContext = WorkoutContext(
                sessionId = sessionId,
                exerciseId = exercise.entityExercise.id,
                muscleGroups = exercise.entityExercise.muscle.split(",").map { it.trim() },
                totalSets = totalSets,
                totalReps = totalReps,
                totalVolume = totalVolume.toFloat(),
                avgWeight = (exercise.workoutExercise.weight ?: 0).toFloat(),
                maxWeight = (exercise.workoutExercise.weight ?: 0).toFloat(), // Simplified for now
                weightProgression = 0f, // TODO: Calculate vs previous workout
                eccentricFactor = 1.0f, // Default, will be updated from session data
                noveltyFactor = 5, // Default, will be updated from session data
                adaptationLevel = 5, // Default, will be updated from session data
                rpe = 5, // Default, will be updated from session data
                subjectiveSoreness = 5, // Default, will be updated from session data
                daysSinceLastWorkout = 1, // TODO: Calculate actual days
                daysSinceLastMuscleGroup = daysSinceLastMuscleGroup,
                totalWorkoutsThisWeek = 1, // TODO: Calculate actual count
                workoutDuration = 0L, // TODO: Get actual workout duration
                restBetweenSets = 0, // TODO: Get actual rest time
                // recoveryFactors = RecoveryFactors(7, 150, 7, 5), // Temporarily commented out
                timestamp = System.currentTimeMillis()
            )
            
            // Save to database (temporarily commented out)
            // sorenessDao.insertWorkoutContext(workoutContext)
            
            Log.d(TAG, "Created workout context for exercise ${exercise.entityExercise.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating workout context for exercise ${exercise.entityExercise.name}", e)
        }
    }
    
    /**
     * Gets days since last workout for specific muscle groups
     */
    private suspend fun getDaysSinceLastMuscleGroup(muscleGroups: String): Map<String, Int> {
        // TODO: Implement actual calculation based on workout history
        // For now, return default values
        return muscleGroups.split(",").associateWith { 1 }
    }
    
    /**
     * Updates workout context with session data
     */
    suspend fun updateWorkoutContextWithSessionData(
        sessionId: Long,
        sessionData: SessionEntityExercise
    ) {
        // Temporarily commented out for debugging
        // try {
        //     val workoutContext = sorenessDao.getWorkoutContext(sessionId)
        //     if (workoutContext != null) {
        //         val updatedContext = workoutContext.copy(
        //             eccentricFactor = sessionData.eccentricFactor,
        //             noveltyFactor = sessionData.noveltyFactor,
        //             adaptationLevel = sessionData.adaptationLevel,
        //             rpe = sessionData.rpe,
        //             subjectiveSoreness = sessionData.subjectiveSoreness,
        //             // recoveryFactors = sessionData.recoveryFactors // Temporarily commented out
        //         )
        //         
        //         sorenessDao.updateWorkoutContext(updatedContext)
        //         Log.d(TAG, "Updated workout context with session data for session $sessionId")
        //     }
        // } catch (e: Exception) {
        //     Log.e(TAG, "Error updating workout context with session data", e)
        // }
    }
    
    /**
     * Handles soreness assessment completion
     */
    suspend fun handleAssessmentCompletion(assessment: SorenessAssessment) {
        try {
            // Save assessment
            sorenessDao.insertSorenessAssessment(assessment)
            
            // Log for ML training
            Log.d(TAG, "Assessment completed: Overall soreness ${assessment.overallSoreness}/10 for ${assessment.muscleGroups}")
            
            // TODO: Update ML model with new data
            // TODO: Generate insights for user
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling assessment completion", e)
        }
    }
    
    /**
     * Gets soreness assessment data for analysis
     */
    suspend fun getSorenessData(): List<SorenessTrainingData> {
        return try {
            // sorenessDao.getTrainingData() // Temporarily commented out
            emptyList() // Return empty list for now
        } catch (e: Exception) {
            Log.e(TAG, "Error getting soreness training data", e)
            emptyList()
        }
    }
    
    /**
     * Gets assessment count for analytics
     */
    suspend fun getAssessmentCount(): Int {
        return try {
            sorenessDao.getAssessmentCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assessment count", e)
            0
        }
    }
    
    /**
     * Cancels soreness assessments for a session
     */
    fun cancelAssessments(sessionId: Long) {
        notificationManager.cancelNotifications(sessionId)
        Log.d(TAG, "Cancelled soreness assessments for session $sessionId")
    }
}
