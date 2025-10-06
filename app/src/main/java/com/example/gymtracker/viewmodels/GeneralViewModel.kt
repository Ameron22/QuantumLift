package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

data class CurrentWorkoutState(
    val workoutId: Int = 0,
    val workoutName: String = "",
    val sessionId: Long = 0,
    val isActive: Boolean = false,
    val startTime: Long = 0,
    val completedExercises: Set<Int> = emptySet(),
    val completedExercisesOrder: List<Int> = emptyList() // Track completion order
)

// XP Buffer data class to store newly earned XP
data class XPBuffer(
    val xpGained: Int,
    val previousLevel: Int,
    val newLevel: Int,
    val previousTotalXP: Int,
    val newTotalXP: Int,
    val timestamp: Long
)

class GeneralViewModel : ViewModel() {
    // Current workout management
    private val _currentWorkout = MutableStateFlow<CurrentWorkoutState?>(null)
    val currentWorkout: StateFlow<CurrentWorkoutState?> = _currentWorkout.asStateFlow()

    // XP Buffer for newly earned XP
    private val _xpBuffer = MutableStateFlow<XPBuffer?>(null)
    val xpBuffer: StateFlow<XPBuffer?> = _xpBuffer.asStateFlow()

    // General app state can be added here in the future
    // For example: user preferences, app settings, etc.

    // XP Buffer management methods
    fun setXPBuffer(xpBuffer: XPBuffer) {
        Log.d("GeneralViewModel", "Setting XP buffer: $xpBuffer")
        _xpBuffer.value = xpBuffer
    }

    fun clearXPBuffer() {
        Log.d("GeneralViewModel", "Clearing XP buffer")
        _xpBuffer.value = null
    }

    fun getXPBuffer(): XPBuffer? {
        return _xpBuffer.value
    }

    // Workout management methods
    fun startWorkout(workoutId: Int, workoutName: String) {
        val sessionId = System.currentTimeMillis()
        val startTime = System.currentTimeMillis()
        Log.d("GeneralViewModel", "Starting workout: $workoutId ($workoutName)")
        _currentWorkout.value = CurrentWorkoutState(
            workoutId = workoutId,
            workoutName = workoutName,
            sessionId = sessionId,
            isActive = true,
            startTime = startTime,
            completedExercises = emptySet(),
            completedExercisesOrder = emptyList()
        )
    }

    fun initializeWorkout(workoutId: Int, workoutName: String) {
        val sessionId = System.currentTimeMillis()
        Log.d("GeneralViewModel", "Initializing workout: $workoutId ($workoutName)")
        _currentWorkout.value = CurrentWorkoutState(
            workoutId = workoutId,
            workoutName = workoutName,
            sessionId = sessionId,
            isActive = false, // Not active until first exercise is started
            startTime = 0, // Will be set when workout actually starts
            completedExercises = emptySet(),
            completedExercisesOrder = emptyList()
        )
        Log.d("GeneralViewModel", "Workout initialized - new state: ${_currentWorkout.value}")
    }

    fun markExerciseAsCompleted(exerciseId: Int) {
        Log.d("GeneralViewModel", "markExerciseAsCompleted called with exerciseId: $exerciseId")
        val currentState = _currentWorkout.value
        if (currentState != null) {
            Log.d("GeneralViewModel", "Marking exercise $exerciseId as completed")
            val updatedCompletedExercises = currentState.completedExercises.toMutableSet()
            val updatedCompletedExercisesOrder = currentState.completedExercisesOrder.toMutableList()
            
            // Only add to order if not already completed
            if (!updatedCompletedExercises.contains(exerciseId)) {
                updatedCompletedExercises.add(exerciseId)
                updatedCompletedExercisesOrder.add(exerciseId)
            }
            
            _currentWorkout.value = currentState.copy(
                completedExercises = updatedCompletedExercises,
                completedExercisesOrder = updatedCompletedExercisesOrder
            )
            Log.d("GeneralViewModel", "Updated completed exercises: ${_currentWorkout.value?.completedExercises}")
            Log.d("GeneralViewModel", "Updated completion order: ${_currentWorkout.value?.completedExercisesOrder}")
        } else {
            Log.e("GeneralViewModel", "Cannot mark exercise as completed - no active workout")
        }
    }

