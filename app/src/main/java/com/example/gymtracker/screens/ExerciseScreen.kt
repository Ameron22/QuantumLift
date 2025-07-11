package com.example.gymtracker.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.navigation.NavController
import com.example.gymtracker.R
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.Converter
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionEntityExercise
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.ExerciseWithWorkoutData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.example.gymtracker.components.SliderWithLabel
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.components.ExerciseGif
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.DisposableEffect
import com.example.gymtracker.data.UserSettingsPreferences
import android.content.Intent
import com.example.gymtracker.services.TimerService
import android.app.NotificationManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import kotlin.math.roundToInt

// Helper functions for unified TimerService
private fun startTimerService(context: Context, time: Int, isBreak: Boolean, exercise: String, exId: Int = 0, sessId: Long = 0L, wId: Int = 0) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "START_TIMER"
        putExtra("remaining_time", time)
        putExtra("is_break", isBreak)
        putExtra("exercise_name", exercise)
        putExtra("exercise_id", exId)
        putExtra("session_id", sessId)
        putExtra("workout_id", wId)
    }
    context.startForegroundService(intent)
}

private fun updateTimerService(context: Context, time: Int, isBreak: Boolean, exercise: String, exId: Int = 0, sessId: Long = 0L, wId: Int = 0) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "UPDATE_TIMER"
        putExtra("remaining_time", time)
        putExtra("is_break", isBreak)
        putExtra("exercise_name", exercise)
        putExtra("exercise_id", exId)
        putExtra("session_id", sessId)
        putExtra("workout_id", wId)
    }
    context.startService(intent)
}

private fun stopTimerService(context: Context) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "STOP_TIMER"
    }
    context.startService(intent)
}

private fun pauseTimerService(context: Context) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "PAUSE_TIMER"
    }
    context.startService(intent)
}

private fun resumeTimerService(context: Context) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "RESUME_TIMER"
    }
    context.startService(intent)
}

private fun startFloatingTimer(context: Context, time: Int, isBreak: Boolean, exercise: String, exId: Int, sessId: Long, wId: Int) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "START_FLOATING"
        putExtra("remaining_time", time)
        putExtra("is_break", isBreak)
        putExtra("exercise_name", exercise)
        putExtra("exercise_id", exId)
        putExtra("session_id", sessId)
        putExtra("workout_id", wId)
    }
    context.startForegroundService(intent)
}

private fun stopFloatingTimer(context: Context) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "STOP_FLOATING"
    }
    context.startService(intent)
}

private fun hideDeleteZone(context: Context) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = "HIDE_DELETE_ZONE"
    }
    context.startService(intent)
}

