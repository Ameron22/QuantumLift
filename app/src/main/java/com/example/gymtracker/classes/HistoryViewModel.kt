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

    // Add a StateFlow for exercise sessions to access muscle parts data
    private val _exerciseSessions = MutableStateFlow<List<SessionEntityExercise>>(emptyList())
    val exerciseSessions: StateFlow<List<SessionEntityExercise>> get() = _exerciseSessions

    // Add a StateFlow for muscle soreness
    private val _muscleSoreness = MutableStateFlow<Map<String, MuscleSorenessData>>(emptyMap())
    val muscleSoreness: StateFlow<Map<String, MuscleSorenessData>> get() = _muscleSoreness

    // Add a StateFlow for weight progression
    private val _weightProgression = MutableStateFlow<Map<String, List<WeightProgressionData>>>(emptyMap())
    val weightProgression: StateFlow<Map<String, List<WeightProgressionData>>> get() = _weightProgression

    // Add a StateFlow for volume progression
    private val _volumeProgression = MutableStateFlow<Map<String, List<VolumeProgressionData>>>(emptyMap())
    val volumeProgression: StateFlow<Map<String, List<VolumeProgressionData>>> get() = _volumeProgression

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
                            Log.d("HistoryViewModel", "Processing workout session: ID=${workoutSession.sessionId}, Name='${workoutSession.workoutName}', Start=${workoutSession.startTime}, End=${workoutSession.endTime}, Duration=${(workoutSession.endTime - workoutSession.startTime) / (60 * 1000)} min")
                            sessionMap[workoutSession.sessionId] = SessionWorkoutWithMuscles(
                                sessionId = workoutSession.sessionId,
                                workoutId = workoutSession.workoutId,
                                startTime = workoutSession.startTime,
                                endTime = workoutSession.endTime,
                                workoutName = workoutSession.workoutName,
                                muscleGroups = mutableMapOf()
                            )
                        }

                        // Then process exercise sessions to add muscle data
                        dao.getAllExerciseSessions().collect { exerciseSessions ->
                            Log.d("HistoryViewModel", "Processing ${exerciseSessions.size} exercise sessions")
                            
                            // Store exercise sessions for muscle parts analysis
                            _exerciseSessions.value = exerciseSessions
                            
                            for (session in exerciseSessions) {
                                val sessionId = session.sessionId
                                
                                // Get the existing workout session
                                val existingSession = sessionMap[sessionId] ?: continue

                                Log.d("HistoryViewModel", "Processing workout: ${existingSession.workoutName}, ID: $sessionId")

                                // Calculate muscle stress using the new formula
                                val muscleStress = calculateMuscleStress(session)
                                
                                // Parse individual muscle parts from the session
                                val muscleParts = session.muscleParts.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
                                
                                // If no specific muscle parts are defined, fall back to muscle group
                                val partsToProcess = if (muscleParts.isNotEmpty()) muscleParts else listOf(session.muscleGroup)
                                
                                // Update muscle data for each individual muscle part
                                for (musclePart in partsToProcess) {
                                    // Normalize muscle part names to handle capitalization inconsistencies
                                    val normalizedMusclePart = normalizeMusclePartName(musclePart)
                                    val existingData = muscleDataMap[normalizedMusclePart] ?: MuscleData()
                                    existingData.totalStress += muscleStress / partsToProcess.size // Distribute stress evenly
                                    existingData.lastWorkoutTime = max(existingData.lastWorkoutTime, session.sessionId)
                                    existingData.exercises.add(session)
                                    muscleDataMap[normalizedMusclePart] = existingData
                                }

                                // Add muscle group stress data (keep for backward compatibility)
                                (existingSession.muscleGroups as MutableMap)[session.muscleGroup] =
                                    (existingSession.muscleGroups[session.muscleGroup] ?: 0) + muscleStress.toInt()
                            }

                            Log.d("HistoryViewModel", "Processed ${sessionMap.size} unique workout sessions")

                            // Update StateFlow with the new list
                            _workoutSessions.value = sessionMap.values.toList()
                                .sortedByDescending { it.startTime }
                            
                            // Calculate and update muscle soreness
                            _muscleSoreness.value = calculateMuscleSoreness(muscleDataMap)

                            // Calculate and update weight progression
                            _weightProgression.value = withContext(Dispatchers.IO) {
                                calculateWeightProgression(exerciseSessions)
                            }

                            // Calculate and update volume progression
                            _volumeProgression.value = withContext(Dispatchers.IO) {
                                calculateVolumeProgression(exerciseSessions)
                            }

                            Log.d("HistoryViewModel", "Updated workout sessions, muscle soreness, and weight progression")

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
     * Calculates muscle stress using either the complete or simplified formula based on user input
     * Complete formula: Stress = k * (Load * Volume * E) * (1 + N/10) * (1 - A/10)
     * Simplified formula: Stress = k * (Load * Volume)
     */
    private fun calculateMuscleStress(session: SessionEntityExercise): Float {
        val k = 0.15f // Increased sensitivity constant
        
        // Calculate average load and volume
        val avgLoad = session.weight.filterNotNull().average().toFloat()
        val totalVolume = session.repsOrTime.filterNotNull().sum()
        
        // Check if user provided detailed data (all values are non-zero)
        val hasDetailedData = session.eccentricFactor > 0f && 
                            session.noveltyFactor > 0 && 
                            session.adaptationLevel > 0 && 
                            session.rpe > 0 && 
                            session.subjectiveSoreness > 0

        return if (hasDetailedData) {
            // Complete formula with all factors
            val baseImpact = avgLoad * totalVolume
            val eccentricMultiplier = 1 + (session.eccentricFactor - 1.0f) / 2.0f // More sensitive to eccentric factor
            val noveltyMultiplier = 1 + (session.noveltyFactor - 5.0f) / 10.0f // More sensitive to novelty
            val adaptationMultiplier = 1 - (session.adaptationLevel - 5.0f) / 10.0f // More sensitive to adaptation
            val rpeMultiplier = 1 + (session.rpe - 5.0f) / 10.0f // More sensitive to RPE
            val sorenessMultiplier = 1 + (session.subjectiveSoreness - 5.0f) / 10.0f // More sensitive to soreness
            val recoveryMultiplier = calculateRecoveryMultiplier(session.recoveryFactors)
            
            k * baseImpact * 
            eccentricMultiplier * 
            noveltyMultiplier * 
            adaptationMultiplier * 
            rpeMultiplier * 
            sorenessMultiplier * 
            recoveryMultiplier
        } else {
            // Simplified formula with increased sensitivity
            k * avgLoad * totalVolume * 1.2f // Add 20% to account for missing detailed data
        }
    }

    /**
     * Calculates recovery multiplier based on various factors
     * Returns a value between 0.5 and 1.5
     * If no recovery factors are provided, returns 1.0
     */
    private fun calculateRecoveryMultiplier(factors: RecoveryFactors?): Float {
        if (factors == null) return 1.0f

        // Check if any recovery factors are provided
        val hasRecoveryData = factors.sleepQuality > 0 || 
                             factors.proteinIntake > 0 || 
                             factors.hydration > 0 || 
                             factors.stressLevel > 0

        if (!hasRecoveryData) return 1.0f

        // Calculate individual multipliers for each factor
        val sleepMultiplier = if (factors.sleepQuality > 0) {
            1.0f + (factors.sleepQuality - 5.0f) / 20.0f
        } else 1.0f

        val proteinMultiplier = if (factors.proteinIntake > 0) {
            1.0f + (factors.proteinIntake - 100.0f) / 200.0f
        } else 1.0f

        val hydrationMultiplier = if (factors.hydration > 0) {
            1.0f + (factors.hydration - 5.0f) / 20.0f
        } else 1.0f

        val stressMultiplier = if (factors.stressLevel > 0) {
            1.0f - (factors.stressLevel - 5.0f) / 20.0f
        } else 1.0f

        // Calculate the average of all provided factors
        val providedFactors = listOfNotNull(
            if (factors.sleepQuality > 0) sleepMultiplier else null,
            if (factors.proteinIntake > 0) proteinMultiplier else null,
            if (factors.hydration > 0) hydrationMultiplier else null,
            if (factors.stressLevel > 0) stressMultiplier else null
        )

        return if (providedFactors.isNotEmpty()) {
            providedFactors.average().toFloat()
        } else {
            1.0f
        }
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
     * Lowered thresholds for more sensitive soreness detection
     */
    private fun determineSorenessLevel(
        daysSinceLastWorkout: Long,
        totalStress: Float,
        avgRPE: Int,
        avgSubjectiveSoreness: Int
    ): String {
        // Lowered thresholds for more sensitive soreness detection
        return when {
            // Very Sore: Lowered thresholds for high stress, recent workout, high RPE and soreness
            daysSinceLastWorkout < 1 && totalStress > 15 && avgRPE > 5 && avgSubjectiveSoreness > 5 -> "Very Sore"
            daysSinceLastWorkout < 1 && totalStress > 10 && avgRPE > 6 -> "Very Sore"
            daysSinceLastWorkout < 1 && avgSubjectiveSoreness > 6 -> "Very Sore"
            
            // Sore: Lowered thresholds for moderate stress, recent workout, moderate RPE and soreness
            daysSinceLastWorkout < 2 && totalStress > 8 && avgRPE > 4 && avgSubjectiveSoreness > 3 -> "Sore"
            daysSinceLastWorkout < 2 && totalStress > 5 && avgRPE > 5 -> "Sore"
            daysSinceLastWorkout < 2 && avgSubjectiveSoreness > 4 -> "Sore"
            
            // Slightly Sore: Lowered thresholds for low stress, recent workout, or moderate indicators
            daysSinceLastWorkout < 3 && totalStress > 3 && avgRPE > 3 -> "Slightly Sore"
            daysSinceLastWorkout < 3 && avgSubjectiveSoreness > 2 -> "Slightly Sore"
            daysSinceLastWorkout < 1 && totalStress > 2 -> "Slightly Sore"
            
            // Fresh: No recent stress or low indicators
            else -> "Fresh"
        }
    }

    /**
     * Calculates weight progression for each exercise
     */
    private suspend fun calculateWeightProgression(exerciseSessions: List<SessionEntityExercise>): Map<String, List<WeightProgressionData>> {
        val progressionMap = mutableMapOf<String, MutableList<WeightProgressionData>>()
        
        // Group exercises by exercise ID first, then get names
        val exercisesById = exerciseSessions.groupBy { it.exerciseId }
        
        for ((exerciseId, sessions) in exercisesById) {
            // Get exercise name from DAO
            val exercise = dao.getExerciseById(exerciseId.toInt())
            val exerciseName = exercise?.name ?: "Unknown Exercise"
            
            val progressionList = mutableListOf<WeightProgressionData>()
            
            for (session in sessions.sortedBy { it.sessionId }) {
                val weights = session.weight.filterNotNull()
                if (weights.isNotEmpty()) {
                    val maxWeight = weights.maxOrNull()?.toFloat() ?: 0f
                    val avgWeight = weights.average().toFloat()
                    val totalVolume = session.repsOrTime.filterNotNull().sum()
                    
                    progressionList.add(
                        WeightProgressionData(
                            exerciseName = exerciseName,
                            date = session.sessionId,
                            maxWeight = maxWeight,
                            avgWeight = avgWeight,
                            totalVolume = totalVolume,
                            sessionId = session.sessionId
                        )
                    )
                }
            }
            
            if (progressionList.isNotEmpty()) {
                progressionMap[exerciseName] = progressionList
            }
        }
        
        return progressionMap
    }

    /**
     * Calculates volume progression for each exercise
     */
    private suspend fun calculateVolumeProgression(exerciseSessions: List<SessionEntityExercise>): Map<String, List<VolumeProgressionData>> {
        val progressionMap = mutableMapOf<String, MutableList<VolumeProgressionData>>()
        
        // Group exercises by exercise ID first, then get names
        val exercisesById = exerciseSessions.groupBy { it.exerciseId }
        
        for ((exerciseId, sessions) in exercisesById) {
            // Get exercise name and useTime from DAO
            val exercise = dao.getExerciseById(exerciseId.toInt())
            val exerciseName = exercise?.name ?: "Unknown Exercise"
            
            // Skip time-based exercises (like running) as volume calculation doesn't apply
            if (exercise?.useTime == true) {
                continue
            }
            
            val progressionList = mutableListOf<VolumeProgressionData>()
            
            for (session in sessions.sortedBy { it.sessionId }) {
                val reps = session.repsOrTime.filterNotNull()
                val weights = session.weight.filterNotNull()
                
                // Calculate total volume: sum of (reps × weight) for each set
                val totalVolume = reps.zip(weights) { rep, weight ->
                    rep * weight
                }.sum()
                
                progressionList.add(
                    VolumeProgressionData(
                        exerciseName = exerciseName,
                        muscleGroup = session.muscleGroup,
                        date = session.sessionId,
                        totalVolume = totalVolume,
                        sessionId = session.sessionId
                    )
                )
            }
            
            if (progressionList.isNotEmpty()) {
                progressionMap[exerciseName] = progressionList
            }
        }
        
        return progressionMap
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

    /**
     * Get all possible muscle groups for complete display
     */
    private fun getAllMuscleGroups(): List<String> {
        return listOf(
            "Arms", "Back", "Chest", "Core", "Legs", "Neck", "Shoulder"
        )
    }

    /**
     * Get all possible muscle parts for complete display
     */
    private fun getAllMuscleParts(): List<String> {
        return listOf(
            "Abs", "Adductors", "Biceps", "Calves", "Deltoids", 
            "Forearms", "Glutes", "Hamstrings", "Lats", "Lower Back", 
            "Neck", "Obliques", "Pectorals", "Quadriceps", 
            "Upper Back", "Upper Traps", "Triceps"
        )
    }
    
    /**
     * Normalize muscle part names to handle capitalization inconsistencies from CSV data
     */
    private fun normalizeMusclePartName(musclePart: String): String {
        return when (musclePart.lowercase()) {
            "upper traps" -> "Upper Traps"
            "upper back" -> "Upper Back"
            "lower back" -> "Lower Back"
            "latissimus dorsi" -> "Lats"
            "adductors" -> "Adductors"
            "adductor" -> "Adductors"
            "quadriceps" -> "Quadriceps"
            "quands" -> "Quadriceps"
            "forearms" -> "Forearms"
            "forearm" -> "Forearms"
            "calves" -> "Calves"
            "calf" -> "Calves"
            "deltoids" -> "Deltoids"
            "deltoid" -> "Deltoids"
            "abs" -> "Abs"
            "obliques" -> "Obliques"
            "pectorals" -> "Pectorals"
            "glutes" -> "Glutes"
            "biceps" -> "Biceps"
            "triceps" -> "Triceps"
            "hamstrings" -> "Hamstrings"
            "neck" -> "Neck"
            else -> musclePart // Return as-is if no normalization needed
        }
    }

    /**
     * Calculate complete muscle group frequency including zero counts
     */
    fun getCompleteMuscleGroupFrequency(workoutSessions: List<SessionWorkoutWithMuscles>): List<Pair<String, Int>> {
        val allMuscleGroups = getAllMuscleGroups()
        val muscleGroupFrequency = workoutSessions
            .flatMap { session -> session.muscleGroups.keys }
            .groupingBy { it }
            .eachCount()
        
        // Create complete list with all muscle groups, including those with 0 count
        return allMuscleGroups.map { muscleGroup ->
            muscleGroup to (muscleGroupFrequency[muscleGroup] ?: 0)
        }.sortedByDescending { it.second }
    }

    /**
     * Calculate complete muscle parts frequency including zero counts
     */
    fun getCompleteMusclePartsFrequency(exerciseSessions: List<SessionEntityExercise>): List<Pair<String, Int>> {
        val allMuscleParts = getAllMuscleParts()
        val musclePartsFrequency = exerciseSessions
            .flatMap { exercise -> 
                exercise.muscleParts.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
                    .map { normalizeMusclePartName(it) } // Normalize muscle part names
            }
            .groupingBy { it }
            .eachCount()
        
        // Create complete list with all muscle parts, including those with 0 count
        return allMuscleParts.map { musclePart ->
            musclePart to (musclePartsFrequency[musclePart] ?: 0)
        }.sortedByDescending { it.second }
    }
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

/**
 * Data class to store weight progression data for exercises
 */
data class WeightProgressionData(
    val exerciseName: String,
    val date: Long,
    val maxWeight: Float,
    val avgWeight: Float,
    val totalVolume: Int,
    val sessionId: Long
)

/**
 * Data class to store volume progression data for exercises
 */
data class VolumeProgressionData(
    val exerciseName: String,
    val muscleGroup: String,
    val date: Long,
    val totalVolume: Int,
    val sessionId: Long
)