package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.gymtracker.data.AppDatabase
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.ExerciseEntity
import com.example.gymtracker.data.ExerciseSessionEntity
import com.example.gymtracker.data.WorkoutEntity
import com.example.gymtracker.data.WorkoutSessionEntity
import com.example.gymtracker.data.WorkoutWithExercises
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class ExerciseState(
    val exercise: ExerciseEntity,
    var isCompleted: Boolean = false
)

data class WorkoutState(
    val workout: WorkoutEntity,
    val exercises: List<ExerciseState>,
    var isFinished: Boolean = false
)
@Composable
fun WorkoutDetailsScreen(workoutId: Int, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }
    // State for sessionId
    var sessionId by remember { mutableStateOf<Long?>(null) }

    var activeExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var remainingSets by remember { mutableIntStateOf(0) }
    var remainingTime by remember { mutableIntStateOf(0) }
    var exerciseTime by remember { mutableIntStateOf(0) }
    var breakTime by remember { mutableIntStateOf(10) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isBreakRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var firstStart by remember { mutableStateOf(true) }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutState by remember { mutableStateOf<WorkoutState?>(null) }

    // Function to start the timer for an exercise
    fun startTimer(exercise: ExerciseEntity) {
        activeExercise = exercise

        remainingSets = exercise.sets
        isTimerRunning = true
        isBreakRunning = false
        isPaused = false
        exerciseTime = if (exercise.reps > 50) {
            exercise.reps - 1000
        } else {
            10
        }
        remainingTime = exerciseTime
        if (firstStart) {
            firstStart = false
            startTimeWorkout = System.currentTimeMillis()
            val workoutSession = WorkoutSessionEntity(
                workoutId = workoutWithExercises?.firstOrNull()?.workout?.id ?: 0,
                startTime = System.currentTimeMillis(),
                duration = 0,
                workoutName = workoutWithExercises?.firstOrNull()?.workout?.name ?: ""
            )
            coroutineScope.launch(Dispatchers.IO) {
                sessionId = dao.insertWorkoutSession(workoutSession) // Insert and get the generated ID
                println("WorkoutSession inserted with ID: $sessionId")
                // Store this ID somewhere (e.g., in workoutState) to use in SaveExerciseSession
            }
            workoutState = WorkoutState(
                workout = workoutWithExercises?.firstOrNull()?.workout
                    ?: throw IllegalStateException("Workout not found"),
                exercises = workoutWithExercises?.flatMap { it.exercises }?.map { exercise ->
                    ExerciseState(exercise = exercise, isCompleted = false)
                } ?: emptyList()
            )
        }
    }

    // Function to save exercise session
    fun CoroutineScope.SaveExerciseSession() {
        println("Saving exercise session...")
        val exercise = activeExercise ?: return
        println("Still Saving exercise session...")
        val exerciseSession = ExerciseSessionEntity(
            sessionId = sessionId?:0,
            exerciseId = activeExercise?.id?.toLong() ?: 0,
            sets = activeExercise?.sets ?: 0,
            repsOrTime = if (activeExercise?.reps!! > 50) activeExercise?.reps!! - 1000 else activeExercise?.reps ?: 0,
            muscleGroup = activeExercise?.muscle ?: "",
            muscleParts = activeExercise?.part ?: emptyList(),
            completedSets = activeExercise?.sets?.minus(remainingSets) ?: 0,
            completedRepsOrTime = activeExercise?.reps ?: 0,
            notes = ""
        )
        // Print the data before insertion
        println("ExerciseSession to be inserted: $exerciseSession")

        launch(Dispatchers.IO) {
            dao.insertExerciseSession(exerciseSession)
            // Fetch and print all ExerciseSessionEntity objects
            val allSessions = dao.getAllExerciseSessions()
            println("All ExerciseSessions in the database: $allSessions")
        }
        // Update the exercise's isCompleted status in workoutState
        workoutState = workoutState?.let { state ->
            val updatedExercises = state.exercises.map { exerciseState ->
                if (exerciseState.exercise.id == exercise.id) {
                    exerciseState.copy(isCompleted = true) // Mark the exercise as completed
                } else {
                    exerciseState
                }
            }
            state.copy(exercises = updatedExercises) // Update the workout state with the modified exercises
        }
    }

    // Function to end the workout session
    fun CoroutineScope.EndWorkoutSession(sessionId: Long) {
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTimeWorkout) / 1000

        launch(Dispatchers.IO) {
            dao.updateWorkoutSessionDuration(sessionId, duration)
            println("Dao updateWorkoutSessionDuration called...")
        }
    }

    // Function to check if the workout is completed
    fun CoroutineScope.checkWorkoutCompletion() {
        println("Checking workout completion...")
        val allExercisesCompleted = workoutState!!.exercises.all { it.isCompleted }

        if (allExercisesCompleted) {
            println("All exercises completed...")
            workoutState!!.isFinished = true
            launch(Dispatchers.IO) {
                // Call EndWorkoutSession with the CoroutineScope
                EndWorkoutSession((workoutWithExercises?.firstOrNull()?.workout?.id ?: 0).toLong())
            }
        }
    }



    // Timer logic
    LaunchedEffect(isTimerRunning, isPaused) {
        while (isTimerRunning) {
            if (!isPaused) {
                if (remainingTime > 0) {
                    delay(1000)
                    remainingTime--
                } else {
                    if (isBreakRunning) {
                        isBreakRunning = false
                        if (remainingSets > 0) {
                            remainingTime = exerciseTime
                        } else {
                            isTimerRunning = false
                            coroutineScope.launch {
                                SaveExerciseSession()
                                checkWorkoutCompletion()
                                activeExercise = null
                            }
                            break
                        }
                    } else {
                        if (remainingSets > 1) {
                            remainingSets--
                            isBreakRunning = true
                            remainingTime = breakTime
                        } else {
                            isTimerRunning = false
                            coroutineScope.launch {
                                SaveExerciseSession()
                                checkWorkoutCompletion()
                                activeExercise = null
                            }
                            break
                        }
                    }
                }
            } else {
                delay(100)
            }
        }
    }

    // Fetch workout data
    LaunchedEffect(workoutId) {
        try {
            workoutWithExercises = dao.getWorkoutWithExercises(workoutId)
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
        }
    }

    // UI
    Scaffold(
        bottomBar = {
            if (activeExercise != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (remainingTime > 0) {
                        LinearProgressIndicator(
                            progress = {
                                if (isBreakRunning) {
                                    remainingTime.toFloat() / breakTime
                                } else {
                                    remainingTime.toFloat() / exerciseTime
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = if (isBreakRunning) Color.Blue else Color.Green
                        )
                    }

                    BottomAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Sets: ${remainingSets}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }

                            IconButton(
                                onClick = {
                                    isTimerRunning = false
                                    isBreakRunning = false
                                    remainingTime = 0
                                    remainingSets = 0
                                    coroutineScope.launch {
                                        SaveExerciseSession()
                                        checkWorkoutCompletion()
                                        activeExercise = null
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isPaused = !isPaused }
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                if (isPaused) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Paused",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = workoutWithExercises?.firstOrNull()?.workout?.name ?: "Loading...",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            workoutWithExercises?.flatMap { it.exercises }?.forEach { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${exercise.muscle} - ${exercise.part.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var sets by remember { mutableStateOf(exercise.sets) }
                            var reps by remember { mutableStateOf(exercise.reps) }
                            var weight by remember { mutableStateOf(exercise.weight) }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$sets Sets", Modifier.padding(horizontal = 16.dp))
                            }

                            if (exercise.reps > 50) {
                                val timeInSeconds = exercise.reps - 1000
                                var minutes by remember { mutableIntStateOf(timeInSeconds / 60) }
                                var seconds by remember { mutableIntStateOf(timeInSeconds % 60) }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$minutes min")
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$seconds sec")
                                        }
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$weight Kg")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$reps Reps")
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(4.dp)) {
                            IconButton(onClick = { startTimer(exercise) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Exercise",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}