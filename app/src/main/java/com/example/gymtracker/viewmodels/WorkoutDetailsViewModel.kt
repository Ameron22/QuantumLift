package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import com.example.gymtracker.data.TempRecoveryFactors
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
        _completedExercises.value = _completedExercises.value + exerciseId
    }

    fun isExerciseCompleted(exerciseId: Int): Boolean {
        return _completedExercises.value.contains(exerciseId)
    }

    fun resetWorkoutSession() {
        Log.d("WorkoutViewModel", "Resetting workout session")
        _workoutSession.value = null
        _recoveryFactors.value = TempRecoveryFactors()
        _hasSetRecoveryFactors.value = false
        _completedExercises.value = emptySet()
    }
} 