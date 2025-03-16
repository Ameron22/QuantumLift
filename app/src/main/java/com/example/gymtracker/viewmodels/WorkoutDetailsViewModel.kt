package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryFactorsState(
    val sleepQuality: Int = 0,
    val proteinIntake: Int = 0,
    val hydration: Int = 0,
    val stressLevel: Int = 0
) {
    fun isAnyValueSet(): Boolean {
        return sleepQuality > 0 || proteinIntake > 0 || hydration > 0 || stressLevel > 0
    }
}

data class WorkoutSessionState(
    val sessionId: Long = System.currentTimeMillis(),
    val startTime: Long = System.currentTimeMillis(),
    val workoutId: Int = 0,
    val workoutName: String = "",
    val isStarted: Boolean = false,
    val completedExercises: MutableSet<Int> = mutableSetOf()
)

class WorkoutDetailsViewModel : ViewModel() {
    private val _recoveryFactors = MutableStateFlow(RecoveryFactorsState())
    val recoveryFactors: StateFlow<RecoveryFactorsState> = _recoveryFactors.asStateFlow()

    private val _hasSetRecoveryFactors = MutableStateFlow(false)
    val hasSetRecoveryFactors: StateFlow<Boolean> = _hasSetRecoveryFactors.asStateFlow()

    private val _workoutSession = MutableStateFlow<WorkoutSessionState?>(null)
    val workoutSession: StateFlow<WorkoutSessionState?> = _workoutSession.asStateFlow()

    fun initializeWorkoutSession(workoutId: Int, workoutName: String) {
        val currentTime = System.currentTimeMillis()
        _workoutSession.value = WorkoutSessionState(
            sessionId = currentTime,
            startTime = currentTime,
            workoutId = workoutId,
            workoutName = workoutName
        )
        if (!_hasSetRecoveryFactors.value) {
            resetRecoveryFactors()
        }
    }

    fun startWorkoutSession() {
        val currentTime = System.currentTimeMillis()
        _workoutSession.value = _workoutSession.value?.copy(
            isStarted = true,
            startTime = currentTime
        )
    }

    fun markExerciseCompleted(exerciseId: Int) {
        _workoutSession.value?.let { session ->
            session.completedExercises.add(exerciseId)
            _workoutSession.value = session
        }
    }

    fun isExerciseCompleted(exerciseId: Int): Boolean {
        return _workoutSession.value?.completedExercises?.contains(exerciseId) == true
    }

    fun updateRecoveryFactors(
        sleepQuality: Int = _recoveryFactors.value.sleepQuality,
        proteinIntake: Int = _recoveryFactors.value.proteinIntake,
        hydration: Int = _recoveryFactors.value.hydration,
        stressLevel: Int = _recoveryFactors.value.stressLevel
    ) {
        viewModelScope.launch {
            val newState = RecoveryFactorsState(
                sleepQuality = sleepQuality,
                proteinIntake = proteinIntake,
                hydration = hydration,
                stressLevel = stressLevel
            )
            _recoveryFactors.value = newState
            _hasSetRecoveryFactors.value = newState.isAnyValueSet()
        }
    }

    fun resetRecoveryFactors() {
        viewModelScope.launch {
            _recoveryFactors.value = RecoveryFactorsState()
            _hasSetRecoveryFactors.value = false
        }
    }

    fun resetWorkoutSession() {
        _workoutSession.value = null
        resetRecoveryFactors()
    }
} 