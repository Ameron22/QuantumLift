package com.example.gymtracker.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.gymtracker.R
import com.example.gymtracker.data.AppDatabase
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.gymtracker.components.SliderWithLabel
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutWithExercises
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.ExerciseWithWorkoutData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.data.ExerciseDao
import java.util.Calendar
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import com.example.gymtracker.navigation.Screen
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.WorkoutExerciseWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    workoutId: Int, 
    navController: NavController,
    viewModel: WorkoutDetailsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }
    var workoutName by remember { mutableStateOf("") }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutStarted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showSaveNotification by remember { mutableStateOf(false) }

    // Collect StateFlow values
    val workoutSession by viewModel.workoutSession.collectAsState()
    val recoveryFactors by viewModel.recoveryFactors.collectAsState()
    val exercisesList by viewModel.exercisesList.collectAsState()
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var currentRecoveryFactor by remember { mutableStateOf("") }
    var showRecoveryInfoDialog by remember { mutableStateOf(false) }

    // Add LaunchedEffect to sync workoutStarted with ViewModel state
    LaunchedEffect(workoutSession) {
        workoutStarted = workoutSession?.isStarted ?: false
        Log.d("WorkoutDetailsScreen", "Synced workoutStarted state: $workoutStarted, session isStarted: ${workoutSession?.isStarted}")
    }

    // Fetch workout data and sync with ViewModel
    LaunchedEffect(workoutId) {
        try {
            Log.d("WorkoutDetailsScreen", "Loading workout data for ID: $workoutId")
            
            // Clear ViewModel state at the beginning to ensure clean state for each workout
            viewModel.clearExercises()
            viewModel.resetWorkoutSession()

            withContext(Dispatchers.IO) {
                // Get exercises with their workout-specific data
                val exercisesData = dao.getExercisesWithWorkoutData(workoutId)
                // Get the workout info
                val workout = dao.getAllWorkouts().find { it.id == workoutId }
                
                withContext(Dispatchers.Main) {
                    Log.d("WorkoutDetailsScreen", "Workout data loaded: ${exercisesData.size} exercises")

                    // Set workout name regardless of whether there are exercises or not
                    workoutName = workout?.name ?: "Unknown Workout"

                    if (exercisesData.isEmpty()) {
                        Log.e("WorkoutDetailsScreen", "No exercises found for workout ID: $workoutId")
                        // Add 5-second delay to show loading effect
                        delay(5000)
                        isLoading = false
                    } else {
                        // Convert to WorkoutExerciseWithDetails and sync with ViewModel
                        val workoutExerciseWithDetails = exercisesData.map { 
                            WorkoutExerciseWithDetails(it.workoutExercise, it.exercise) 
                        }
                        workoutExerciseWithDetails.forEach { exerciseWithDetails ->
                            viewModel.addExercise(exerciseWithDetails)
                        }

                        // Check if there's an existing session
                        if (workoutSession == null || workoutSession?.workoutId != workoutId) {
                            Log.d("WorkoutDetailsScreen", "Initializing new workout session")
                            viewModel.initializeWorkoutSession(workoutId, workoutName)
                        } else {
                            Log.d("WorkoutDetailsScreen", "Using existing workout session")
                            workoutStarted = workoutSession?.isStarted ?: false
                            if (workoutStarted) {
                                startTimeWorkout = workoutSession?.startTime ?: System.currentTimeMillis()
                            }
                        }

                        Log.d("WorkoutDetailsScreen", "Workout name: $workoutName")
                        Log.d(
                            "WorkoutDetailsScreen",
                            "Number of exercises: ${exercisesData.size}"
                        )
                        
                        // Add 5-second delay to show loading effect
                        delay(5000)
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                navController.popBackStack()
            }
        }
    }

    // Listen for new exercises added from AddExerciseToWorkoutScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Int?>("newExerciseId", null)
            ?.collect { newExerciseId ->
                if (newExerciseId != null) {
                    Log.d("WorkoutDetailsScreen", "New exercise added with ID: $newExerciseId")
                    // Refresh exercises from database
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val updatedExercisesData = dao.getExercisesWithWorkoutData(workoutId)
                            withContext(Dispatchers.Main) {
                                // Update ViewModel
                                val workoutExerciseWithDetails = updatedExercisesData.map { 
                                    WorkoutExerciseWithDetails(it.workoutExercise, it.exercise) 
                                }
                                viewModel.clearExercises()
                                workoutExerciseWithDetails.forEach { exerciseWithDetails ->
                                    viewModel.addExercise(exerciseWithDetails)
                                }
                            }
                        }
                    }
                    // Clear the flag
                    navController.currentBackStackEntry?.savedStateHandle?.set("newExerciseId", null)
                }
            }
    }

    // Function to start the workout session
    fun startWorkoutSession(exId: Int) {
        val currentTime = System.currentTimeMillis()
        Log.d("WorkoutDetailsScreen", "Starting workout session at time: $currentTime")
        viewModel.startWorkoutSession(currentTime)
        startTimeWorkout = currentTime
        workoutStarted = true  // Explicitly set workoutStarted to true
        if (workoutSession != null) {
            Log.d(
                "WorkoutDetailsScreen",
                "Navigating to exercise with sessionId: ${workoutSession?.sessionId}, isStarted: ${workoutSession?.isStarted}"
            )
            navController.navigate(Screen.Exercise.createRoute(exId, workoutSession!!.sessionId.toLong(), workoutId))
        } else {
            Log.e("WorkoutDetailsScreen", "Failed to start workout session: session is null")
        }
    }

    // Function to end the workout session
    fun endWorkoutSession() {
        Log.d("WorkoutDetailsScreen", "Ending workout session")
        coroutineScope.launch {
            try {
                // First, save the workout session data
                withContext(Dispatchers.IO) {
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - (workoutSession?.startTime ?: endTime)
                    
                    Log.d("WorkoutDetailsScreen", "Saving workout session - Start: ${workoutSession?.startTime}, End: $endTime, Duration: $duration")
                    Log.d("WorkoutDetailsScreen", "Session details - ID: ${workoutSession?.sessionId}, Workout ID: ${workoutSession?.workoutId}, Name: ${workoutSession?.workoutName}")

                    val sessionWorkout = SessionWorkoutEntity(
                        sessionId = workoutSession?.sessionId ?: 0,
                        workoutId = workoutSession?.workoutId ?: 0,
                        workoutName = workoutSession?.workoutName ?: "",
                        startTime = workoutSession?.startTime ?: endTime,
                        endTime = endTime
                    )

                    // Check if session already exists
                    val existingSession = dao.getWorkoutSession(sessionWorkout.sessionId)
                    if (existingSession != null) {
                        Log.d("WorkoutDetailsScreen", "Updating existing session: ${sessionWorkout.sessionId}")
                        dao.updateWorkoutSession(sessionWorkout)
                    } else {
                        Log.d("WorkoutDetailsScreen", "Inserting new session: ${sessionWorkout.sessionId}")
                        dao.insertWorkoutSession(sessionWorkout)
                    }

                    // Update achievements
                    val totalWorkouts = dao.getTotalWorkoutCount()
                    val isNightWorkout = isNightWorkout(sessionWorkout.startTime)
                    
                    // Update workout count achievement
                    val achievementManager = AchievementManager.getInstance()
                    achievementManager.updateWorkoutCount(totalWorkouts)
                    
                    // Update night workout achievement if applicable
                    if (isNightWorkout) {
                        achievementManager.updateSpecialChallenges("night_owl")
                    }

                    // Calculate and update streak
                    val streak = calculateWorkoutStreak(dao)
                    achievementManager.updateConsistencyStreak(streak)
                }

                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    // Stop the workout session
                    viewModel.stopWorkoutSession()
                    
                    // Show save notification
                    showSaveNotification = true
                    
                    // Wait for 3 seconds
                    delay(3000)
                    
                    // Hide notification and navigate back
                    showSaveNotification = false
                    viewModel.resetWorkoutSession()
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error ending workout session: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    viewModel.resetWorkoutSession()
                    navController.popBackStack()
                }
            }
        }
    }

    // Add state for back confirmation dialog
    var showBackConfirmationDialog by remember { mutableStateOf(false) }

    // Handle back navigation
    BackHandler {
        if (workoutSession != null) {
            showBackConfirmationDialog = true
        } else {
            navController.popBackStack()
        }
    }

    // Back confirmation dialog
    if (showBackConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmationDialog = false },
            title = { Text("Save Exercise?") },
            text = { Text("Do you want to save this workout before going back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        endWorkoutSession()
                        showBackConfirmationDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackConfirmationDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("No")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Update the Recovery Factor Dialog
    if (showRecoveryDialog) {
        val context = LocalContext.current
        
        fun vibrateOnValueChange() {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(currentRecoveryFactor) },
            text = {
                when {
                    currentRecoveryFactor.contains("Protein") -> {
                        SliderWithLabel(
                            value = recoveryFactors.proteinIntake.toFloat(),
                            onValueChange = {
                                vibrateOnValueChange()
                                viewModel.updateRecoveryFactors(proteinIntake = it.toInt())
                            },
                            label = "",
                            valueRange = 0f..300f,
                            steps = 30
                        )
                    }

                    else -> {
                        SliderWithLabel(
                            value = when {
                                currentRecoveryFactor.contains("Sleep") -> recoveryFactors.sleepQuality
                                currentRecoveryFactor.contains("Hydration") -> recoveryFactors.hydration
                                currentRecoveryFactor.contains("Stress") -> recoveryFactors.stressLevel
                                else -> 5
                            }.toFloat(),
                            onValueChange = {
                                vibrateOnValueChange()
                                when {
                                    currentRecoveryFactor.contains("Sleep") ->
                                        viewModel.updateRecoveryFactors(sleepQuality = it.toInt())

                                    currentRecoveryFactor.contains("Hydration") ->
                                        viewModel.updateRecoveryFactors(hydration = it.toInt())

                                    currentRecoveryFactor.contains("Stress") ->
                                        viewModel.updateRecoveryFactors(stressLevel = it.toInt())
                                }
                            },
                            label = "",
                            valueRange = 1f..10f,
                            steps = 9
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Auto-dismiss notification after 3 seconds
    LaunchedEffect(showSaveNotification) {
        if (showSaveNotification) {
            delay(3000)
            showSaveNotification = false
            viewModel.resetWorkoutSession()
            navController.popBackStack()
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

    // Function to format duration in HH:MM:SS format
    fun formatDuration(durationInMillis: Long): String {
        val durationInSeconds = durationInMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Function to calculate current duration
    fun calculateCurrentDuration(startTimeWorkout: Long, workoutStarted: Boolean): String {
        return if (startTimeWorkout > 0 && workoutStarted) {
            val currentDuration = (System.currentTimeMillis() - startTimeWorkout) / 1000
            formatDuration(currentDuration)
        } else {
            "00:00"
        }
    }

    @Composable
    fun WorkoutDurationDisplay(startTimeWorkout: Long, workoutStarted: Boolean) {
        var durationText by remember { mutableStateOf("00:00") }
        var lastUpdateTime by remember { mutableLongStateOf(0L) }

        LaunchedEffect(workoutStarted, startTimeWorkout) {
            while (workoutStarted && startTimeWorkout > 0) {
                lastUpdateTime = System.currentTimeMillis()
                durationText = calculateCurrentDuration(startTimeWorkout, workoutStarted)
                delay(1000) // Update every second
            }
        }

        Text(
            text = "Duration: $durationText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    // Function to display exercise details
    @Composable
    fun ExerciseItem(
        exercise: EntityExercise,
        workoutExercise: WorkoutExercise,
        onDelete: () -> Unit = {}
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)  // Reduced vertical padding
                .clickable {
                    if (workoutSession == null) {
                        startWorkoutSession(exercise.id)
                    } else {
                        navController.navigate(Screen.Exercise.createRoute(exercise.id, workoutSession!!.sessionId.toLong(), workoutId))
                    }
                },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Exercise GIF
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/${exercise.gifUrl}")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Exercise GIF",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Exercise details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sets: ${workoutExercise.sets}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (exercise.useTime && workoutExercise.reps > 1000) {
                                val timeInSeconds = workoutExercise.reps - 1000
                                val minutes = timeInSeconds / 60
                                val seconds = timeInSeconds % 60
                                "Time: ${minutes}:${String.format("%02d", seconds)}"
                            } else {
                                "Reps: ${workoutExercise.reps}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Weight: ${workoutExercise.weight}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Difficulty: ${exercise.difficulty}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Muscle Group: ${exercise.muscle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Delete button and completion indicator
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Delete button
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.minus_icon),
                            contentDescription = "Delete Exercise",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Completion indicator moved to bottom right
                if (viewModel.isExerciseCompleted(exercise.id)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(12.dp)
                            .background(
                                color = Color.Green,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }

    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = workoutName,
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
                        ),
                        enabled = !showSaveNotification // Disable button while showing notification
                    ) {
                        Text("Save Workout")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            // Loading indicator in center of page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout duration display
                if (workoutStarted) {
                    WorkoutDurationDisplay(startTimeWorkout, workoutStarted)
                }

                // Exercise list
                exercisesList.forEach { exerciseWithDetails ->
                    ExerciseItem(
                        exercise = exerciseWithDetails.entityExercise,
                        workoutExercise = exerciseWithDetails.workoutExercise,
                        onDelete = {
                            coroutineScope.launch {
                                // Remove the exercise from the workout
                                dao.deleteWorkoutExercise(exerciseWithDetails.workoutExercise)
                                // Update ViewModel by refreshing from database
                                val updatedData = withContext(Dispatchers.IO) {
                                    dao.getExercisesWithWorkoutData(workoutId)
                                        .map { WorkoutExerciseWithDetails(it.workoutExercise, it.exercise) }
                                }
                                viewModel.clearExercises()
                                updatedData.forEach { exerciseWithDetails ->
                                    viewModel.addExercise(exerciseWithDetails)
                                }
                            }
                        }
                    )
                }

                // Add Exercise Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            navController.navigate(Screen.AddExerciseToWorkout.createRoute(workoutId))
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus_icon),
                            contentDescription = "Add Exercise",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Exercise",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Recovery Factors Section
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recovery Factors",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            IconButton(
                                onClick = { showRecoveryInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Recovery Factors Info",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Add Recovery Factors Info Dialog
                        if (showRecoveryInfoDialog) {
                            AlertDialog(
                                onDismissRequest = { showRecoveryInfoDialog = false },
                                title = { Text("Recovery Factors") },
                                text = {
                                    Text(
                                        "Recovery factors help calculate muscle stress and recovery more accurately. " +
                                        "If you provide all recovery factors, the app will use a more detailed formula " +
                                        "that takes into account your sleep quality, protein intake, hydration, and stress levels. " +
                                        "If any factor is not provided (set to 0), the app will use a simplified formula " +
                                        "based only on exercise load and volume.\n\n" +
                                        "Factors:\n" +
                                        "• Sleep Quality (1-10): How well you slept\n" +
                                        "• Protein Intake (g): Amount of protein consumed\n" +
                                        "• Hydration (1-10): Your hydration level\n" +
                                        "• Stress Level (1-10): Your current stress level"
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showRecoveryInfoDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Sleep", recoveryFactors.sleepQuality, "Sleep Quality (1-10)"),
                                Triple("Protein", recoveryFactors.proteinIntake, "Protein Intake (g)"),
                                Triple("Hydration", recoveryFactors.hydration, "Hydration Level (1-10)"),
                                Triple("Stress Lvl", recoveryFactors.stressLevel, "Stress Level (1-10)")
                            ).forEach { (shortLabel, value, fullLabel) ->
                                Button(
                                    onClick = {
                                        currentRecoveryFactor = fullLabel
                                        showRecoveryDialog = true
                                    },
                                    modifier = Modifier
                                        .height(80.dp)  // Increased height
                                        .weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (value > 0)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (value == 0) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.plus_icon),
                                                    contentDescription = "Add $shortLabel",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Factor-specific icon
                                        when (shortLabel) {
                                            "Sleep" -> Icon(
                                                painter = painterResource(id = R.drawable.sleep_icon),
                                                contentDescription = "Sleep Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Protein" -> Icon(
                                                painter = painterResource(id = R.drawable.food_icon),
                                                contentDescription = "Food Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Hydration" -> Icon(
                                                painter = painterResource(id = R.drawable.drop_icon),
                                                contentDescription = "Hydration Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Stress Lvl" -> Icon(
                                                painter = painterResource(id = R.drawable.stress_icon),
                                                contentDescription = "Stress Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (value == 0) shortLabel
                                            else if (shortLabel == "Protein") "${value}g"
                                            else "$value",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (value > 0)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun calculateWorkoutStreak(dao: ExerciseDao): Int {
    val sessions = dao.getAllWorkoutSessionsOrderedByDate()
    if (sessions.isEmpty()) return 0

    val calendar = Calendar.getInstance()
    var currentStreak = 1
    var lastWorkoutDate = calendar.apply { 
        timeInMillis = sessions.first().startTime 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    for (i in 1 until sessions.size) {
        val session = sessions[i]
        val sessionDate = calendar.apply { 
            timeInMillis = session.startTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Check if this workout was on the consecutive previous day
        if ((lastWorkoutDate - sessionDate) == TimeUnit.DAYS.toMillis(1)) {
            currentStreak++
            lastWorkoutDate = sessionDate
        } else {
            break
        }
    }

    return currentStreak
}

private fun isNightWorkout(startTime: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startTime
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    return currentHour >= 22 || currentHour < 4
}