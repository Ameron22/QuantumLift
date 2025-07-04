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
    val completedExercises: Set<Int> = emptySet()
)

class GeneralViewModel : ViewModel() {
    // Current workout management
    private val _currentWorkout = MutableStateFlow<CurrentWorkoutState?>(null)
    val currentWorkout: StateFlow<CurrentWorkoutState?> = _currentWorkout.asStateFlow()

    // General app state can be added here in the future
    // For example: user preferences, app settings, etc.

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
            completedExercises = emptySet()
        )
    }

    fun markExerciseAsCompleted(exerciseId: Int) {
        Log.d("GeneralViewModel", "markExerciseAsCompleted called with exerciseId: $exerciseId")
        val currentState = _currentWorkout.value
        if (currentState != null) {
            Log.d("GeneralViewModel", "Marking exercise $exerciseId as completed")
            val updatedCompletedExercises = currentState.completedExercises.toMutableSet()
            updatedCompletedExercises.add(exerciseId)
            _currentWorkout.value = currentState.copy(
                completedExercises = updatedCompletedExercises
            )
            Log.d("GeneralViewModel", "Updated completed exercises: ${_currentWorkout.value?.completedExercises}")
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

    fun clearCompletedExercises() {
        val currentState = _currentWorkout.value
        if (currentState != null) {
            Log.d("GeneralViewModel", "Clearing completed exercises")
            _currentWorkout.value = currentState.copy(
                completedExercises = emptySet()
            )
        }
    }

    // General app methods can be added here
    // For example:
    // fun setUserPreference(key: String, value: Any) { ... }
    // fun getUserPreference(key: String): Any? { ... }
    // fun setAppTheme(theme: String) { ... }
    // etc.
} 