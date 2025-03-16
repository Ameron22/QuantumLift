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
import androidx.compose.material.icons.filled.Info
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
import com.example.gymtracker.components.SliderWithLabel
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import kotlinx.coroutines.withContext


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
    var breakTime by remember { mutableIntStateOf(120) } // Default break time in seconds (2 minutes)
    var setTimeReps by remember { mutableIntStateOf(120) } // Default time in seconds for reps (2 minutes)

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

    // Function to save exercise session
    fun saveExerciseSession() {
        val exercise = exercise ?: return

        // Ensure completedSet doesn't exceed total sets
        completedSet = minOf(completedSet, exercise.sets)

        Log.d("ExerciseScreen", "Saving exercise session - Exercise: ${exercise.name}, Completed Sets: $completedSet")

        // Convert repsOrTime and weight to lists
        val repsOrTimeList = (1..exercise.sets).map { setReps[it] ?: exercise.reps }
        val weightList = (1..exercise.sets).map { setWeights[it] ?: exercise.weight }

        Log.d("ExerciseScreen", "Session details - Reps: $repsOrTimeList, Weights: $weightList")

        val exerciseSession = SessionEntityExercise(
            sessionId = workoutSessionId,
            exerciseId = exercise.id.toLong(),
            sets = exercise.sets,
            repsOrTime = repsOrTimeList,
            weight = weightList,
            muscleGroup = exercise.muscle,
            muscleParts = exercise.part,
            completedSets = completedSet,
            notes = "",
            eccentricFactor = eccentricFactor,
            noveltyFactor = noveltyFactor,
            adaptationLevel = adaptationLevel,
            rpe = rpe,
            subjectiveSoreness = subjectiveSoreness
        )

        coroutineScope.launch(Dispatchers.IO) {
            try {
                dao.insertExerciseSession(exerciseSession)
                Log.d("ExerciseScreen", "Exercise session saved successfully: $exerciseSession")
                withContext(Dispatchers.Main) {
                    showSaveNotification = true
                }
            } catch (e: Exception) {
                Log.e("ExerciseScreen", "Error saving exercise session: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Could add error notification here
                }
            }
        }
    }

    // Auto-dismiss notification after 3 seconds
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
                        if (activeSetIndex != null && activeSetIndex!! <= exercise?.sets ?: 0) {
                            remainingTime = exerciseTime
                        } else {
                            isTimerRunning = false
                            activeSetIndex = null
                            completedSet = exercise?.sets ?: 0  // Set to total number of sets
                            saveExerciseSession()
                        }
                    } else {
                        completedSet += 1  // Increment completed sets after exercise time
                        if (activeSetIndex!! >= (exercise?.sets ?: 0)) {
                            isTimerRunning = false
                            activeSetIndex = null
                            completedSet = exercise?.sets ?: 0  // Ensure we mark all sets as completed
                            saveExerciseSession()
                        } else {
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
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTimerRunning) {
                    // Timer progress bar
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
                        color = if (isBreakRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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
                                text = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Pause/Resume button
                            IconButton(
                                onClick = { isPaused = !isPaused }
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.ArrowDropDown,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Save Exercise Button when timer is not running
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
                                    saveExerciseSession()
                                    navController.popBackStack()
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
                                containerColor = if (set <= completedSet + 1) {
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

                    // Add time selectors row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Set time selector - only show for exercises with reps
                        if (exercise?.reps!! < 50) {
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
                                        text = String.format("%02d:%02d", setTimeReps / 60, setTimeReps % 60),
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
                                    text = String.format("%02d:%02d", breakTime / 60, breakTime % 60),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Add Track Muscle Soreness button
                    Button(
                        onClick = { showSorenessDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Track Muscle Soreness")
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
                                    modifier = Modifier.size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Eccentric Factor Info",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            SliderWithLabel(
                                value = eccentricFactor,
                                onValueChange = { eccentricFactor = it },
                                label = "",
                                valueRange = 1f..2f,
                                steps = 10
                            )

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
                                    modifier = Modifier.size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                onValueChange = { noveltyFactor = it.toInt() },
                                label = "",
                                valueRange = 0f..10f,
                                steps = 10
                            )

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
                                    modifier = Modifier.size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                onValueChange = { adaptationLevel = it.toInt() },
                                label = "",
                                valueRange = 0f..10f,
                                steps = 10
                            )

                            // Perceived exertion and soreness
                            Text("Perceived Exertion & Soreness", style = MaterialTheme.typography.titleMedium)
                            
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
                                    modifier = Modifier.size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                onValueChange = { rpe = it.toInt() },
                                label = "",
                                valueRange = 1f..10f,
                                steps = 9
                            )

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
                                    modifier = Modifier.size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                onValueChange = { subjectiveSoreness = it.toInt() },
                                label = "",
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