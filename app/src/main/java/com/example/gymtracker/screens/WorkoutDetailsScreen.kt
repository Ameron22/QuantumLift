package com.example.gymtracker.screens

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.components.SliderWithLabel
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutWithExercises
import com.example.gymtracker.data.RecoveryFactors
import com.example.gymtracker.data.TempRecoveryFactors
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel

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
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutStarted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSaveNotification by remember { mutableStateOf(false) }
    
    // Recovery Factors states from ViewModel
    val recoveryFactors by viewModel.recoveryFactors.collectAsState()
    val hasSetRecoveryFactors by viewModel.hasSetRecoveryFactors.collectAsState()
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var currentRecoveryFactor by remember { mutableStateOf("") }

    // Update the LaunchedEffect for loading workout data
    LaunchedEffect(workoutId) {
        try {
            Log.d("WorkoutDetailsScreen", "Loading workout data for ID: $workoutId")
            
            val workoutData = dao.getWorkoutWithExercises(workoutId)
            Log.d("WorkoutDetailsScreen", "Workout data loaded: ${workoutData?.size} items")
            
            if (workoutData.isNullOrEmpty()) {
                Log.e("WorkoutDetailsScreen", "No workout data found for ID: $workoutId")
            } else {
                workoutWithExercises = workoutData
                val workoutName = workoutData.firstOrNull()?.workout?.name ?: "Unknown Workout"
                viewModel.initializeWorkoutSession(workoutId, workoutName)
                Log.d("WorkoutDetailsScreen", "Workout name: $workoutName")
                Log.d("WorkoutDetailsScreen", "Number of exercises: ${workoutData.firstOrNull()?.exercises?.size}")
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Add loading state UI
    if (workoutWithExercises == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Function to start the workout session
    fun startWorkoutSession(exId: Int) {
        viewModel.startWorkoutSession()
        workoutStarted = true  // Set workoutStarted to true
        val sessionId = viewModel.workoutSession.value?.sessionId
        if (sessionId != null) {
                navController.navigate("exerciseDetails/${exId}/${sessionId}")
        } else {
            Log.e("WorkoutDetailsScreen", "Failed to start workout session: sessionId is null")
        }
    }

    // Function to end the workout session
    fun endWorkoutSession() {
        val session = viewModel.workoutSession.value
        Log.d("WorkoutDetailsScreen", "Ending workout session - Session: $session, Started: ${session?.isStarted}, WorkoutStarted: $workoutStarted")
        
        if (session == null) {
            // If no session exists, just reset and go back
            viewModel.resetWorkoutSession()
            navController.popBackStack()
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val duration = (currentTime - session.startTime) / 1000
                Log.d("WorkoutDetailsScreen", "Saving workout session - ID: ${session.workoutId}, Name: ${session.workoutName}, Duration: $duration")
                
                // Create and save the workout session
                val workoutSession = SessionWorkoutEntity(
                    sessionId = session.sessionId,  // Use the session ID from viewModel
                    workoutId = session.workoutId,
                    startTime = session.startTime,
                    duration = duration,
                    workoutName = session.workoutName
                )

                // First, check if session already exists
                val existingSession = dao.getWorkoutSession(session.sessionId)
                if (existingSession == null) {
                    Log.d("WorkoutDetailsScreen", "Inserting new workout session")
                    dao.insertWorkoutSession(workoutSession)
                } else {
                    Log.d("WorkoutDetailsScreen", "Updating existing workout session")
                    dao.updateWorkoutSession(workoutSession)
                }
                
                // Save recovery factors if set
                if (hasSetRecoveryFactors) {
                    val recoveryFactorsObj = RecoveryFactors(
                        sleepQuality = recoveryFactors.sleepQuality,
                        proteinIntake = recoveryFactors.proteinIntake,
                        hydration = recoveryFactors.hydration,
                        stressLevel = recoveryFactors.stressLevel
                    )
                    val recoveryFactorsJson = Gson().toJson(recoveryFactorsObj)
                    dao.updateWorkoutSessionWithRecovery(session.sessionId, duration, recoveryFactorsJson)
                }
                
                Log.d("WorkoutDetailsScreen", "Workout session saved successfully with ID: ${session.sessionId}")
                withContext(Dispatchers.Main) {
                    startTimeWorkout = session.startTime // Set the start time for duration display
                    showSaveNotification = true
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error saving workout session: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Handle error (show message to user)
                    viewModel.resetWorkoutSession()
                    navController.popBackStack()
                }
            }
        }
    }

    // Handle back navigation
    BackHandler {
        if (workoutStarted) {
            endWorkoutSession()
        } else {
            viewModel.resetWorkoutSession()
            navController.popBackStack()
        }
    }

    // Update the Recovery Factor Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(currentRecoveryFactor) },
            text = {
                when {
                    currentRecoveryFactor.contains("Protein") -> {
                        SliderWithLabel(
                            value = recoveryFactors.proteinIntake.toFloat(),
                            onValueChange = { 
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

    // Update the Recovery Factors buttons section
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
                        ),
                        enabled = !showSaveNotification // Disable button while showing notification
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
                    .padding(16.dp)
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
                        Text(
                            text = "Recovery Factors",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save Notification overlay
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