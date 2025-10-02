package com.example.gymtracker.utils

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.ExerciseAlternative
import com.example.gymtracker.data.WorkoutExercise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MigrationTestRunner {
    private const val TAG = "MigrationTestRunner"
    
    fun testMigration47To48(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting migration test 47→48")
                
                // Get database instance
                val database = AppDatabase.getDatabase(context)
                val dao = database.exerciseDao()
                
                // Test 1: Verify hasAlternatives column exists and works
                Log.d(TAG, "Test 1: Testing hasAlternatives column")
                val testWorkout = com.example.gymtracker.data.EntityWorkout(
                    id = 0,
                    name = "Test Workout"
                )
                val workoutId = dao.insertWorkout(testWorkout).toInt()
                
                val testExercise = com.example.gymtracker.data.EntityExercise(
                    id = 0,
                    name = "Test Exercise",
                    description = "Test exercise for migration",
                    category = "Strength",
                    muscle = "Chest",
                    parts = "[\"Pectorals\"]",
                    equipment = "Barbell",
                    difficulty = "Intermediate"
                )
                val exerciseId = dao.insertExercise(testExercise).toInt()
                
                val testWorkoutExercise = WorkoutExercise(
                    id = 0,
                    exerciseId = exerciseId,
                    workoutId = workoutId,
                    sets = 3,
                    reps = 10,
                    weight = 100,
                    order = 0,
                    hasAlternatives = false
                )
                val workoutExerciseId = dao.insertWorkoutExercise(testWorkoutExercise).toInt()
                
                // Update hasAlternatives flag
                dao.updateWorkoutExerciseHasAlternatives(workoutExerciseId, true)
                
                // Verify the update worked
                val updatedWorkoutExercise = dao.getWorkoutExerciseById(workoutExerciseId)
                if (updatedWorkoutExercise?.hasAlternatives == true) {
                    Log.d(TAG, "✓ hasAlternatives column working correctly")
                } else {
                    Log.e(TAG, "✗ hasAlternatives column not working")
                }
                
                // Test 2: Verify exercise_alternatives table exists and works
                Log.d(TAG, "Test 2: Testing exercise_alternatives table")
                val alternativeExercise = com.example.gymtracker.data.EntityExercise(
                    id = 0,
                    name = "Alternative Exercise",
                    description = "Alternative test exercise",
                    category = "Strength",
                    muscle = "Chest",
                    parts = "[\"Pectorals\"]",
                    equipment = "Dumbbell",
                    difficulty = "Intermediate"
                )
                val alternativeExerciseId = dao.insertExercise(alternativeExercise).toInt()
                
                val testAlternative = ExerciseAlternative(
                    id = 0,
                    originalExerciseId = exerciseId,
                    alternativeExerciseId = alternativeExerciseId,
                    workoutExerciseId = workoutExerciseId,
                    order = 0,
                    isActive = false
                )
                val alternativeId = dao.insertExerciseAlternative(testAlternative).toInt()
                
                // Verify alternative was inserted
                val alternatives = dao.getExerciseAlternatives(workoutExerciseId)
                if (alternatives.isNotEmpty()) {
                    Log.d(TAG, "✓ exercise_alternatives table working correctly")
                } else {
                    Log.e(TAG, "✗ exercise_alternatives table not working")
                }
                
                // Test 3: Test alternative management functions
                Log.d(TAG, "Test 3: Testing alternative management functions")
                dao.deactivateAllAlternatives(workoutExerciseId)
                dao.activateAlternative(alternativeId.toInt())
                
                val activeAlternatives = dao.getExerciseAlternatives(workoutExerciseId)
                val activeAlternative = activeAlternatives.find { it.isActive }
                if (activeAlternative != null) {
                    Log.d(TAG, "✓ Alternative management functions working correctly")
                } else {
                    Log.e(TAG, "✗ Alternative management functions not working")
                }
                
                // Clean up test data
                dao.deleteExerciseAlternative(testAlternative)
                dao.deleteWorkoutExercise(testWorkoutExercise)
                dao.deleteExercise(testExercise)
                dao.deleteExercise(alternativeExercise)
                dao.deleteWorkout(testWorkout)
                
                Log.d(TAG, "✓ Migration test 47→48 completed successfully!")
                
            } catch (e: Exception) {
                Log.e(TAG, "Migration test failed", e)
            }
        }
    }
}
