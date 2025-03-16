package com.example.gymtracker.classes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.RecoveryFactors
import com.example.gymtracker.data.SessionEntityExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.max

class HistoryViewModel(private val dao: ExerciseDao) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _workoutSessions = MutableStateFlow<List<SessionWorkoutWithMuscles>>(emptyList())
    val workoutSessions: StateFlow<List<SessionWorkoutWithMuscles>> get() = _workoutSessions

    // Add a StateFlow for muscle soreness
    private val _muscleSoreness = MutableStateFlow<Map<String, MuscleSorenessData>>(emptyMap())
    val muscleSoreness: StateFlow<Map<String, MuscleSorenessData>> get() = _muscleSoreness

    init {
        loadWorkoutSessions()
    }

    private fun loadWorkoutSessions() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    Log.d("HistoryViewModel", "Starting to load workout sessions")
                    
                    // First, collect all workout sessions
                    dao.getAllWorkoutSessionsFlow().collectLatest { workoutSessions ->
                        Log.d("HistoryViewModel", "Loaded ${workoutSessions.size} workout sessions")
                        
                        // Create a map to store aggregated results
                        val sessionMap = mutableMapOf<Long, SessionWorkoutWithMuscles>()
                        val muscleDataMap = mutableMapOf<String, MuscleData>()

                        // First, add all workout sessions to the map
                        workoutSessions.forEach { workoutSession ->
                            sessionMap[workoutSession.sessionId] = SessionWorkoutWithMuscles(
                                sessionId = workoutSession.sessionId,
                                workoutId = workoutSession.workoutId,
                                startTime = workoutSession.startTime,
                                duration = workoutSession.duration,
                                workoutName = workoutSession.workoutName,
                                muscleGroups = mutableMapOf()
                            )
                        }

                        // Then process exercise sessions to add muscle data
                        dao.getAllExerciseSessions().collect { exerciseSessions ->
                            Log.d("HistoryViewModel", "Processing ${exerciseSessions.size} exercise sessions")
                            
                            for (session in exerciseSessions) {
                                val sessionId = session.sessionId
                                
                                // Get the existing workout session
                                val existingSession = sessionMap[sessionId] ?: continue

                                Log.d("HistoryViewModel", "Processing workout: ${existingSession.workoutName}, ID: $sessionId")

                                // Calculate muscle stress using the new formula
                                val muscleStress = calculateMuscleStress(session)
                                
                                // Update muscle data
                                val existingData = muscleDataMap[session.muscleGroup] ?: MuscleData()
                                existingData.totalStress += muscleStress
                                existingData.lastWorkoutTime = max(existingData.lastWorkoutTime, session.sessionId)
                                existingData.exercises.add(session)
                                muscleDataMap[session.muscleGroup] = existingData

                                // Add muscle group stress data
                                (existingSession.muscleGroups as MutableMap)[session.muscleGroup] =
                                    (existingSession.muscleGroups[session.muscleGroup] ?: 0) + muscleStress.toInt()
                            }

                            Log.d("HistoryViewModel", "Processed ${sessionMap.size} unique workout sessions")

                            // Update StateFlow with the new list
                            _workoutSessions.value = sessionMap.values.toList()
                                .sortedByDescending { it.startTime }
                            
                            // Calculate and update muscle soreness
                            _muscleSoreness.value = calculateMuscleSoreness(muscleDataMap)

                            Log.d("HistoryViewModel", "Updated workout sessions and muscle soreness")

                            // Start a parallel coroutine for the minimum display time
                            launch {
                                delay(1500L)
                                _isLoading.value = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HistoryViewModel", "Error loading workout sessions: ${e.message}")
                    e.printStackTrace()
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Calculates muscle stress using the formula:
     * Stress = k * (Load * Volume * E) * (1 + N/10) * (1 - A/10)
     * Where:
     * k = Individual sensitivity constant (0.1)
     * Load = Average weight used
     * Volume = Total reps (sets × reps)
     * E = Eccentric factor
     * N = Novelty factor (0-10)
     * A = Adaptation level (0-10)
     */
    private fun calculateMuscleStress(session: SessionEntityExercise): Float {
        val k = 0.1f // Individual sensitivity constant
        
        // Calculate average load and volume
        val avgLoad = session.weight.filterNotNull().average().toFloat()
        val totalVolume = session.repsOrTime.filterNotNull().sum()
        
        // Calculate base impact
        val baseImpact = avgLoad * totalVolume * session.eccentricFactor
        
        // Calculate multipliers
        val noveltyMultiplier = 1 + (session.noveltyFactor / 10f)
        val adaptationMultiplier = 1 - (session.adaptationLevel / 10f)
        val recoveryMultiplier = calculateRecoveryMultiplier(session.recoveryFactors)
        
        return k * baseImpact * noveltyMultiplier * adaptationMultiplier * recoveryMultiplier
    }

    /**
     * Calculates recovery multiplier based on various factors
     * Returns a value between 0.5 and 1.5
     */
    private fun calculateRecoveryMultiplier(factors: RecoveryFactors): Float {
        val sleepImpact = factors.sleepQuality / 10f
        val proteinImpact = min(factors.proteinIntake / 200f, 1f) // Assuming 200g is optimal
        val hydrationImpact = factors.hydration / 10f
        val stressImpact = 1 - (factors.stressLevel / 10f)

        return (sleepImpact + proteinImpact + hydrationImpact + stressImpact) / 4f
    }

    /**
     * Calculates muscle soreness for each muscle group
     */
    private fun calculateMuscleSoreness(muscleDataMap: Map<String, MuscleData>): Map<String, MuscleSorenessData> {
        val currentTime = System.currentTimeMillis()
        val sorenessMap = mutableMapOf<String, MuscleSorenessData>()

        for ((muscle, data) in muscleDataMap) {
            val daysSinceLastWorkout = (currentTime - data.lastWorkoutTime) / (1000 * 60 * 60 * 24)
            val totalStress = data.totalStress
            val recentExercises = data.exercises.filter { 
                (currentTime - it.sessionId) < (7 * 24 * 60 * 60 * 1000) // Last 7 days
            }

            // Calculate average RPE and subjective soreness
            val avgRPE = recentExercises.map { it.rpe }.average().toInt()
            val avgSubjectiveSoreness = recentExercises.map { it.subjectiveSoreness }.average().toInt()

            sorenessMap[muscle] = MuscleSorenessData(
                sorenessLevel = determineSorenessLevel(
                    daysSinceLastWorkout,
                    totalStress,
                    avgRPE,
                    avgSubjectiveSoreness
                ),
                totalStress = totalStress,
                lastWorkoutTime = data.lastWorkoutTime,
                averageRPE = avgRPE,
                averageSubjectiveSoreness = avgSubjectiveSoreness,
                recentExercises = recentExercises
            )
        }

        return sorenessMap
    }

    /**
     * Determines the soreness level based on various factors
     */
    private fun determineSorenessLevel(
        daysSinceLastWorkout: Long,
        totalStress: Float,
        avgRPE: Int,
        avgSubjectiveSoreness: Int
    ): String {
        return when {
            daysSinceLastWorkout < 1 && totalStress > 50 && avgRPE > 7 && avgSubjectiveSoreness > 7 -> "Very Sore"
            daysSinceLastWorkout < 2 && totalStress > 30 && avgRPE > 6 && avgSubjectiveSoreness > 5 -> "Sore"
            daysSinceLastWorkout < 3 && totalStress > 20 && avgRPE > 5 && avgSubjectiveSoreness > 3 -> "Slightly Sore"
            else -> "Fresh"
        }
    }

    /** Do tomorow: It's probably something wrong in the logic, track What Exercise is
    fun generateProgressData(): ProgressData {
        val sessions = _workoutSessions.value

        // Duration over time: each session's start time and duration
        val durationOverTime = sessions.map { it.startTime to it.duration }

        // Flatten all exercises from sessions
       /// val allExercises: List<Exercise> = sessions.flatMap { it.exercises }

        // Group exercises by exerciseId and sum completed reps/time
        val setsRepsByExercise = allExercises
            .groupBy { workoutName }
            .mapValues { entry ->
                entry.value.sumOf { ex: Exercise -> ex.completedRepsOrTime }
            }

        // Group exercises by muscleGroup and count them
        val muscleActivation = allExercises
            .groupBy { it.muscleGroup }
            .mapValues { entry ->
                entry.value.size
            }

        return ProgressData(
            durationOverTime = durationOverTime,
            setsRepsByExercise = setsRepsByExercise,
            muscleActivation = muscleActivation
        )
    }*/

}

/**
 * Data class to store muscle-specific data for soreness calculation
 */
data class MuscleData(
    var totalStress: Float = 0f,
    var lastWorkoutTime: Long = 0L,
    val exercises: MutableList<SessionEntityExercise> = mutableListOf()
)

/**
 * Data class to store muscle soreness information
 */
data class MuscleSorenessData(
    val sorenessLevel: String,
    val totalStress: Float,
    val lastWorkoutTime: Long,
    val averageRPE: Int,
    val averageSubjectiveSoreness: Int,
    val recentExercises: List<SessionEntityExercise>
)