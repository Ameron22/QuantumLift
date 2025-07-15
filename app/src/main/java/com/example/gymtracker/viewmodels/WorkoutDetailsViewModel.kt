package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import com.example.gymtracker.data.TempRecoveryFactors
import com.example.gymtracker.data.WorkoutExerciseWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

data class WorkoutSessionState(
    val sessionId: Long = System.currentTimeMillis(),
    val workoutId: Int = 0,
    val workoutName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val isStarted: Boolean = false
)

class WorkoutDetailsViewModel : ViewModel() {
    private val _workoutSession = MutableStateFlow<WorkoutSessionState?>(null)
    val workoutSession: StateFlow<WorkoutSessionState?> = _workoutSession.asStateFlow()

    private val _recoveryFactors = MutableStateFlow(TempRecoveryFactors())
    val recoveryFactors: StateFlow<TempRecoveryFactors> = _recoveryFactors.asStateFlow()

    private val _hasSetRecoveryFactors = MutableStateFlow(false)
    val hasSetRecoveryFactors: StateFlow<Boolean> = _hasSetRecoveryFactors.asStateFlow()

    private val _completedExercises = MutableStateFlow<Set<Int>>(emptySet())
    val completedExercises: StateFlow<Set<Int>> = _completedExercises.asStateFlow()

    // Break timer state
    private val _breakStartTime = MutableStateFlow(0L)
    val breakStartTime: StateFlow<Long> = _breakStartTime.asStateFlow()

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    // Exercise management for existing workouts
    private val _exercisesList = MutableStateFlow<List<WorkoutExerciseWithDetails>>(emptyList())
    val exercisesList: StateFlow<List<WorkoutExerciseWithDetails>> = _exercisesList.asStateFlow()

    fun initializeWorkoutSession(workoutId: Int, workoutName: String) {
        val currentTime = System.currentTimeMillis()
        Log.d("WorkoutViewModel", "Initializing workout session at time: $currentTime")
        _workoutSession.value = WorkoutSessionState(
            sessionId = currentTime,
            workoutId = workoutId,
            workoutName = workoutName,
            startTime = currentTime,
            isStarted = false
        )
        // Don't clear completed exercises here - they should persist during the workout
    }

    fun initializeWorkoutSessionAndClearCompleted(workoutId: Int, workoutName: String) {
        val currentTime = System.currentTimeMillis()
        Log.d("WorkoutViewModel", "Initializing workout session and clearing completed exercises at time: $currentTime")
        _workoutSession.value = WorkoutSessionState(
            sessionId = currentTime,
            workoutId = workoutId,
            workoutName = workoutName,
            startTime = currentTime,
            isStarted = false
        )
        _completedExercises.value = emptySet()
    }

    fun startWorkoutSession(startTime: Long) {
        Log.d("WorkoutViewModel", "Starting workout session at time: $startTime")
        val currentSession = _workoutSession.value
        if (currentSession != null) {
            _workoutSession.value = currentSession.copy(
                startTime = startTime,
                isStarted = true
            )
            Log.d("WorkoutViewModel", "Workout session started with state: ${_workoutSession.value}")
        } else {
            Log.e("WorkoutViewModel", "Cannot start session - no initialized session found")
        }
    }

    fun stopWorkoutSession() {
        val currentSession = _workoutSession.value
        if (currentSession != null) {
            _workoutSession.value = currentSession.copy(
                isStarted = false
            )
            Log.d("WorkoutViewModel", "Workout session stopped")
        }
    }

    fun updateRecoveryFactors(
        sleepQuality: Int? = null,
        proteinIntake: Int? = null,
        hydration: Int? = null,
        stressLevel: Int? = null
    ) {
        val currentFactors = _recoveryFactors.value
        _recoveryFactors.value = currentFactors.copy(
            sleepQuality = sleepQuality ?: currentFactors.sleepQuality,
            proteinIntake = proteinIntake ?: currentFactors.proteinIntake,
            hydration = hydration ?: currentFactors.hydration,
            stressLevel = stressLevel ?: currentFactors.stressLevel
        )
        _hasSetRecoveryFactors.value = true
    }

    fun markExerciseAsCompleted(exerciseId: Int) {
        Log.d("WorkoutViewModel", "Marking exercise $exerciseId as completed")
        val currentCompleted = _completedExercises.value.toMutableSet()
        currentCompleted.add(exerciseId)
        _completedExercises.value = currentCompleted
        Log.d("WorkoutViewModel", "Updated completed exercises: ${_completedExercises.value}")
    }

