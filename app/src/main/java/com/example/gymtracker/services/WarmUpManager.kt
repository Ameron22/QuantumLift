package com.example.gymtracker.services

import com.example.gymtracker.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WarmUpManager(private val warmUpDao: WarmUpDao) {
    
    // Get all warm-up templates
    fun getAllWarmUpTemplates(): Flow<List<WarmUpTemplate>> {
        return warmUpDao.getAllWarmUpTemplates()
    }
    
    // Get default warm-up templates
    fun getDefaultWarmUpTemplates(): Flow<List<WarmUpTemplate>> {
        return warmUpDao.getDefaultWarmUpTemplates()
    }
    
    // Get user-created warm-up templates
    fun getUserWarmUpTemplates(userId: String): Flow<List<WarmUpTemplate>> {
        return warmUpDao.getUserWarmUpTemplates(userId)
    }
    
    // Get warm-up templates by category
    fun getWarmUpTemplatesByCategory(category: String): Flow<List<WarmUpTemplate>> {
        return warmUpDao.getWarmUpTemplatesByCategory(category)
    }
    
    // Get warm-up templates by muscle group
    fun getWarmUpTemplatesByMuscleGroup(muscleGroup: String): Flow<List<WarmUpTemplate>> {
        return warmUpDao.getWarmUpTemplatesByMuscleGroup(muscleGroup)
    }
    
    // Get warm-up template with exercises
    suspend fun getWarmUpTemplateWithExercises(templateId: Int): WarmUpTemplateWithExercises? {
        return warmUpDao.getWarmUpTemplateWithExercises(templateId)
    }
    
    // Create a new warm-up template
    suspend fun createWarmUpTemplate(
        name: String,
        description: String,
        category: String,
        targetMuscleGroups: List<String>,
        difficulty: String,
        estimatedDuration: Int,
        userId: String,
        exercises: List<WarmUpExercise>
    ): Long {
        val template = WarmUpTemplate(
            name = name,
            description = description,
            category = category,
            targetMuscleGroups = Converter().fromList(targetMuscleGroups),
            difficulty = difficulty,
            estimatedDuration = estimatedDuration,
            isDefault = false,
            createdBy = userId
        )
        
        val templateId = warmUpDao.insertWarmUpTemplate(template)
        
        // Insert exercises with correct template ID
        val exercisesWithTemplateId = exercises.map { exercise ->
            exercise.copy(templateId = templateId.toInt())
        }
        warmUpDao.insertWarmUpExercises(exercisesWithTemplateId)
        
        return templateId
    }
    
    // Update an existing warm-up template
    suspend fun updateWarmUpTemplate(
        template: WarmUpTemplate,
        exercises: List<WarmUpExercise>
    ) {
        warmUpDao.updateWarmUpTemplate(template)
        
        // Delete existing exercises and insert new ones
        warmUpDao.deleteWarmUpExercisesByTemplate(template.id)
        warmUpDao.insertWarmUpExercises(exercises)
    }
    
    // Delete a warm-up template
    suspend fun deleteWarmUpTemplate(template: WarmUpTemplate) {
        // Delete associated exercises first
        warmUpDao.deleteWarmUpExercisesByTemplate(template.id)
        
        // Delete the template
        warmUpDao.deleteWarmUpTemplate(template)
    }
    
    // Add warm-up to a workout
    suspend fun addWarmUpToWorkout(workoutId: Int, templateId: Int) {
        val workoutWarmUp = WorkoutWarmUp(
            workoutId = workoutId,
            templateId = templateId
        )
        warmUpDao.insertWorkoutWarmUp(workoutWarmUp)
    }
    
    // Remove warm-up from a workout
    suspend fun removeWarmUpFromWorkout(workoutId: Int) {
        warmUpDao.deleteWorkoutWarmUpByWorkout(workoutId)
    }
    
    // Get warm-up for a specific workout
    suspend fun getWorkoutWarmUp(workoutId: Int): WorkoutWarmUp? {
        return warmUpDao.getWorkoutWarmUp(workoutId)
    }
    
    // Get warm-up template for a workout
    suspend fun getWorkoutWarmUpTemplate(workoutId: Int): WarmUpTemplateWithExercises? {
        val workoutWarmUp = warmUpDao.getWorkoutWarmUp(workoutId)
        return workoutWarmUp?.let { warmUp ->
            warmUpDao.getWarmUpTemplateWithExercises(warmUp.templateId)
        }
    }
    
    // Check if a workout has a warm-up
    suspend fun workoutHasWarmUp(workoutId: Int): Boolean {
        return warmUpDao.getWorkoutWarmUp(workoutId) != null
    }
    
    // Get recommended warm-up templates based on workout muscle groups
    suspend fun getRecommendedWarmUpsForWorkout(
        workoutMuscleGroups: List<String>
    ): List<WarmUpTemplate> {
        val allTemplates = warmUpDao.getAllWarmUpTemplates().first()
        
        return allTemplates.filter { template ->
            val templateMuscleGroups = Converter().fromString(template.targetMuscleGroups)
            workoutMuscleGroups.any { muscleGroup ->
                templateMuscleGroups.contains(muscleGroup) || 
                templateMuscleGroups.contains("Full Body")
            }
        }.sortedBy { it.estimatedDuration }
    }
}

