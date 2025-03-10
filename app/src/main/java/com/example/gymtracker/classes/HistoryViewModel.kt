package com.example.gymtracker.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.classes.ProgressData
import com.example.gymtracker.data.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val dao: ExerciseDao) : ViewModel() {
    private val _workoutSessions = MutableStateFlow<List<WorkoutSessionWithMuscles>>(emptyList())
    val workoutSessions: StateFlow<List<WorkoutSessionWithMuscles>> get() = _workoutSessions

    init {
        loadWorkoutSessions()
    }

    private fun loadWorkoutSessions() {
        viewModelScope.launch (Dispatchers.IO){
            dao.getAllWorkoutSessionsWithMuscleStress()
                .collect { sessionsWithStress ->

                    // Create a map to store aggregated results
                    val sessionMap = mutableMapOf<Long, WorkoutSessionWithMuscles>()

                    for (session in sessionsWithStress) {
                        val sessionId = session.sessionId

                        // If the sessionId is not yet in the map, create a new entry
                        if (!sessionMap.containsKey(sessionId)) {
                            sessionMap[sessionId] = WorkoutSessionWithMuscles(
                                sessionId = sessionId,
                                workoutId = session.workoutId,
                                startTime = session.startTime,
                                duration = session.duration,
                                workoutName = session.workoutName,
                                muscleGroups = mutableMapOf() // Will be filled later
                            )
                        }

                        // Add muscle group stress data
                        val existingSession = sessionMap[sessionId]!!
                        (existingSession.muscleGroups as MutableMap)[session.muscleGroup] =
                            session.totalStress
                    }

                    // Update StateFlow with the new list
                    _workoutSessions.value = sessionMap.values.toList()
                }
        }
    }

    // Calculate muscle soreness dynamically
    fun calculateMuscleSoreness(): Map<String, String> {
        val sessions = _workoutSessions.value
        val sorenessMap = mutableMapOf<String, String>()
        val currentTime = System.currentTimeMillis()

        // Create a map to store the total stress and last workout time for each muscle group
        val muscleData = mutableMapOf<String, Pair<Int, Long>>() // Muscle group -> (Total stress, Last workout time)

        // Iterate through all sessions
        for (session in sessions) {
            val timeSinceWorkout = currentTime - session.startTime

            // Iterate through muscle groups in the session
            for ((muscle, stress) in session.muscleGroups) {
                // Update total stress and last workout time for the muscle group
                val (existingStress, existingTime) = muscleData[muscle] ?: (0 to 0L)
                muscleData[muscle] = (existingStress + stress) to maxOf(existingTime, session.startTime)
            }
        }

        // Determine soreness level for each muscle group
        for ((muscle, data) in muscleData) {
            val (totalStress, lastWorkoutTime) = data // Explicitly destructure the Pair
            val daysSinceLastWorkout = (currentTime - lastWorkoutTime) / (1000 * 60 * 60 * 24)

            sorenessMap[muscle] = when {
                daysSinceLastWorkout < 1 && totalStress > 50 -> "Very Sore"
                daysSinceLastWorkout < 3 && totalStress > 20 -> "Slightly Sore"
                else -> "Fresh"
            }
        }

        return sorenessMap
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