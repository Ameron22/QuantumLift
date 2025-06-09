package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.CrossRefWorkoutExercise
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseToWorkoutScreen(
    workoutId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val coroutineScope = rememberCoroutineScope()

    var exercises by remember { mutableStateOf<List<EntityExercise>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showNumberPicker by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<EntityExercise?>(null) }
    var sets by remember { mutableStateOf(3) }
    var reps by remember { mutableStateOf(12) }
    var weight by remember { mutableStateOf(0) }

    // Create a single interaction source for all cards
    val interactionSource = remember { MutableInteractionSource() }

    // Load all exercises
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val allExercises = dao.getAllExercises()
                withContext(Dispatchers.Main) {
                    exercises = allExercises
                }
            }
        } catch (e: Exception) {
            Log.e("AddExerciseToWorkoutScreen", "Error loading exercises: ${e.message}")
            e.printStackTrace()
        }
    }

    // Filter exercises based on search query
    val filteredExercises = exercises.filter { exercise ->
        exercise.name.contains(searchQuery, ignoreCase = true)
    }

    // Add LaunchedEffect to check returnTo state
    LaunchedEffect(Unit) {
        println("AddExerciseToWorkoutScreen: Checking returnTo state")
        val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
        println("AddExerciseToWorkoutScreen: Initial returnTo value: $returnTo")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Exercise",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search exercises...") },
                singleLine = true
            )

            // Exercise list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises) { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                selectedExercise = exercise
                                showNumberPicker = true
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
                            // Display GIF if available
                            if (exercise.gifUrl.isNotEmpty()) {
                                ExerciseGif(
                                    gifPath = exercise.gifUrl,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exercise.muscle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            selectedExercise?.let { exercise ->
                                // Check if we should return to workout creation
                                val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
                                if (returnTo == "workout_creation") {
                                    println("AddExerciseToWorkoutScreen: Creating new exercise with values - sets: $sets, reps: $reps, weight: $weight")
                                    val newExercise = Exercise(
                                        name = exercise.name,
                                        sets = sets,
                                        weight = weight,
                                        reps = reps,
                                        muscle = exercise.muscle,
                                        part = exercise.part
                                    )
                                    println("AddExerciseToWorkoutScreen: Setting newExercise in savedStateHandle")
                                    navController.currentBackStackEntry?.savedStateHandle?.set("newExercise", newExercise)
                                    println("AddExerciseToWorkoutScreen: Popping back stack")
                                    navController.popBackStack()
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                // Check if the exercise is already in the workout
                                                val existingCrossRef = dao.getWorkoutExerciseCrossRef(workoutId, exercise.id)
                                                if (existingCrossRef == null) {
                                                    // Add exercise to workout
                                                    val crossRef = CrossRefWorkoutExercise(
                                                        workoutId = workoutId,
                                                        exerciseId = exercise.id
                                                    )
                                                    dao.insertWorkoutExerciseCrossRef(crossRef)
                                                } else {
                                                    println("AddExerciseToWorkoutScreen: Exercise already in workout")
                                                }
                                            }
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            Log.e("AddExerciseToWorkoutScreen", "Error adding exercise: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AddExerciseToWorkoutScreen", "Error adding exercise: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedExercise != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add Exercise")
            }
        }

        // Number picker dialog
        if (showNumberPicker && selectedExercise != null) {
            AlertDialog(
                onDismissRequest = { showNumberPicker = false },
                title = {
                    Text(
                        "Configure ${selectedExercise?.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sets picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sets")
                            NumberPicker(
                                value = sets,
                                range = 1..10,
                                onValueChange = { sets = it },
                                unit = ""
                            )
                        }

                        // Reps picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reps")
                            NumberPicker(
                                value = reps,
                                range = 1..50,
                                onValueChange = { reps = it },
                                unit = ""
                            )
                        }

                        // Weight picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weight (kg)")
                            NumberPicker(
                                value = weight,
                                range = 0..200,
                                onValueChange = { weight = it },
                                unit = ""
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedExercise?.let { exercise ->
                                println("AddExerciseToWorkoutScreen: Selected exercise: ${exercise.name}")
                                // Check if we should return to workout creation
                                val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
                                println("AddExerciseToWorkoutScreen: Return to: $returnTo")
                                if (returnTo == "workout_creation") {
                                    println("AddExerciseToWorkoutScreen: Creating new exercise with values - sets: $sets, reps: $reps, weight: $weight")
                                    val newExercise = Exercise(
                                        name = exercise.name,
                                        sets = sets,
                                        weight = weight,
                                        reps = reps,
                                        muscle = exercise.muscle,
                                        part = exercise.part
                                    )
                                    println("AddExerciseToWorkoutScreen: Setting newExercise in savedStateHandle")
                                    navController.previousBackStackEntry?.savedStateHandle?.set("newExercise", newExercise)
                                    println("AddExerciseToWorkoutScreen: Popping back stack")
                                    navController.popBackStack()
                                } else {
                                    println("AddExerciseToWorkoutScreen: Adding exercise to workout")
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                // Check if the exercise is already in the workout
                                                val existingCrossRef = dao.getWorkoutExerciseCrossRef(workoutId, exercise.id)
                                                if (existingCrossRef == null) {
                                                    // Add exercise to workout
                                                    val crossRef = CrossRefWorkoutExercise(
                                                        workoutId = workoutId,
                                                        exerciseId = exercise.id
                                                    )
                                                    dao.insertWorkoutExerciseCrossRef(crossRef)
                                                } else {
                                                    println("AddExerciseToWorkoutScreen: Exercise already in workout")
                                                }
                                            }
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            Log.e("AddExerciseToWorkoutScreen", "Error adding exercise: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            showNumberPicker = false
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNumberPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 