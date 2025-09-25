package com.example.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WarmUpDao {
    
    // WarmUpTemplate operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarmUpTemplate(template: WarmUpTemplate): Long
    
    @Update
    suspend fun updateWarmUpTemplate(template: WarmUpTemplate)
    
    @Delete
    suspend fun deleteWarmUpTemplate(template: WarmUpTemplate)
    
    @Query("SELECT * FROM warm_up_templates ORDER BY name ASC")
    fun getAllWarmUpTemplates(): Flow<List<WarmUpTemplate>>
    
    @Query("SELECT * FROM warm_up_templates WHERE isDefault = 1 ORDER BY name ASC")
    fun getDefaultWarmUpTemplates(): Flow<List<WarmUpTemplate>>
    
    @Query("SELECT * FROM warm_up_templates WHERE createdBy = :userId ORDER BY name ASC")
    fun getUserWarmUpTemplates(userId: String): Flow<List<WarmUpTemplate>>
    
    @Query("SELECT * FROM warm_up_templates WHERE id = :templateId")
    suspend fun getWarmUpTemplateById(templateId: Int): WarmUpTemplate?
    
    @Query("SELECT * FROM warm_up_templates WHERE category = :category ORDER BY name ASC")
    fun getWarmUpTemplatesByCategory(category: String): Flow<List<WarmUpTemplate>>
    
    @Query("SELECT * FROM warm_up_templates WHERE targetMuscleGroups LIKE '%' || :muscleGroup || '%' ORDER BY name ASC")
    fun getWarmUpTemplatesByMuscleGroup(muscleGroup: String): Flow<List<WarmUpTemplate>>
    
    // WarmUpExercise operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarmUpExercise(exercise: WarmUpExercise): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarmUpExercises(exercises: List<WarmUpExercise>)
    
    @Update
    suspend fun updateWarmUpExercise(exercise: WarmUpExercise)
    
    @Delete
    suspend fun deleteWarmUpExercise(exercise: WarmUpExercise)
    
    @Query("SELECT * FROM warm_up_exercises WHERE templateId = :templateId ORDER BY `order` ASC")
    suspend fun getWarmUpExercisesByTemplate(templateId: Int): List<WarmUpExercise>
    
    @Query("DELETE FROM warm_up_exercises WHERE templateId = :templateId")
    suspend fun deleteWarmUpExercisesByTemplate(templateId: Int)
    
    // WorkoutWarmUp operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutWarmUp(workoutWarmUp: WorkoutWarmUp)
    
    @Update
    suspend fun updateWorkoutWarmUp(workoutWarmUp: WorkoutWarmUp)
    
    @Delete
    suspend fun deleteWorkoutWarmUp(workoutWarmUp: WorkoutWarmUp)
    
    @Query("SELECT * FROM workout_warm_ups WHERE workoutId = :workoutId")
    suspend fun getWorkoutWarmUp(workoutId: Int): WorkoutWarmUp?
    
    @Query("DELETE FROM workout_warm_ups WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutWarmUpByWorkout(workoutId: Int)
    
    // Complex queries
    @Transaction
    @Query("SELECT * FROM warm_up_templates WHERE id = :templateId")
    suspend fun getWarmUpTemplateWithExercises(templateId: Int): WarmUpTemplateWithExercises?
    
    @Transaction
    @Query("SELECT * FROM warm_up_templates ORDER BY name ASC")
    fun getAllWarmUpTemplatesWithExercises(): Flow<List<WarmUpTemplateWithExercises>>
    
    @Transaction
    @Query("SELECT * FROM warm_up_templates WHERE isDefault = 1 ORDER BY name ASC")
    fun getDefaultWarmUpTemplatesWithExercises(): Flow<List<WarmUpTemplateWithExercises>>
}

