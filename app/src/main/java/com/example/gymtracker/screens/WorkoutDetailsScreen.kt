package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class ExerciseState(
    val exercise: EntityExercise,
    var isCompleted: Boolean = false
)

data class WorkoutState(
    val workout: EntityWorkout,
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

    var activeExercise by remember { mutableStateOf<EntityExercise?>(null) }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutStarted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Loading state


    // Function to start the workout session
    fun startWorkoutSession(exId: Int) {
        val workout = workoutWithExercises?.firstOrNull()?.workout ?: return
        println("Starting workout session for workout ID: $workoutId")
        val workoutSession = SessionWorkoutEntity(
            workoutId = workout.id,
            startTime = System.currentTimeMillis(),
            duration = 0,
            workoutName = workout.name
        )

        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true // Set loading state
            sessionId = dao.insertWorkoutSession(workoutSession) // Insert and get the sessionId
            startTimeWorkout = System.currentTimeMillis() // Record the start time
            println("Workout session started with ID: $sessionId")
            isLoading = false // Reset loading state
            // Navigate to ExerciseScreen after sessionId is set
            withContext(Dispatchers.Main) {
                isLoading = false // Reset loading state
                navController.navigate("exerciseDetails/${exId}/${sessionId}")
            }
        }
    }

    // Function to end the workout session
    fun endWorkoutSession() {
        val endTime = System.currentTimeMillis()
        val duration = (endTime - (startTimeWorkout ?: return)) / 1000

        coroutineScope.launch(Dispatchers.IO) {
            sessionId?.let { id ->
                dao.updateWorkoutSessionDuration(id, duration)
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

    // Animation for breathing effect
    val brightness by animateFloatAsState(
        targetValue = if (isLoading) 1.2f else 1f, // Target brightness
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brightnessAnimation"
    )

    // UI
    Scaffold(
        bottomBar = {
            if (activeExercise == null) {
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
                        Button(
                            onClick = {
                                endWorkoutSession()
                                navController.popBackStack() // Navigate back after saving
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Save Workout")
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
                        .padding(vertical = 8.dp)
                        .clickable {
                            if (!workoutStarted) {
                                startWorkoutSession(exercise.id)
                                workoutStarted = true
                            } else {
                                if (sessionId != null) {
                                    navController.navigate("exerciseDetails/${exercise.id}/${sessionId}")
                                } else {
                                    println("Workout session not started yet. Please wait.")
                                }
                            }
                        }
                        .graphicsLayer {
                            // Apply brightness effect
                            this.alpha = brightness
                        },
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
                                if(exercise.weight!=0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$weight Kg")
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$reps Reps")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}