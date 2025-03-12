package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ExerciseState(
    val exercise: EntityExercise,
    var isCompleted: Boolean = false
)

data class WorkoutState(
    val workout: EntityWorkout,
    val exercises: List<ExerciseState>,
    var isFinished: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(workoutId: Int, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutStarted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSaveNotification by remember { mutableStateOf(false) }

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
            isLoading = true
            sessionId = dao.insertWorkoutSession(workoutSession)
            startTimeWorkout = System.currentTimeMillis()
            println("Workout session started with ID: $sessionId")
            isLoading = false
            withContext(Dispatchers.Main) {
                isLoading = false
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
                Log.d("WorkoutDetailsScreen", "Workout session duration updated: $duration seconds")
                showSaveNotification = true
            }
        }
    }

    // Auto-dismiss notification after 3 seconds
    LaunchedEffect(showSaveNotification) {
        if (showSaveNotification) {
            delay(3000)
            showSaveNotification = false
            navController.popBackStack()
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
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = workoutWithExercises?.firstOrNull()?.workout?.name ?: "Loading...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { endWorkoutSession() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save Workout")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                workoutWithExercises?.flatMap { it.exercises }?.forEach { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${exercise.sets} sets × ${if (exercise.reps > 50) "${(exercise.reps-1000)/60}:${(exercise.reps-1000)%60} min" else "${exercise.reps} reps"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Save Notification
            if (showSaveNotification) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showSaveNotification = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(32.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Workout Completed!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = workoutWithExercises?.firstOrNull()?.workout?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Duration: ${TimeUnit.SECONDS.toMinutes(((System.currentTimeMillis() - startTimeWorkout) / 1000))} minutes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(System.currentTimeMillis()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}