    fun isExerciseCompleted(exerciseId: Int): Boolean {
        val isCompleted = _completedExercises.value.contains(exerciseId)
        Log.d("WorkoutViewModel", "Checking if exercise $exerciseId is completed: $isCompleted (total completed: ${_completedExercises.value.size})")
        return isCompleted
    }

    fun resetWorkoutSession() {
        Log.d("WorkoutViewModel", "Resetting workout session")
        _workoutSession.value = null
        _recoveryFactors.value = TempRecoveryFactors()
        _hasSetRecoveryFactors.value = false
        // Don't clear completed exercises here - they should persist during the workout
    }

    fun resetWorkoutSessionAndClearCompleted() {
        Log.d("WorkoutViewModel", "Resetting workout session and clearing completed exercises")
        _workoutSession.value = null
        _recoveryFactors.value = TempRecoveryFactors()
        _hasSetRecoveryFactors.value = false
        _completedExercises.value = emptySet()
    }

    fun clearCompletedExercises() {
        Log.d("WorkoutViewModel", "Clearing completed exercises")
        _completedExercises.value = emptySet()
    }

    // Exercise management methods for existing workouts
    fun addExercise(exerciseWithDetails: WorkoutExerciseWithDetails) {
        Log.d("WorkoutDetailsViewModel", "Adding exercise: ${exerciseWithDetails.entityExercise.name}")
        Log.d("WorkoutDetailsViewModel", "Current list size: ${_exercisesList.value.size}")
        val newList = _exercisesList.value.toMutableList()
        newList.add(exerciseWithDetails)
        _exercisesList.value = newList
        Log.d("WorkoutDetailsViewModel", "New list size: ${_exercisesList.value.size}")
    }
/*  Update won't be used
    fun updateExercise(index: Int, exerciseWithDetails: WorkoutExerciseWithDetails) {
        Log.d("WorkoutDetailsViewModel", "Updating exercise at index $index: ${exerciseWithDetails.entityExercise.name}")
        val newList = _exercisesList.value.toMutableList()
        newList[index] = exerciseWithDetails
        _exercisesList.value = newList
        Log.d("WorkoutDetailsViewModel", "Updated list size: ${_exercisesList.value.size}")
    }
    */

    fun removeExercise(index: Int) {
        Log.d("WorkoutDetailsViewModel", "Removing exercise at index $index")
        val newList = _exercisesList.value.toMutableList()
        newList.removeAt(index)
        _exercisesList.value = newList
        Log.d("WorkoutDetailsViewModel", "New list size: ${_exercisesList.value.size}")
    }

    fun clearExercises() {
        Log.d("WorkoutDetailsViewModel", "Clearing exercises")
        _exercisesList.value = emptyList()
    }

    fun updateExercisesOrder(exercises: List<WorkoutExerciseWithDetails>) {
        Log.d("WorkoutDetailsViewModel", "Updating exercises order with ${exercises.size} exercises")
        _exercisesList.value = exercises
    }

    // Break timer functions
    fun startBreakTimer() {
        Log.d("WorkoutDetailsViewModel", "startBreakTimer called")
        _breakStartTime.value = System.currentTimeMillis()
        _isBreakActive.value = true
        Log.d("WorkoutDetailsViewModel", "Break timer started at: ${_breakStartTime.value}, isBreakActive: ${_isBreakActive.value}")
    }

    fun stopBreakTimer() {
        Log.d("WorkoutDetailsViewModel", "stopBreakTimer called")
        _isBreakActive.value = false
        _breakStartTime.value = 0L
        Log.d("WorkoutDetailsViewModel", "Break timer stopped - isBreakActive: ${_isBreakActive.value}")
    }

    fun calculateBreakDuration(): String {
        val duration = if (_isBreakActive.value && _breakStartTime.value > 0) {
            val currentDuration = System.currentTimeMillis() - _breakStartTime.value
            formatDuration(currentDuration)
        } else {
            "00:00"
        }
        Log.d("WorkoutDetailsViewModel", "calculateBreakDuration - isBreakActive: ${_isBreakActive.value}, breakStartTime: ${_breakStartTime.value}, duration: $duration")
        return duration
    }

    private fun formatDuration(durationInMillis: Long): String {
        val durationInSeconds = durationInMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
} 