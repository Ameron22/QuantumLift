package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionEntityExercise
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

    // Timer related states
    var activeSetIndex by remember { mutableStateOf<Int?>(null) }
    var remainingTime by remember { mutableIntStateOf(0) }
    var exerciseTime by remember { mutableIntStateOf(0) }
    var breakTime by remember { mutableIntStateOf(10) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isBreakRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var setTimeReps = 60// Default time in seconds for reps

    // Map to store weights for each set (set number to weight)
    val setWeights = remember { mutableStateMapOf<Int, Int>() }
    // Map to store repetitions for each set (set number to reps)
    val setReps = remember { mutableStateMapOf<Int, Int>() }
    // Track which set is currently being edited (1-indexed)
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }


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

        // Convert repsOrTime and weight to lists
        val repsOrTimeList = (1..exercise.sets).map { setReps[it] ?: 0 } // Use setReps map
        val weightList = (1..exercise.sets).map { setWeights[it] ?: 0 } // Use setWeights map

        val exerciseSession = SessionEntityExercise(
            sessionId = workoutSessionId,
            exerciseId = exercise.id.toLong(),
            sets = exercise.sets,
            repsOrTime = repsOrTimeList, // List of reps or time for each set
            weight = weightList, // List of weights for each set
            muscleGroup = exercise.muscle,
            muscleParts = exercise.part,
            completedSets = exercise.sets,
            notes = ""
        )

        coroutineScope.launch(Dispatchers.IO) {
            dao.insertExerciseSession(exerciseSession)
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
                        if (activeSetIndex!! <= exercise?.sets ?: 0) {
                            isBreakRunning = false
                            activeSetIndex = activeSetIndex?.let { it + 1 }
                            if (activeSetIndex != null && activeSetIndex!! <= exercise?.sets ?: 0) {
                                remainingTime = exerciseTime
                            } else {
                                isTimerRunning = false
                                activeSetIndex = null
                                saveExerciseSession()
                            }
                        }else{
                            isTimerRunning = false
                            activeSetIndex = null
                            saveExerciseSession()
                        }
                    } else {
                        isBreakRunning = true
                        remainingTime = breakTime
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
                title = { Text(exercise?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
                        // Save Exercise Button
                        Button(
                            onClick = {
                                saveExerciseSession()
                                navController.popBackStack() // Navigate back after saving
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Save Exercise")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display exercise details
            exercise?.let { ex ->
                // Display sets with weight and reps
                for (set in 1..ex.sets) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // Distribute space evenly
                            verticalAlignment = Alignment.CenterVertically // Align items vertically in the center
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
                                                modifier = Modifier.fillMaxWidth(),
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
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                }

                                Text(
                                    text = "${setWeights[set] ?: ex.weight} Kg",
                                    style = MaterialTheme.typography.bodyLarge,
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
                                                modifier = Modifier.fillMaxWidth(),
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
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                                Text(
                                    text = "${setReps[set] ?: ex.reps} Reps",
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
                                                modifier = Modifier.fillMaxWidth()
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
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds), // Display as mm:ss
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
            } ?: run {
                // Show loading or error message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (exercise == null) "Loading..." else "Exercise not found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}