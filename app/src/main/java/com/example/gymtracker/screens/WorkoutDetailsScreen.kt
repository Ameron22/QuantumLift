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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.ExerciseEntity
import com.example.gymtracker.data.WorkoutWithExercises
import kotlinx.coroutines.delay


@Composable
fun WorkoutDetailsScreen(workoutId: Int, navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }

    var activeExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var remainingSets by remember { mutableIntStateOf(0) }
    var remainingTime by remember { mutableIntStateOf(0) }
    var exerciseTime by remember { mutableIntStateOf(0) }
    var breakTime by remember { mutableIntStateOf(10) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isBreakRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) } // New state for pause/resume

    // Function to start the timer for an exercise
    fun startTimer(exercise: ExerciseEntity) {
        activeExercise = exercise

        remainingSets = exercise.sets
        isTimerRunning = true
        isBreakRunning = false
        isPaused = false // Reset pause state
        exerciseTime = if (exercise.reps > 50) {
            // Time-based exercise (convert reps to seconds)
            exercise.reps - 1000
        } else {
            // Reps-based exercise (default to 60 seconds for demonstration)
            10
        }
        remainingTime = exerciseTime

    }

    // Countdown logic
    LaunchedEffect(isTimerRunning, isPaused) {
        while (isTimerRunning) {
            if (!isPaused) { // Only count down if not paused
                if (remainingTime > 0) {
                    delay(1000) // Wait for 1 second
                    remainingTime--
                } else {
                    if (isBreakRunning) {
                        // Break timer finished
                        isBreakRunning = false
                        if (remainingSets > 0) {
                            // Start the next set
                            remainingSets--
                            remainingTime = exerciseTime // Reset to exercise time
                        } else {
                            // All sets and breaks are done
                            activeExercise = null
                            isTimerRunning = false
                            break // Exit the loop
                        }
                    } else {
                        // Exercise timer finished
                        if (remainingSets > 1) {
                            // Start the break (only if it's not the last set)
                            isBreakRunning = true
                            remainingTime = breakTime // Set to break time
                        } else {
                            // Last set completed, stop the timer
                            activeExercise = null
                            isTimerRunning = false
                            break // Exit the loop
                        }
                    }
                }
            } else {
                delay(100) // Small delay to avoid busy-waiting
            }
        }
    }

    LaunchedEffect(workoutId) {
        try {
            workoutWithExercises = dao.getWorkoutWithExercises(workoutId)
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
        }
    }


    Scaffold(
        bottomBar = {
            if (activeExercise != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Slim progress bar
                    if (remainingTime > 0) {
                        LinearProgressIndicator(
                            progress = {
                                if (isBreakRunning) {
                                    // Break progress (blue)
                                    remainingTime.toFloat() / breakTime
                                } else {
                                    // Exercise progress (green)
                                    remainingTime.toFloat() / exerciseTime
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp), // Slim bar height
                            color = if (isBreakRunning) {
                                Color.Blue // Blue for break
                            } else {
                                Color.Green // Green for exercise
                            }
                        )
                    }

                    // BottomAppBar content
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
                                modifier = Modifier.weight(1f) // Take half of the available space
                            ) {
                                Text(
                                    text = "Sets: ${remainingSets}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }

                            // Stop Button
                            IconButton(
                                onClick = {
                                    // Stop the timer and reset the state
                                    activeExercise = null
                                    isTimerRunning = false
                                    isBreakRunning = false
                                    remainingTime = 0
                                    remainingSets = 0
                                },
                                modifier = Modifier
                                    .size(64.dp) // Big stop button
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
                                    .weight(1f) // Take half of the available space
                                    .clickable { isPaused = !isPaused } // Toggle pause/resume
                            ) {
                                Text(
                                    text = "${
                                        String.format(
                                            "%02d:%02d",
                                            remainingTime / 60,
                                            remainingTime % 60
                                        )
                                    }",
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
                        // Left side: Exercise details (name, muscle, and parts)
                        Column(
                            modifier = Modifier
                                .weight(1f)// Takes remaining space
                        ) {
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

                        // Right side: Sets and reps
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var sets by remember { mutableStateOf(exercise.sets) }
                            var reps by remember { mutableStateOf(exercise.reps) }

                            // Sets Column
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$sets Sets", Modifier.padding(horizontal = 16.dp))
                            }

                            if (exercise.reps > 50) {
                                val timeInSeconds = exercise.reps - 1000
                                var minutes by remember { mutableIntStateOf(timeInSeconds / 60) }
                                var seconds by remember { mutableIntStateOf(timeInSeconds % 60) }
                                // Time Input
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Minutes
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$minutes min")
                                        }
                                        // Seconds
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$seconds sec")
                                        }
                                    }
                                }
                            } else {
                                // Reps Input
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$reps Reps")
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .padding(4.dp)
                        ) {
                            // Start Button
                            IconButton(
                                onClick = { startTimer(exercise)  },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow, // Play icon
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