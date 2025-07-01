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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.WorkoutExerciseWithDetails
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseToWorkoutScreen(
    workoutId: Int,
    navController: NavController,
    detailsViewModel: WorkoutDetailsViewModel = viewModel()
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
    var weight by remember { mutableStateOf(5) }
    var minutes by remember { mutableStateOf(1) }
    var seconds by remember { mutableStateOf(0) }
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
            Column {
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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        Row {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                            IconButton(onClick = { showFilterDialog = true }) {
                                Text(
                                    text = "Filter",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) { }
                
                // Active filters in top bar
                if (selectedMuscleGroup != null || selectedDifficulty != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
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
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
                                // Set appropriate default values based on exercise type
                                if (exercise.useTime) {
                                    minutes = 1
                                    seconds = 0
                                } else {
                                    reps = 12
                                    weight = 5
                                }
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Alert Dialog for number pickers
            if (showNumberPicker && selectedExercise != null) {
                AlertDialog(
                    onDismissRequest = { showNumberPicker = false },
                    title = { Text(selectedExercise!!.name) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Number pickers section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sets picker
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sets", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    NumberPicker(
                                        value = sets,
                                        onValueChange = { sets = it },
                                        range = 1..10
                                    )
                                }

                                if (selectedExercise!!.useTime) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Minutes picker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Minutes", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NumberPicker(
                                            value = minutes,
                                            onValueChange = { minutes = it },
                                            range = 0..59
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Seconds picker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Seconds", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NumberPicker(
                                            value = seconds,
                                            onValueChange = { seconds = it },
                                            range = 0..59
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Weight picker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Weight (kg)", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NumberPicker(
                                            value = weight,
                                            onValueChange = { weight = it },
                                            range = 0..500,
                                            unit = "kg"
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Reps picker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reps", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NumberPicker(
                                            value = reps,
                                            onValueChange = { reps = it },
                                            range = 1..50
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val entityExercise = dao.getExerciseById(selectedExercise!!.id)
                                    if (entityExercise != null) {
                                        val workoutExercise = WorkoutExercise(
                                            exerciseId = selectedExercise!!.id,
                                            workoutId = workoutId,
                                            sets = sets,
                                            reps = if (selectedExercise!!.useTime) (minutes * 60 + seconds) + 1000 else reps,
                                            weight = weight,
                                            order = 0 // order will be set when saving workout
                                        )
                                        val combined = WorkoutExerciseWithDetails(
                                            workoutExercise = workoutExercise,
                                            entityExercise = entityExercise
                                        )
                                        
                                        withContext(Dispatchers.IO) {
                                            // Get the next order for this workout
                                            val existingExercises = dao.getWorkoutExercisesForWorkout(workoutId)
                                            val nextOrder = existingExercises.size
                                            
                                            // Update the order
                                            val updatedWorkoutExercise = workoutExercise.copy(order = nextOrder)
                                            val updatedCombined = combined.copy(workoutExercise = updatedWorkoutExercise)
                                            
                                            // Save to database
                                            dao.insertWorkoutExercise(updatedWorkoutExercise)
                                            
                                            withContext(Dispatchers.Main) {
                                                // Add to ViewModel for UI updates
                                                detailsViewModel.addExercise(updatedCombined)
                                                Log.d("AddExerciseToWorkoutScreen", "Added exercise to database and DetailsViewModel: $updatedCombined")
                                            }
                                        }
                                        
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("newExerciseId", combined.workoutExercise.exerciseId)
                                    } else {
                                        Log.e("AddExerciseToWorkoutScreen", "EntityExercise not found for id: ${selectedExercise!!.id}")
                                    }
                                    showNumberPicker = false
                                    navController.popBackStack()
                                }
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
} 