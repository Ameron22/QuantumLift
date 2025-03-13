package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionEntityExercise
import com.example.gymtracker.data.RecoveryFactors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(exerciseId: Int, workoutSessionId: Long, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var exercise by remember { mutableStateOf<EntityExercise?>(null) }
    var sessionId by remember { mutableStateOf<Long?>(null) }
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
    var breakTime by remember { mutableIntStateOf(5) } // Default break time in seconds
    var setTimeReps = 10// Default time in seconds for reps

    // Map to store weights for each set (set number to weight)
    val setWeights = remember { mutableStateMapOf<Int, Int>() }
    // Map to store repetitions for each set (set number to reps)
    val setReps = remember { mutableStateMapOf<Int, Int>() }
    // Track which set is currently being edited (1-indexed)
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }
    //Track which set is currently being executed
    var completedSet by remember { mutableStateOf<Int?>(0) }

    // New state variables for muscle soreness tracking
    var eccentricFactor by remember { mutableStateOf(1.0f) }
    var noveltyFactor by remember { mutableStateOf(5) }
    var adaptationLevel by remember { mutableStateOf(5) }
    var rpe by remember { mutableStateOf(5) }
    var subjectiveSoreness by remember { mutableStateOf(5) }
    var sleepQuality by remember { mutableStateOf(7) }
    var proteinIntake by remember { mutableStateOf(150) }
    var hydration by remember { mutableStateOf(7) }
    var stressLevel by remember { mutableStateOf(5) }
    var showSorenessDialog by remember { mutableStateOf(false) }

    // Fetch exercise data
    LaunchedEffect(exerciseId) {
        try {
            exercise = dao.getExerciseById(exerciseId)?.also { ex ->
                // Initialize weights for all sets with the exercise weight
                if (ex.weight != 0) {
                    for (set in 1..ex.sets) {
                        setWeights[set] = ex.weight
                    }
                }

                for (set in 1..ex.sets) {
                    setReps[set] = ex.reps
                }
            }
        } catch (e: Exception) {
            Log.e("ExerciseScreen", "Database error: ${e.message}")
        }
    }

    // Function to save exercise session with muscle soreness data
    fun saveExerciseSession() {
        val exercise = exercise ?: return

        // Convert repsOrTime and weight to lists
        val repsOrTimeList = (1..exercise.sets).map { setReps[it] ?: 0 }
        val weightList = (1..exercise.sets).map { setWeights[it] ?: 0 }

        val recoveryFactors = RecoveryFactors(
            sleepQuality = sleepQuality,
            proteinIntake = proteinIntake,
            hydration = hydration,
            stressLevel = stressLevel
        )

        val exerciseSession = SessionEntityExercise(
            sessionId = workoutSessionId,
            exerciseId = exercise.id.toLong(),
            sets = exercise.sets,
            repsOrTime = repsOrTimeList,
            weight = weightList,
            muscleGroup = exercise.muscle,
            muscleParts = exercise.part,
            completedSets = completedSet!!,
            notes = "",
            eccentricFactor = eccentricFactor,
            noveltyFactor = noveltyFactor,
            adaptationLevel = adaptationLevel,
            rpe = rpe,
            subjectiveSoreness = subjectiveSoreness,
            recoveryFactors = recoveryFactors
        )

        coroutineScope.launch(Dispatchers.IO) {
            dao.insertExerciseSession(exerciseSession)
            Log.d("ExerciseScreen", "Exercise session saved: $exerciseSession")
            showSaveNotification = true
        }
    }

    // Auto-dismiss notification after 2 seconds
    LaunchedEffect(showSaveNotification) {
        if (showSaveNotification) {
            delay(3000)
            showSaveNotification = false
            navController.popBackStack() // Navigate back after notification is shown
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
                        activeSetIndex = activeSetIndex?.let { it + 1 }
                        completedSet = completedSet!! + 1
                        if (activeSetIndex != null && activeSetIndex!! <= exercise?.sets ?: 0) {
                            remainingTime = exerciseTime
                        } else {
                            isTimerRunning = false
                            activeSetIndex = null
                            saveExerciseSession()
                            completedSet = completedSet!! + 1
                        }
                    } else {
                        if (activeSetIndex!! == exercise?.sets ?: 0) {
                            isTimerRunning = false
                            activeSetIndex = null
                            saveExerciseSession()
                            completedSet = completedSet!! + 1
                        }else{
                            isBreakRunning = true
                            remainingTime = breakTime
                        }
                    }
                }
            } else {
                delay(100)
            }
        }
    }

    // Function to start the timer for a specific set
    fun startTimer(setIndex: Int) {
        activeSetIndex = setIndex
        exerciseTime = if (exercise?.reps!! > 50) {
            exercise?.reps!! - 1000
        } else {
            setTimeReps
        }
        remainingTime = exerciseTime
        isTimerRunning = true
        isBreakRunning = false
        isPaused = false
    }


    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = exercise?.name ?: "Loading...",
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
            if (activeSetIndex != null) {
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
                                    text = "Set: ${activeSetIndex}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }

                            IconButton(
                                onClick = {
                                    isTimerRunning = false
                                    isBreakRunning = false
                                    remainingTime = 0
                                    activeSetIndex = null
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
            }   else {
                // Save Exercise Button
                BottomAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    //containerColor = MaterialTheme.colorScheme.primaryContainer,
                    //contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Save Exercise Button
                        Button(
                            onClick = {
                                saveExerciseSession()
                                navController.popBackStack() // Navigate back after saving
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text("Save Exercise")
                        }

                        // Track Muscle Soreness Button
                        Button(
                            onClick = { showSorenessDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text("Track Muscle Soreness")
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
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Display exercise details
                exercise?.let { ex ->
                    // Display sets with weight and reps
                    for (set in 1..ex.sets) {
                        val animatedBorder by animateColorAsState(if (activeSetIndex == set) Color.Green else Color.Gray)
                        val elevation = if (activeSetIndex == set) 8.dp else 2.dp

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
                                containerColor = if (set <= completedSet!!+1) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.background
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .background(
                                        brush = if (activeSetIndex == set) {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface,
                                                    MaterialTheme.colorScheme.surface
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
                                    modifier = Modifier.weight(1f) // Occupy equal space
                                )
                                if (ex.weight != 0) {
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
                                                        value = setWeights[set] ?: ex.weight,
                                                        range = 0..200,
                                                        onValueChange = { weight ->
                                                            setWeights[set] = weight
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
                                        text = "${setWeights[set] ?: ex.weight} Kg",
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


                                if (ex.reps < 50) {
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
                                                        value = setReps[set] ?: ex.reps,
                                                        range = 0..50,
                                                        onValueChange = { reps ->
                                                            setReps[set] = reps
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
                                        text = "${setReps[set] ?: ex.reps} Reps",
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
                                                            setReps[set] = (newMinutes * 60 + seconds) + 1000
                                                        },
                                                        unit = ""
                                                    )
                                                    NumberPicker(
                                                        value = seconds,
                                                        range = 0..59,
                                                        onValueChange = { newSeconds ->
                                                            seconds = newSeconds
                                                            setReps[set] = (minutes * 60 + newSeconds) + 1000
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
                                        text = String.format("%02d:%02d", minutes, seconds), // Display as mm:ss
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
                                if (completedSet == (set-1) && activeSetIndex == null) {
                                    IconButton(onClick = { startTimer(set) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Set",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add a button to show the soreness tracking dialog
            Button(
                onClick = { showSorenessDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Track Muscle Soreness")
            }

            // Show loading or error message
            if (exercise == null) {
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
                                text = "Exercise Saved",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${exercise?.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sets completed: $completedSet/${exercise?.sets}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Add muscle soreness tracking UI
            if (showSorenessDialog) {
                AlertDialog(
                    onDismissRequest = { showSorenessDialog = false },
                    title = { Text("Track Muscle Soreness") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Exercise-specific factors
                            Text("Exercise Factors", style = MaterialTheme.typography.titleMedium)
                            SliderWithLabel(
                                value = eccentricFactor,
                                onValueChange = { eccentricFactor = it },
                                label = "Eccentric Factor (1.0-2.0)",
                                valueRange = 1f..2f,
                                steps = 10
                            )
                            SliderWithLabel(
                                value = noveltyFactor.toFloat(),
                                onValueChange = { noveltyFactor = it.toInt() },
                                label = "Novelty Factor (0-10)",
                                valueRange = 0f..10f,
                                steps = 10
                            )
                            SliderWithLabel(
                                value = adaptationLevel.toFloat(),
                                onValueChange = { adaptationLevel = it.toInt() },
                                label = "Adaptation Level (0-10)",
                                valueRange = 0f..10f,
                                steps = 10
                            )

                            // Perceived exertion and soreness
                            Text("Perceived Exertion & Soreness", style = MaterialTheme.typography.titleMedium)
                            SliderWithLabel(
                                value = rpe.toFloat(),
                                onValueChange = { rpe = it.toInt() },
                                label = "Rate of Perceived Exertion (1-10)",
                                valueRange = 1f..10f,
                                steps = 9
                            )
                            SliderWithLabel(
                                value = subjectiveSoreness.toFloat(),
                                onValueChange = { subjectiveSoreness = it.toInt() },
                                label = "Subjective Soreness (1-10)",
                                valueRange = 1f..10f,
                                steps = 9
                            )

                            // Recovery factors
                            Text("Recovery Factors", style = MaterialTheme.typography.titleMedium)
                            SliderWithLabel(
                                value = sleepQuality.toFloat(),
                                onValueChange = { sleepQuality = it.toInt() },
                                label = "Sleep Quality (1-10)",
                                valueRange = 1f..10f,
                                steps = 9
                            )
                            SliderWithLabel(
                                value = proteinIntake.toFloat(),
                                onValueChange = { proteinIntake = it.toInt() },
                                label = "Protein Intake (g)",
                                valueRange = 0f..300f,
                                steps = 30
                            )
                            SliderWithLabel(
                                value = hydration.toFloat(),
                                onValueChange = { hydration = it.toInt() },
                                label = "Hydration Level (1-10)",
                                valueRange = 1f..10f,
                                steps = 9
                            )
                            SliderWithLabel(
                                value = stressLevel.toFloat(),
                                onValueChange = { stressLevel = it.toInt() },
                                label = "Stress Level (1-10)",
                                valueRange = 1f..10f,
                                steps = 9
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSorenessDialog = false
                                saveExerciseSession()
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSorenessDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
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