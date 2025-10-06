package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gymtracker.data.Converter

/**
 * Entity class to store workout context data for ML training
 * This captures all the factors that might influence soreness
 */
@Entity(tableName = "workout_context")
@TypeConverters(Converter::class)
data class WorkoutContext(
    @PrimaryKey(autoGenerate = true) val contextId: Long = 0,
    val sessionId: Long,
    val exerciseId: Int,
    val muscleGroups: List<String>,
    
    // Volume data
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Float, // sets × reps × weight
    
    // Intensity data
    val avgWeight: Float,
    val maxWeight: Float,
    val weightProgression: Float, // vs last workout
    
    // Workout factors (existing)
    val eccentricFactor: Float,
    val noveltyFactor: Int,
    val adaptationLevel: Int,
    val rpe: Int,
    val subjectiveSoreness: Int,
    
    // Context factors
    val daysSinceLastWorkout: Int,
    val daysSinceLastMuscleGroup: Map<String, Int>,
    val totalWorkoutsThisWeek: Int,
    val workoutDuration: Long, // in minutes
    val restBetweenSets: Int, // average rest time
    
    // Recovery factors (temporarily commented out for debugging)
    // val recoveryFactors: RecoveryFactors,
    val timestamp: Long
)
