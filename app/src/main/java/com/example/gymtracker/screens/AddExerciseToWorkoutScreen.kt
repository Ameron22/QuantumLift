package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }

    // Create a single interaction source for all cards
    val interactionSource = remember { MutableInteractionSource() }

    // Load all exercises
    LaunchedEffect(Unit) {
        try {
            Log.d("AddExerciseToWorkoutScreen", "Starting to load exercises")
            withContext(Dispatchers.IO) {
                val allExercises = dao.getAllExercises()
                Log.d("AddExerciseToWorkoutScreen", "Loaded ${allExercises.size} exercises from database")
                withContext(Dispatchers.Main) {
                    exercises = allExercises
                    Log.d("AddExerciseToWorkoutScreen", "Updated UI with exercises")
                }
            }
        } catch (e: Exception) {
            Log.e("AddExerciseToWorkoutScreen", "Error loading exercises: ${e.message}")
            e.printStackTrace()
        }
    }

    // Get unique muscle groups and difficulties
    val muscleGroups = exercises.map { it.muscle }.distinct().sorted()
    val difficulties = listOf("Beginner", "Intermediate", "Advanced")

    // Filter exercises based on search query and filters
    val filteredExercises = exercises.filter { exercise ->
        val matchesSearch = exercise.name.contains(searchQuery, ignoreCase = true)
        val matchesMuscle = selectedMuscleGroup == null || exercise.muscle == selectedMuscleGroup
        val matchesDifficulty = selectedDifficulty == null || exercise.difficulty == selectedDifficulty
        matchesSearch && matchesMuscle && matchesDifficulty
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
            // Search bar with filter button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search exercises...") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Text(
                            text = "Filter",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            // Active filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedMuscleGroup != null) {
                    FilterChip(
                        selected = true,
                        onClick = { selectedMuscleGroup = null },
                        label = { Text(selectedMuscleGroup!!) }
                    )
                }
                if (selectedDifficulty != null) {
                    FilterChip(
                        selected = true,
                        onClick = { selectedDifficulty = null },
                        label = { Text(selectedDifficulty!!) }
                    )
                }
            }

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Display GIF if available
                            if (exercise.gifUrl.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    ExerciseGif(
                                        gifPath = exercise.gifUrl,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercise.muscle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = exercise.difficulty,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when (exercise.difficulty) {
                                            "Beginner" -> Color(0xFF4CAF50) // Green
                                            "Intermediate" -> Color(0xFFFFA000) // Orange
                                            "Advanced" -> Color(0xFFF44336) // Red
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Exercises") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Muscle Group Filter
                    Text(
                        text = "Muscle Group",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMuscleGroup == null,
                            onClick = { selectedMuscleGroup = null },
                            label = { Text("All") }
                        )
                        muscleGroups.forEach { muscle ->
                            FilterChip(
                                selected = selectedMuscleGroup == muscle,
                                onClick = { selectedMuscleGroup = muscle },
                                label = { Text(muscle) }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Difficulty Level Filter
                    Text(
                        text = "Difficulty Level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDifficulty == null,
                            onClick = { selectedDifficulty = null },
                            label = { Text("All") }
                        )
                        difficulties.forEach { difficulty ->
                            FilterChip(
                                selected = selectedDifficulty == difficulty,
                                onClick = { selectedDifficulty = difficulty },
                                label = { 
                                    Text(
                                        text = difficulty,
                                        color = when (difficulty) {
                                            "Beginner" -> Color(0xFF4CAF50)
                                            "Intermediate" -> Color(0xFFFFA000)
                                            "Advanced" -> Color(0xFFF44336)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Number Picker Dialog
    if (showNumberPicker && selectedExercise != null) {
        AlertDialog(
            onDismissRequest = { showNumberPicker = false },
            title = { Text("Set Exercise Details") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sets
                    Text("Sets")
                    NumberPicker(
                        value = sets,
                        range = 1..10,
                        onValueChange = { sets = it },
                        unit = "sets"
                    )

                    // Reps
                    Text("Reps")
                    NumberPicker(
                        value = reps,
                        range = 1..50,
                        onValueChange = { reps = it },
                        unit = "reps"
                    )

                    // Weight
                    Text("Weight")
                    NumberPicker(
                        value = weight,
                        range = 0..200,
                        onValueChange = { weight = it },
                        unit = "kg"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                selectedExercise?.let { exercise ->
                                    // Check if we should return to workout creation
                                    val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
                                    if (returnTo == "workout_creation") {
                                        val newExercise = Exercise(
                                            name = exercise.name,
                                            sets = sets,
                                            weight = weight,
                                            reps = reps,
                                            muscle = exercise.muscle,
                                            part = exercise.part,
                                            gifUrl = exercise.gifUrl
                                        )
                                        println("AddExerciseToWorkoutScreen: Setting newExercise in savedStateHandle: ${newExercise.name}")
                                        navController.previousBackStackEntry?.savedStateHandle?.set("newExercise", newExercise)
                                        println("AddExerciseToWorkoutScreen: Popping back stack")
                                        navController.popBackStack()
                                    } else {
                                        // Add exercise to workout
                                        try {
                                            withContext(Dispatchers.IO) {
                                                // Check if the exercise is already in the workout
                                                val existingCrossRef = dao.getWorkoutExerciseCrossRef(workoutId, exercise.id)
                                                if (existingCrossRef == null) {
                                                    val crossRef = CrossRefWorkoutExercise(
                                                        workoutId = workoutId,
                                                        exerciseId = exercise.id
                                                    )
                                                    dao.insertWorkoutExerciseCrossRef(crossRef)
                                                    Log.d("AddExerciseToWorkoutScreen", "Added exercise ${exercise.name} to workout $workoutId")
                                                } else {
                                                    Log.d("AddExerciseToWorkoutScreen", "Exercise ${exercise.name} already in workout $workoutId")
                                                }
                                            }
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            Log.e("AddExerciseToWorkoutScreen", "Error adding exercise to workout: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AddExerciseToWorkoutScreen", "Error saving exercise: ${e.message}")
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