// Function to stop the timer and clean up notifications
private fun stopTimerAndCleanup(context: Context) {
    Log.d("ExerciseScreen", "stopTimerAndCleanup called")
    
    // Cancel all notifications immediately
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        Log.d("ExerciseScreen", "All notifications cancelled")
    } catch (e: Exception) {
        Log.e("ExerciseScreen", "Error cancelling notifications: ${e.message}")
    }
    
    // Stop timer service
    stopTimerService(context)
    
    // Stop floating timer
    stopFloatingTimer(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    exerciseId: Int,
    workoutSessionId: Long,
    workoutId: Int,
    navController: NavController,
    viewModel: WorkoutDetailsViewModel = viewModel(),
    generalViewModel: GeneralViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var preSetBreakTime by remember { mutableIntStateOf(10) } // Will be set from user preferences
    var exerciseWithDetails by remember { mutableStateOf<ExerciseWithWorkoutData?>(null) }
    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }
    var showSaveNotification by remember { mutableStateOf(false) }

    // Timer related states
    var activeSetIndex by remember { mutableStateOf<Int?>(null) }
    var remainingTime by remember { mutableIntStateOf(0) }
    var exerciseTime by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isBreakRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var breakTime by remember { mutableIntStateOf(60) } // Will be set from user preferences
    var setTimeReps by remember { mutableIntStateOf(30) } // Will be set from user preferences
    var pausedTime by remember { mutableStateOf(0L) }
    // Add new state for countdown
    var showCountdown by remember { mutableStateOf(false) }
    var countdownNumber by remember { mutableIntStateOf(5) }

    // Map to store weights for each set (set number to weight)
    val setWeights = remember { mutableStateMapOf<Int, Int>() }
    // Map to store repetitions for each set (set number to reps)
    val setReps = remember { mutableStateMapOf<Int, Int>() }
    // Track which set is currently being edited (1-indexed)
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }
    // Track which set is currently being executed
    var completedSet by remember { mutableStateOf(0) }

    // New state variables for muscle soreness tracking
    var eccentricFactor by remember { mutableStateOf(1.0f) }
    var noveltyFactor by remember { mutableStateOf(5) }
    var adaptationLevel by remember { mutableStateOf(5) }
    var rpe by remember { mutableStateOf(5) }
    var subjectiveSoreness by remember { mutableStateOf(5) }
    var showSorenessDialog by remember { mutableStateOf(false) }

    // Add new state variable for break time picker
    var showBreakTimePicker by remember { mutableStateOf(false) }
    var showSetTimePicker by remember { mutableStateOf(false) }

    // Add this state variable with the other state variables
    var showInfoDialog by remember { mutableStateOf<String?>(null) }

    // Add new state variables at the top of ExerciseScreen composable
    var hasProvidedSorenessData by remember { mutableStateOf(false) }
    var defaultSorenessValues by remember { mutableStateOf(true) }  // Changed to true to make checkbox unchecked by default


    // Add state for back confirmation dialog
    var showBackConfirmationDialog by remember { mutableStateOf(false) }

    // Add MediaPlayer instances
    val pingSound = remember { MediaPlayer.create(context, R.raw.ping) }
    val peeengSound = remember { MediaPlayer.create(context, R.raw.peeeng) }
    
    // Load user settings
    val userSettings = remember { UserSettingsPreferences(context) }
    val settings by userSettings.settingsFlow.collectAsState(initial = null)

    // Clean up MediaPlayer when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            pingSound.release()
            peeengSound.release()
        }
    }
    
    // Stop timer service when leaving screen if timer is running
    DisposableEffect(Unit) {
        onDispose {
            if (isTimerRunning) {
                stopTimerAndCleanup(context)
            } else {
                // Always stop floating timer when leaving screen
                stopFloatingTimer(context)
            }
        }
    }
    
    // Lifecycle observer to detect when app goes to background/foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // App going to background - start floating timer if timer is running
                    if (isTimerRunning) {
                        Log.d("ExerciseScreen", "App going to background - starting floating timer, isPaused: $isPaused")
                        
                        // First, update the service with current state
                        updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                        
                        // Start floating timer
                        startFloatingTimer(
                            context,
                            remainingTime,
                            isBreakRunning,
                            exerciseWithDetails?.exercise?.name ?: "Exercise",
                            exerciseId,
                            workoutSessionId,
                            workoutId
                        )
                        
                        // If the app timer was paused, update service pause state
                        if (isPaused) {
                            Log.d("ExerciseScreen", "App timer was paused, updating service pause state")
                            pauseTimerService(context)
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // App coming to foreground - sync pause state and stop floating timer
                    if (isTimerRunning && TimerService.isFloatingMode) {
                        Log.d("ExerciseScreen", "App coming to foreground - syncing pause state and stopping floating timer")
                        val floatingTimerPaused = TimerService.isPaused
                        
                        // Sync pause state
                        if (floatingTimerPaused != isPaused) {
                            Log.d("ExerciseScreen", "Syncing pause state: floating=$floatingTimerPaused, app=$isPaused")
                            isPaused = floatingTimerPaused
                        }
                        
                        // Stop floating timer when app comes to foreground
                        stopFloatingTimer(context)
                        
                        Log.d("ExerciseScreen", "State synced and floating timer stopped: isPaused=$isPaused")
                    }
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Set up callbacks for floating timer pause/resume requests
    DisposableEffect(Unit) {
        TimerService.onPauseRequest = {
            Log.d("ExerciseScreen", "Pause request from floating timer")
            isPaused = true
        }
        TimerService.onResumeRequest = {
            Log.d("ExerciseScreen", "Resume request from floating timer")
            isPaused = false
        }
        TimerService.onTimerDeleted = {
            Log.d("ExerciseScreen", "Floating timer deleted - keeping app timer running")
            // Don't stop the main timer, just acknowledge floating timer is gone
            // Timer continues running in app and notifications
        }
        
        onDispose {
            TimerService.onPauseRequest = null
            TimerService.onResumeRequest = null
            TimerService.onTimerDeleted = null
        }
    }

    // Load user settings when they become available
    LaunchedEffect(settings) {
        settings?.let { userSettings: com.example.gymtracker.data.UserSettings ->
            breakTime = userSettings.defaultBreakTime
            setTimeReps = userSettings.defaultWorkTime
            preSetBreakTime = userSettings.defaultPreSetBreakTime
        }
    }
    
    // Fetch exercise and workoutExercise data using the new structure
    LaunchedEffect(exerciseId, workoutId) {
        try {
            Log.d("ExerciseScreen", "Fetching exercise with ID: $exerciseId and workoutId: $workoutId")
            withContext(Dispatchers.IO) {
                // Get exercises with workout data for this workout
                val exercisesData = dao.getExercisesWithWorkoutData(workoutId)
                // Find the specific exercise we need
                withContext(Dispatchers.Main) {
                    val foundExerciseWithDetails = exercisesData.find { it.exercise.id == exerciseId }
                    if (foundExerciseWithDetails == null) {
                        Log.e("ExerciseScreen", "Exercise not found in workout")
                        navController.popBackStack()
                        return@withContext
                    }
                    
                    // Assign to the mutable state variable
                    exerciseWithDetails = foundExerciseWithDetails
                    val workoutExercise = foundExerciseWithDetails.workoutExercise
                    val exercise = foundExerciseWithDetails.exercise
                    
                    // Initialize weights and reps for all sets
                    for (set in 1..workoutExercise.sets) {
                        setWeights[set] = workoutExercise.weight
                        setReps[set] = workoutExercise.reps
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExerciseScreen", "Database error: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                navController.popBackStack()
            }
        }
    }

    // Function to save exercise session
    suspend fun saveExerciseSession() {
        Log.d("ExerciseScreen", "saveExerciseSession called")
        val exercise = exerciseWithDetails?.exercise ?: return
        val workoutExercise = exerciseWithDetails?.workoutExercise ?: return
        
        // Ensure completedSet reflects the actual completed sets
        completedSet = minOf(completedSet, workoutExercise.sets)
        
        val repsOrTimeList = (1..workoutExercise.sets).map { setReps[it] ?: workoutExercise.reps }
        val weightList = (1..workoutExercise.sets).map { setWeights[it] ?: workoutExercise.weight }
        val maxWeight = weightList.maxOrNull() ?: 0
        val exerciseSession = SessionEntityExercise(
            sessionId = workoutSessionId,
            exerciseId = exercise.id.toLong(),
            sets = workoutExercise.sets,
            repsOrTime = repsOrTimeList,
            weight = weightList,
            muscleGroup = exercise.muscle,
            muscleParts = Converter().fromString(exercise.parts).joinToString(", "),
            completedSets = completedSet,
            notes = "",
            eccentricFactor = eccentricFactor,
            noveltyFactor = noveltyFactor,
            adaptationLevel = adaptationLevel,
            rpe = rpe,
            subjectiveSoreness = subjectiveSoreness
        )

        try {
            withContext(Dispatchers.IO) {
                dao.insertExerciseSession(exerciseSession)
                Log.d("ExerciseScreen", "Exercise session saved successfully")

                // Update achievements
                val achievementManager = AchievementManager.getInstance()
                achievementManager.updateStrengthProgress(exercise.name, maxWeight.toFloat())
            }
            
            // Mark exercise as completed in GeneralViewModel
            Log.d("ExerciseScreen", "Marking exercise ${exercise.id} (${exercise.name}) as completed")
            generalViewModel.markExerciseAsCompleted(exercise.id)
            
            // Show success notification and navigate back
            withContext(Dispatchers.Main) {
                showSaveNotification = true
                delay(2000)
                showSaveNotification = false
                // Stop timer if still running when exercise is saved
                if (isTimerRunning) {
                    isTimerRunning = false
                    isPaused = false
                    isBreakRunning = false
                    remainingTime = 0
                    showCountdown = false
                    stopTimerAndCleanup(context)
                }
                navController.previousBackStackEntry?.savedStateHandle?.set("exerciseCompleted", true)
                // Navigate back after the notification timer expires
                navController.popBackStack()
            }
        } catch (e: Exception) {
            Log.e("ExerciseScreen", "Error saving exercise session: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                navController.popBackStack()
            }
        }
    }

    // Timer logic - single source of truth in ExerciseScreen
    LaunchedEffect(isTimerRunning, isPaused) {
        Log.d("ExerciseScreen", "Timer LaunchedEffect started: isTimerRunning=$isTimerRunning, isPaused=$isPaused")
        
        // ExerciseScreen handles all countdown logic, service just displays
        while (isTimerRunning) {
            // Check local pause state (floating timer is stopped when in app)
            if (!isPaused) {
                if (remainingTime > 0) {
                    // Check if we're in the last 5 seconds of break
                    if (isBreakRunning && remainingTime <= 5) {
                        showCountdown = true
                        countdownNumber = remainingTime
                        // Vibrate and play sound when countdown number changes (check user preferences)
                        if (settings?.vibrationEnabled == true) {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(100)
                            }
                        }
                        // Play ping sound (check user preferences)
                        if (settings?.soundEnabled == true) {
                            pingSound.seekTo(0)
                            pingSound.setVolume(settings?.soundVolume ?: 0.5f, settings?.soundVolume ?: 0.5f)
                            pingSound.start()
                        }
                    } else {
                        showCountdown = false
                    }
                    delay(1000)
                    remainingTime--
                    
                    // Update floating timer (it will be visible when app goes to background)
                    Log.d("ExerciseScreen", "Updating floating timer: $remainingTime seconds")
                    updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                } else {
                    showCountdown = false
                    if (isBreakRunning) {
                        // Break time finished, start next exercise set
                        isBreakRunning = false
                        // Long vibration and sound when exercise starts (check user preferences)
                        if (settings?.vibrationEnabled == true) {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(500)
                            }
                        }
                        // Play peeeng sound (check user preferences)
                        if (settings?.soundEnabled == true) {
                            peeengSound.seekTo(0)
                            peeengSound.setVolume(settings?.soundVolume ?: 0.5f, settings?.soundVolume ?: 0.5f)
                            peeengSound.start()
                        }
                        
                        val workoutExercise = exerciseWithDetails?.workoutExercise
                        // If this was the pre-set break before the first set, start the first set now
                        if (activeSetIndex == 1 && completedSet == 0) {
                            exerciseTime = if (workoutExercise?.reps ?: 0 > 50) {
                                (workoutExercise?.reps ?: 0) - 1000
                            } else {
                                setTimeReps
                            }
                            remainingTime = exerciseTime
                            Log.d("ExerciseScreen", "Pre-set break finished, starting first set: $exerciseTime seconds")
                            // No need to increment activeSetIndex
                            // Update floating timer immediately when switching to exercise mode
                            Log.d("ExerciseScreen", "Updating floating timer to EXERCISE mode: $remainingTime seconds")
                            updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                            continue
                        }
                        if (activeSetIndex != null && activeSetIndex!! < (workoutExercise?.sets ?: 0)) {
                            // Start next set
                            activeSetIndex = activeSetIndex!! + 1
                            remainingTime = exerciseTime
                            Log.d("ExerciseScreen", "Starting next set: ${activeSetIndex}")
                            // Update floating timer immediately when switching to exercise mode
                            Log.d("ExerciseScreen", "Updating floating timer to EXERCISE mode: $remainingTime seconds")
                            updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                        } else {
                            // All sets completed
                            Log.d("ExerciseScreen", "All sets completed, saving exercise session")
                            isTimerRunning = false
                            isPaused = false
                            isBreakRunning = false
                            remainingTime = 0
                            showCountdown = false
                            activeSetIndex = null
                            stopTimerAndCleanup(context)
                            completedSet = workoutExercise?.sets ?: 0
                            try {
                                coroutineScope.launch {
                                    saveExerciseSession()
                                }
                            } catch (e: Exception) {
                                Log.e("ExerciseScreen", "Error saving exercise: ${e.message}")
                                navController.popBackStack()
                            }
                        }
                    } else {
                        // Exercise time finished, start break
                        completedSet += 1
                        
                        // Activate the workout when the first set is completed
                        if (completedSet == 1) {
                            Log.d("ExerciseScreen", "First set completed - activating workout")
                            generalViewModel.activateWorkout()
                        }
                        
                        val workoutExercise = exerciseWithDetails?.workoutExercise
                        if (activeSetIndex!! >= (workoutExercise?.sets ?: 0)) {
                            // All sets completed
                            Log.d("ExerciseScreen", "All sets completed (exercise time), saving exercise session")
                            isTimerRunning = false
                            isPaused = false
                            isBreakRunning = false
                            remainingTime = 0
                            showCountdown = false
                            activeSetIndex = null
                            stopTimerAndCleanup(context)
                            completedSet = workoutExercise?.sets ?: 0
                            try {
                                coroutineScope.launch {
                                    saveExerciseSession()
                                }
                            } catch (e: Exception) {
                                Log.e("ExerciseScreen", "Error saving exercise: ${e.message}")
                                navController.popBackStack()
                            }
                        } else {
                            // Start break time
                            isBreakRunning = true
                            remainingTime = breakTime
                            Log.d("ExerciseScreen", "Starting break time: $breakTime seconds")
                            // Update floating timer immediately when switching to break mode
                            Log.d("ExerciseScreen", "Updating floating timer to BREAK mode: $remainingTime seconds")
                            updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                        }
                    }
                }
            } else {
                // Timer is paused (either locally or by floating timer), just wait
                delay(100)
            }
        }
    }

    // Function to stop the timer
    fun stopTimer() {
        Log.d("ExerciseScreen", "stopTimer called")
        isTimerRunning = false
        isPaused = false
        isBreakRunning = false
        remainingTime = 0
        pausedTime = 0L
        showCountdown = false  // Hide countdown display when stopping timer
        
        // Use the external cleanup function
        stopTimerAndCleanup(context)
        
        // Reset active set index when stopping timer
        activeSetIndex = null
        // Don't reset completedSet to maintain progress
    }

    // Handle back navigation
    BackHandler {
        if (isTimerRunning) {
            stopTimer()
        }
        // Only show confirmation dialog if at least one set is completed
        if (completedSet > 0) {
            showBackConfirmationDialog = true
        } else {
            // If no sets completed, just go back without confirmation
            navController.popBackStack()
        }
    }

    // Back confirmation dialog
    if (showBackConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmationDialog = false },
            title = { Text("Save Exercise?") },
            text = { Text("Do you want to save this exercise before going back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackConfirmationDialog = false
                        coroutineScope.launch {
                            saveExerciseSession()
                        }
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

    // Function to start the timer for a specific set
    fun startTimer(setIndex: Int) {
        try {
            Log.d("ExerciseScreen", "startTimer called with setIndex: $setIndex")
            
            val workoutExercise = exerciseWithDetails?.workoutExercise ?: return
            
            // Activate workout if not already active
            if (!generalViewModel.isWorkoutActive()) {
                Log.d("ExerciseScreen", "Activating workout at timer start")
                generalViewModel.activateWorkout()
            }

            if (setIndex == 1 && completedSet == 0) {
                // Pre-set break before first set
                isBreakRunning = true
                remainingTime = preSetBreakTime
                activeSetIndex = setIndex
                isTimerRunning = true
                isPaused = false
                pausedTime = 0L
                Log.d("ExerciseScreen", "Starting pre-set break before first set: $preSetBreakTime seconds")
                return
            }
            
            activeSetIndex = setIndex
            exerciseTime = if (workoutExercise.reps > 50) {
                workoutExercise.reps - 1000
            } else {
                setTimeReps
            }
            remainingTime = exerciseTime
            isTimerRunning = true
            isBreakRunning = false
            isPaused = false
            pausedTime = 0L
            
            // Check for overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                // Request overlay permission
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                return
            }
            
            // Don't start floating timer when in the app - it will start when app goes to background
            Log.d("ExerciseScreen", "Timer started in app - floating timer will appear when app goes to background")
            
            Log.d("ExerciseScreen", "Timer started: exerciseTime=$exerciseTime, remainingTime=$remainingTime, sets=${workoutExercise.sets}")
            
            // Floating timer will be started when app goes to background
            Log.d("ExerciseScreen", "Timer started - floating timer will appear when app goes to background")
        } catch (e: Exception) {
            Log.e("ExerciseScreen", "Error starting timer: ${e.message}")
            e.printStackTrace()
            // Continue without floating timer
            Log.d("ExerciseScreen", "Continuing timer without floating timer")
        }
    }

    // Function to pause the timer
    fun pauseTimer() {
        if (isTimerRunning && !isPaused) {
            isPaused = true
            pausedTime = System.currentTimeMillis()
            // Pause timer service
            pauseTimerService(context)
        }
    }

    // Function to resume the timer
    fun resumeTimer() {
        if (isTimerRunning && isPaused) {
            isPaused = false
            // Resume timer service
            resumeTimerService(context)
        }
    }

    // Function to skip the current set
    fun skipSet() {
        Log.d("ExerciseScreen", "skipSet called")
        if (isTimerRunning) {
            val workoutExercise = exerciseWithDetails?.workoutExercise
            if (isBreakRunning) {
                // Check if this is the pre-set break (before first set)
                if (activeSetIndex == 1 && completedSet == 0) {
                    Log.d("ExerciseScreen", "Skipping pre-set break, starting first set")
                    isBreakRunning = false
                    // Calculate exercise time for first set
                    exerciseTime = if (workoutExercise?.reps ?: 0 > 50) {
                        (workoutExercise?.reps ?: 0) - 1000
                    } else {
                        setTimeReps
                    }
                    remainingTime = exerciseTime
                    // activeSetIndex stays at 1 for the first set
                    // Update floating timer immediately when switching to exercise mode
                    Log.d("ExerciseScreen", "Updating floating timer to EXERCISE mode: $remainingTime seconds")
                    updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                } else {
                    // Normal break between sets
                    Log.d("ExerciseScreen", "Skipping break, moving to next set")
                    isBreakRunning = false
                    activeSetIndex = activeSetIndex?.let { it + 1 }
                    if (activeSetIndex != null && activeSetIndex!! <= (workoutExercise?.sets ?: 0)) {
                        remainingTime = exerciseTime
                        // Update service with new exercise time
                        updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                    } else {
                        Log.d("ExerciseScreen", "All sets completed (skip break), saving exercise session")
                        stopTimer()
                        coroutineScope.launch {
                            saveExerciseSession()
                        }
                    }
                }
            } else {
                Log.d("ExerciseScreen", "Skipping exercise set, completedSet=$completedSet, totalSets=${workoutExercise?.sets}")
                completedSet += 1  // Increment completed sets when skipping
                
                // Activate the workout when the first set is completed
                if (completedSet == 1) {
                    Log.d("ExerciseScreen", "First set completed (skip) - activating workout")
                    generalViewModel.activateWorkout()
                }
                
                // Check if this was the last set
                if (activeSetIndex!! >= (workoutExercise?.sets ?: 0)) {
                    Log.d("ExerciseScreen", "Last set completed (skip exercise), saving exercise session")
                    completedSet = workoutExercise?.sets ?: 0  // Mark all sets as completed
                    stopTimer()
                    coroutineScope.launch {
                        saveExerciseSession()
                    }
                } else {
                    // Start break for next set
                    isBreakRunning = true
                    remainingTime = breakTime
                    // Update service with break time
                    updateTimerService(context, remainingTime, isBreakRunning, exerciseWithDetails?.exercise?.name ?: "Exercise", exerciseId, workoutSessionId, workoutId)
                }
            }
        }
    }

    fun vibrateOnValueChange() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    // UI
    // Add this before the Scaffold
    if (showCountdown) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .zIndex(1000f), // Ensure it's on top of everything
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(100.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownNumber.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 180.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(100.dp),
                            spotColor = Color.Red
                        )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseWithDetails?.exercise?.name ?: "Exercise",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (isTimerRunning) {
                            stopTimer()
                        }
                        // Only show confirmation dialog if at least one set is completed
                        if (completedSet > 0) {
                            showBackConfirmationDialog = true
                        } else {
                            // If no sets completed, just go back without confirmation
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                if (isTimerRunning) {
                    // Timer progress bar
                    ProgressBarWithBall(
                        progress = if (isBreakRunning) {
                            val calculatedProgress = remainingTime.toFloat() / breakTime
                            if (calculatedProgress.isNaN() || calculatedProgress.isInfinite()) 0f else calculatedProgress
                        } else {
                            val calculatedProgress = remainingTime.toFloat() / exerciseTime
                            if (calculatedProgress.isNaN() || calculatedProgress.isInfinite()) 0f else calculatedProgress
                        },
                        isBreak = isBreakRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp)
                    )

                    // Timer display
                    BottomAppBar(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timer text
                                Text(
                                text = if (isBreakRunning) "Break Time" else "Exercise Time",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    remainingTime / 60,
                                    remainingTime % 60
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Pause/Resume button
                            IconButton(
                                onClick = {
                                    if (isPaused) {
                                        resumeTimer()
                                    } else {
                                        pauseTimer()
                                    }
                                }
                            ) {
                                if (isPaused) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.pause2_icon),
                                        contentDescription = "Pause",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Stop button
                            IconButton(
                                onClick = { stopTimer() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.stop_icon),
                                    contentDescription = "Stop",
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Skip button
                            IconButton(
                                onClick = { skipSet() }
                            ) {
                                    Icon(
                                    painter = painterResource(id = R.drawable.skip_icon),
                                    contentDescription = "Skip Set",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else if (completedSet > 0) {
                    // Save Exercise Button when timer is not running and at least one set is completed
                BottomAppBar(
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                    coroutineScope.launch {
                                saveExerciseSession()
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text("Save Exercise")
                            }
                        }
                    }
                }
            }
            

        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display exercise details
            exerciseWithDetails?.let { ex ->
                val we = ex.workoutExercise
                // Display GIF and exercise details in a row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left side - GIF
                    if (ex.exercise.gifUrl.isNotEmpty()) {
                        ExerciseGif(
                            gifPath = ex.exercise.gifUrl,
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    // Right side - Exercise details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailItem("Muscle Group", ex.exercise.muscle)
                        DetailItem("Muscles", Converter().fromString(ex.exercise.parts).joinToString(", "))
                        DetailItem("Difficulty", ex.exercise.difficulty)
                    }
                }

                // Display sets with weight and reps
                for (set in 1..we.sets) {
                        val animatedBorder by animateColorAsState(if (activeSetIndex == set && isTimerRunning && !isBreakRunning) Color.Green else Color.Gray)
                        val elevation = if (activeSetIndex == set && isTimerRunning) 8.dp else 2.dp

                    Card(
                        modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = animatedBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .shadow(
                                elevation = elevation,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = MaterialTheme.colorScheme.primary
                            ),
                        elevation = CardDefaults.cardElevation(elevation),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                set <= completedSet -> MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.9f
                                )

                                set == activeSetIndex && isTimerRunning -> MaterialTheme.colorScheme.background
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(
                                        brush = when {
                                            set <= completedSet -> Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                                )
                                            )
                                            set == activeSetIndex && isTimerRunning -> {
                                                val infiniteTransition = rememberInfiniteTransition(label = "gradient")
                                                val translateX by infiniteTransition.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = 1000f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(2000, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Restart
                                                    ),
                                                    label = "gradientTranslation"
                                                )
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.surface,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    ),
                                                    startX = translateX,
                                                    endX = translateX + 1000f
                                                )
                                            }
                                            else -> Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.background,
                                                    MaterialTheme.colorScheme.background
                                                )
                                            )
                                        }
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Set: $set",
                                style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        set <= completedSet -> MaterialTheme.colorScheme.onSurface
                                        set == activeSetIndex && isTimerRunning -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.weight(1f)
                            )
                            if (we.weight != 0 && !ex.exercise.useTime) {
                                if (showWeightPicker && editingSetIndex == set) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showWeightPicker = false
                                            editingSetIndex = null
                                        },
                                        title = {
                                            Text(
                                                "Select Weight",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        text = {
                                            Box(
                                                modifier = Modifier
                                                    .height(215.dp)
                                                    .width(260.dp)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center // Center the NumberPicker
                                            ) {
                                                NumberPicker(
                                                    value = setWeights[set] ?: we.weight,
                                                    range = 0..200,
                                                    onValueChange = { weight ->
                                                        for (i in set..we.sets) {
                                                            setWeights[i] = weight
                                                        }
                                                    },
                                                    unit = "Kg"
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showWeightPicker = false
                                                editingSetIndex = null
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.background(Color.Transparent)
                                    )
                                }

                                Text(
                                    text = "${setWeights[set] ?: we.weight} Kg",
                                    style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showWeightPicker = true
                                            editingSetIndex = set
                                        }
                                        .padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )

                            }


                            if (we.reps < 50) {
                                if (showRepsPicker && editingSetIndex == set) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showRepsPicker = false
                                            editingSetIndex = null
                                        },
                                        title = {
                                            Text(
                                                "Select Repetitions",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        text = {
                                            Box(
                                                modifier = Modifier
                                                    .height(215.dp)
                                                    .width(260.dp)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center // Center the NumberPicker
                                            ) {
                                                NumberPicker(
                                                    value = setReps[set] ?: we.reps,
                                                    range = 0..50,
                                                    onValueChange = { reps ->
                                                        for (i in set..we.sets) {
                                                            setReps[i] = reps
                                                        }
                                                    },
                                                    unit = "reps"
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showRepsPicker = false
                                                editingSetIndex = null
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.background(Color.Transparent)
                                    )
                                }
                                Text(
                                    text = "${setReps[set] ?: we.reps} Reps",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showRepsPicker = true
                                            editingSetIndex = set
                                        }
                                        .padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {

                                val timeInSeconds = setReps[set]?.minus(1000) ?: 60
                                var minutes by remember { mutableIntStateOf(timeInSeconds / 60) }
                                var seconds by remember { mutableIntStateOf(timeInSeconds % 60) }

                                if (showRepsPicker && editingSetIndex == set) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showRepsPicker = false
                                            editingSetIndex = null
                                        },
                                        title = {
                                            Text(
                                                "Select Time mm:ss",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(215.dp)
                                                    .width(260.dp)
                                            ) {
                                                NumberPicker(
                                                    value = minutes,
                                                    range = 0..59,
                                                    onValueChange = { newMinutes ->
                                                        minutes = newMinutes
                                                        for (i in set..we.sets) {
                                                            setReps[i] = (newMinutes * 60 + seconds) + 1000
                                                        }
                                                    },
                                                    unit = ""
                                                )
                                                NumberPicker(
                                                    value = seconds,
                                                    range = 0..59,
                                                    onValueChange = { newSeconds ->
                                                        seconds = newSeconds
                                                        for (i in set..we.sets) {
                                                            setReps[i] = (minutes * 60 + newSeconds) + 1000
                                                        }
                                                    },
                                                    unit = ""
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showRepsPicker = false
                                                editingSetIndex = null
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.background(Color.Transparent)
                                    )
                                }
                                Text(
                                        text = String.format(
                                            "%02d:%02d",
                                            minutes,
                                            seconds
                                        ), // Display as mm:ss
                                        color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showRepsPicker = true
                                            editingSetIndex = set
                                        }
                                        .padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                                                            // Action buttons area - always present for consistent alignment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    // Show play button for next set to complete
                                    completedSet == (set - 1) && !isTimerRunning -> {
                                        IconButton(onClick = { startTimer(set) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Start Set",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    // Show delete button only for the last set, if not completed, and not the active set during timer
                                    set == we.sets && set > completedSet && !(isTimerRunning && activeSetIndex == set) -> {
                                        IconButton(
                                            onClick = {
                                                setWeights.remove(set)
                                                setReps.remove(set)
                                                // Update the exerciseWithDetails with the new workoutExercise
                                                exerciseWithDetails = exerciseWithDetails?.copy(
                                                    workoutExercise = we.copy(sets = we.sets - 1)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.minus_icon),
                                                contentDescription = "Delete Set",
                                                tint = Color.Red,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    // Invisible placeholder for consistent alignment
                                    else -> {
                                        Spacer(modifier = Modifier.size(48.dp))
                                    }
                                }
                            }
                            }
                        }
                        // Add break indicator after each set except the last one
                        if (set != we.sets && isBreakRunning && set == completedSet) {
                            BreakIndicatorBar()
                        }

                    }

                    // Add Set Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = {
                                val newSet = we.sets + 1
                                setWeights[newSet] = we.weight
                                setReps[newSet] = we.reps
                                // Update the exerciseWithDetails with the new workoutExercise
                                exerciseWithDetails = exerciseWithDetails?.copy(
                                    workoutExercise = we.copy(sets = newSet)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(start = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.plus_icon),
                                contentDescription = "Add Set",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Add time selectors row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Set time selector - only show for exercises with reps
                        if (we.reps < 50) {
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(end = 8.dp)
                                    .clickable { showSetTimePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Set",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = String.format(
                                            "%02d:%02d",
                                            setTimeReps / 60,
                                            setTimeReps % 60
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Break time selector
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { showBreakTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Break",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(
                                        "%02d:%02d",
                                        breakTime / 60,
                                        breakTime % 60
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Add Track Muscle Soreness card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Track Muscle Soreness",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Checkbox(
                                    checked = !defaultSorenessValues,
                                    onCheckedChange = { checked ->
                                        defaultSorenessValues = !checked
                                        if (checked) {
                                            // Reset to default values
                                            eccentricFactor = 1.0f
                                            noveltyFactor = 5
                                            adaptationLevel = 5
                                            rpe = 5
                                            subjectiveSoreness = 5
                                        }
                                    }
                                )
                            }

                            if (!defaultSorenessValues) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Exercise Factors section
                                Text(
                                    "Exercise Factors",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Eccentric Factor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Eccentric Factor (1.0-2.0)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showInfoDialog = "eccentric" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Eccentric Factor Info",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = eccentricFactor,
                                        onValueChange = { 
                                            eccentricFactor = it
                                            vibrateOnValueChange()
                                        },
                                        valueRange = 1f..2f,
                                        steps = 9,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format("%.1f", eccentricFactor),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Novelty Factor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Novelty Factor (0-10)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showInfoDialog = "novelty" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Novelty Factor Info",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                SliderWithLabel(
                                    value = noveltyFactor.toFloat(),
                                    onValueChange = { 
                                        noveltyFactor = it.toInt()
                                        vibrateOnValueChange()
                                    },
                                    label = "",
                                    valueRange = 0f..10f,
                                    steps = 10
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Adaptation Level
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Adaptation Level (0-10)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showInfoDialog = "adaptation" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Adaptation Level Info",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                SliderWithLabel(
                                    value = adaptationLevel.toFloat(),
                                    onValueChange = { 
                                        adaptationLevel = it.toInt()
                                        vibrateOnValueChange()
                                    },
                                    label = "",
                                    valueRange = 0f..10f,
                                    steps = 10
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Perceived Exertion & Soreness section
                                Text(
                                    "Perceived Exertion & Soreness",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // RPE
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Rate of Perceived Exertion (1-10)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showInfoDialog = "rpe" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "RPE Info",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                SliderWithLabel(
                                    value = rpe.toFloat(),
                                    onValueChange = { 
                                        rpe = it.toInt()
                                        vibrateOnValueChange()
                                    },
                                    label = "",
                                    valueRange = 1f..10f,
                                    steps = 9
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Subjective Soreness
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Subjective Soreness (1-10)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showInfoDialog = "soreness" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Soreness Info",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                SliderWithLabel(
                                    value = subjectiveSoreness.toFloat(),
                                    onValueChange = { 
                                        subjectiveSoreness = it.toInt()
                                        vibrateOnValueChange()
                                    },
                                    label = "",
                                    valueRange = 1f..10f,
                                    steps = 9
                                )
                            }
                        }
                    }
                }
            }

            // Add set time picker dialog
            if (showSetTimePicker) {
                AlertDialog(
                    onDismissRequest = { showSetTimePicker = false },
                    title = {
                        Text(
                            "Select Set Time",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            NumberPicker(
                                value = setTimeReps / 60,
                                range = 0..5,
                                onValueChange = { newMinutes ->
                                    setTimeReps = (newMinutes * 60) + (setTimeReps % 60)
                                },
                                unit = "m"
                            )
                            NumberPicker(
                                value = setTimeReps % 60,
                                range = 0..59,
                                onValueChange = { newSeconds ->
                                    setTimeReps = (setTimeReps / 60 * 60) + newSeconds
                                },
                                unit = "s"
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSetTimePicker = false }) {
                            Text("OK")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.background(Color.Transparent)
                )
            }

            // Add break time picker dialog
            if (showBreakTimePicker) {
                AlertDialog(
                    onDismissRequest = { showBreakTimePicker = false },
                    title = {
                        Text(
                            "Select Break Time",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            NumberPicker(
                                value = breakTime / 60,
                                range = 0..5,
                                onValueChange = { newMinutes ->
                                    breakTime = (newMinutes * 60) + (breakTime % 60)
                                },
                                unit = "m"
                            )
                            NumberPicker(
                                value = breakTime % 60,
                                range = 0..59,
                                onValueChange = { newSeconds ->
                                    breakTime = (breakTime / 60 * 60) + newSeconds
                                },
                                unit = "s"
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBreakTimePicker = false }) {
                            Text("OK")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.background(Color.Transparent)
                )
            }

            // Add Info Dialog
            if (showInfoDialog != null) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = null },
                    title = {
                        Text(
                            text = when (showInfoDialog) {
                                "eccentric" -> "Eccentric Factor"
                                "novelty" -> "Novelty Factor"
                                "adaptation" -> "Adaptation Level"
                                "rpe" -> "Rate of Perceived Exertion"
                                "soreness" -> "Subjective Soreness"
                                else -> ""
                            }
                        )
                    },
                    text = {
                        Text(
                            text = when (showInfoDialog) {
                                "eccentric" -> "The degree of emphasis on the lowering (eccentric) phase of the movement. Higher values indicate slower, more controlled negatives."
                                "novelty" -> "How new or different this exercise is from your usual routine. Higher values indicate more novel movements that may cause more soreness."
                                "adaptation" -> "How well adapted your body is to this exercise. Higher values indicate better adaptation and potentially less soreness."
                                "rpe" -> "How hard the exercise felt. 1 = Very easy, 5 = Moderate effort, 10 = Maximum effort possible."
                                "soreness" -> "Current muscle soreness level. 1 = No soreness, 5 = Moderate discomfort, 10 = Severe pain/inability to move."
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = null }) {
                            Text("OK")
                        }
                    }
                )
            }

                // Show loading or error message
            if (exerciseWithDetails == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Save Notification
            if (showSaveNotification) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    showSaveNotification = false
                    navController.popBackStack()
                }
            }
        }
    }
}

/**
 * Composable function for a slider with a label
 */
@Composable
private fun SliderWithLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column {
        Text(label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (label.contains("Protein")) {
                    "${value.toInt()}g"
                } else {
                    value.toInt().toString()
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun BreakIndicatorBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                color = Color.Red,
                shape = RoundedCornerShape(1.dp)
            )
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProgressBarWithBall(progress: Float, isBreak: Boolean, modifier: Modifier = Modifier) {
    // Safety check to prevent NaN or infinite values
    val safeProgress = when {
        progress.isNaN() || progress.isInfinite() -> 0f
        progress < 0f -> 0f
        progress > 1f -> 1f
        else -> progress
    }
    
    val barColor = if (isBreak) Color.Red else Color.Green
    val ballColor = barColor
    val ballDiameter = 18.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val ballDiameterPx = with(density) { ballDiameter.toPx() }

        // Progress bar
        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.CenterStart),
            color = barColor
        )
        
        // Ball positioned using fractional width and alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Spacer(modifier = Modifier.fillMaxWidth(safeProgress))
            Box(
                modifier = Modifier
                    .size(ballDiameter)
                    .shadow(8.dp, shape = CircleShape, ambientColor = ballColor, spotColor = ballColor)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(ballColor, ballColor.copy(alpha = 0.7f), Color.Transparent),
                            center = Offset(ballDiameterPx / 2, ballDiameterPx / 2),
                            radius = ballDiameterPx * 0.7f
                        ),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}