    fun isExerciseCompleted(exerciseId: Int): Boolean {
        Log.d("GeneralViewModel", "isExerciseCompleted called with exerciseId: $exerciseId")
        val isCompleted = _currentWorkout.value?.completedExercises?.contains(exerciseId) ?: false
        Log.d("GeneralViewModel", "Checking if exercise $exerciseId is completed: $isCompleted")
        return isCompleted
    }

    fun getCompletedExercises(): Set<Int> {
        return _currentWorkout.value?.completedExercises ?: emptySet()
    }

    fun getCurrentWorkoutId(): Int {
        return _currentWorkout.value?.workoutId ?: 0
    }

    fun getCurrentSessionId(): Long {
        return _currentWorkout.value?.sessionId ?: 0
    }

    fun isWorkoutActive(): Boolean {
        return _currentWorkout.value?.isActive ?: false
    }

    fun getWorkoutName(): String {
        return _currentWorkout.value?.workoutName ?: ""
    }

    fun getStartTime(): Long {
        return _currentWorkout.value?.startTime ?: 0
    }

    fun endWorkout() {
        Log.d("GeneralViewModel", "Ending workout")
        _currentWorkout.value = null
    }

    fun activateWorkout() {
        val currentState = _currentWorkout.value
        Log.d("GeneralViewModel", "activateWorkout called - currentState: $currentState")
        if (currentState != null && !currentState.isActive) {
            Log.d("GeneralViewModel", "Activating workout with name: '${currentState.workoutName}'")
            _currentWorkout.value = currentState.copy(
                isActive = true,
                startTime = System.currentTimeMillis()
            )
            Log.d("GeneralViewModel", "Workout activated - new state: ${_currentWorkout.value}")
        } else {
            Log.d("GeneralViewModel", "Cannot activate workout - currentState is null or already active")
        }
    }

    fun updateWorkoutName(workoutName: String) {
        val currentState = _currentWorkout.value
        if (currentState != null) {
            Log.d("GeneralViewModel", "Updating workout name to: $workoutName")
            _currentWorkout.value = currentState.copy(
                workoutName = workoutName
            )
        }
    }

    fun clearCompletedExercises() {
        val currentState = _currentWorkout.value
        if (currentState != null) {
            Log.d("GeneralViewModel", "Clearing completed exercises")
            _currentWorkout.value = currentState.copy(
                completedExercises = emptySet(),
            completedExercisesOrder = emptyList()
            )
        }
    }

    fun hasActiveWorkout(): Boolean {
        return _currentWorkout.value != null
    }

    suspend fun initializeWorkoutWithName(workoutId: Int, context: android.content.Context) {
        val sessionId = System.currentTimeMillis()
        Log.d("GeneralViewModel", "Initializing workout with name lookup: $workoutId")
        
        // Get workout name from database
        val workoutName = try {
            val dao = com.example.gymtracker.data.AppDatabase.getDatabase(context).exerciseDao()
            val workouts = dao.getAllWorkouts()
            val workout = workouts.find { it.id == workoutId }
            val name = workout?.name ?: "Unknown Workout"
            Log.d("GeneralViewModel", "Found workout name: '$name' for workoutId: $workoutId")
            name
        } catch (e: Exception) {
            Log.e("GeneralViewModel", "Error getting workout name: ${e.message}")
            "Unknown Workout"
        }
        
        _currentWorkout.value = CurrentWorkoutState(
            workoutId = workoutId,
            workoutName = workoutName,
            sessionId = sessionId,
            isActive = false, // Not active until first exercise is started
            startTime = 0, // Will be set when workout actually starts
            completedExercises = emptySet(),
            completedExercisesOrder = emptyList()
        )
        Log.d("GeneralViewModel", "Workout initialized with name - new state: ${_currentWorkout.value}")
    }

    // General app methods can be added here
    // For example:
    // fun setUserPreference(key: String, value: Any) { ... }
    // fun getUserPreference(key: String): Any? { ... }
    // fun setAppTheme(theme: String) { ... }
    // etc.
} 