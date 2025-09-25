package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.data.*
import com.example.gymtracker.services.WarmUpManager
import android.util.Log
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.gymtracker.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWarmUpScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val warmUpDao = remember { db.warmUpDao() }
    val exerciseDao = remember { db.exerciseDao() }
    val warmUpManager = remember { WarmUpManager(warmUpDao) }
    val coroutineScope = rememberCoroutineScope()

    // Get saved state handle for persistence
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    
    // Form state with persistence using savedStateHandle
    var templateName by rememberSaveable { mutableStateOf(savedStateHandle?.get<String>("templateName") ?: "") }
    var description by rememberSaveable { mutableStateOf(savedStateHandle?.get<String>("description") ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(savedStateHandle?.get<String>("selectedCategory") ?: "General") }
    var selectedDifficulty by rememberSaveable { mutableStateOf(savedStateHandle?.get<String>("selectedDifficulty") ?: "Beginner") }
    var estimatedDuration by rememberSaveable { mutableStateOf(savedStateHandle?.get<Int?>("estimatedDuration")) }
    var selectedMuscleGroups by rememberSaveable { mutableStateOf(savedStateHandle?.get<List<String>>("selectedMuscleGroups") ?: emptyList()) }
    
    // Exercise list state with persistence
    var warmUpExercises by rememberSaveable { mutableStateOf(savedStateHandle?.get<List<WarmUpExercise>>("warmUpExercises") ?: emptyList()) }
    var exerciseDetails by rememberSaveable { mutableStateOf(savedStateHandle?.get<Map<Int, EntityExercise>>("exerciseDetails") ?: emptyMap()) }
    
    // Local state for UI
    var showDeleteConfirmation by remember { mutableStateOf<WarmUpExercise?>(null) }
    
    // Categories and difficulties
    val categories = listOf("General", "Muscle-Specific", "Cardio", "Stretching", "Equipment-Based")
    val difficulties = listOf("Beginner", "Intermediate", "Advanced") 
    val muscleGroups = listOf("Full Body", "Neck", "Chest", "Shoulders", "Arms", "Core", "Back", "Legs", "Glutes", "Cardio")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Warm-Up",
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Template Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Template Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Template Name
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { 
                            templateName = it
                            savedStateHandle?.set("templateName", it)
                        },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { 
                            description = it
                            savedStateHandle?.set("description", it)
                        },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    
                    // Category and Difficulty Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category Dropdown
                        var categoryExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = { 
                                            selectedCategory = category
                                            savedStateHandle?.set("selectedCategory", category)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Difficulty Dropdown
                        var difficultyExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = difficultyExpanded,
                            onExpandedChange = { difficultyExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedDifficulty,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Difficulty") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = difficultyExpanded,
                                onDismissRequest = { difficultyExpanded = false }
                            ) {
                                difficulties.forEach { difficulty ->
                                    DropdownMenuItem(
                                        text = { Text(difficulty) },
                                        onClick = { 
                                            selectedDifficulty = difficulty
                                            savedStateHandle?.set("selectedDifficulty", difficulty)
                                            difficultyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Duration and Muscle Groups Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Duration
                        OutlinedTextField(
                            value = if (estimatedDuration == null) "" else estimatedDuration.toString(),
                            onValueChange = { 
                                val newDuration = it.toIntOrNull()
                                estimatedDuration = newDuration
                                savedStateHandle?.set("estimatedDuration", newDuration)
                            },
                            label = { Text("Duration (minutes)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        // Muscle Groups
                        var muscleGroupsExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = muscleGroupsExpanded,
                            onExpandedChange = { muscleGroupsExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedMuscleGroups.isEmpty()) "Select Muscle Groups" else "${selectedMuscleGroups.size} selected",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Target Muscles") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleGroupsExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = muscleGroupsExpanded,
                                onDismissRequest = { muscleGroupsExpanded = false }
                            ) {
                                muscleGroups.forEach { muscleGroup ->
                                    DropdownMenuItem(
                                        text = { Text(muscleGroup) },
                                        onClick = {
                                            if (selectedMuscleGroups.contains(muscleGroup)) {
                                                val newList = selectedMuscleGroups.filter { it != muscleGroup }
                                                selectedMuscleGroups = newList
                                                savedStateHandle?.set("selectedMuscleGroups", newList)
                                            } else {
                                                val newList = selectedMuscleGroups + muscleGroup
                                                selectedMuscleGroups = newList
                                                savedStateHandle?.set("selectedMuscleGroups", newList)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Selected Muscle Groups Chips
                    if (selectedMuscleGroups.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedMuscleGroups.forEach { muscleGroup ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val newList = selectedMuscleGroups.filter { it != muscleGroup }
                                        selectedMuscleGroups = newList
                                        savedStateHandle?.set("selectedMuscleGroups", newList)
                                    },
                                    label = { Text(muscleGroup) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Exercises Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Warm-Up Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Button(
                            onClick = { 
                                // Navigate to AddExerciseToWorkoutScreen with a dummy workoutId
                                // We'll use -1 to indicate this is for warm-up creation
                                navController.navigate(
                                    Screen.AddExerciseToWorkout.createRoute(-1)
                                )
                            },
                            enabled = templateName.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Exercise")
                        }
                    }
                    
                    if (warmUpExercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No exercises added yet.\nClick 'Add Exercise' to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(warmUpExercises) { warmUpExercise ->
                                val exercise = exerciseDetails[warmUpExercise.exerciseId]
                                if (exercise != null) {
                                    WarmUpExerciseCard(
                                        warmUpExercise = warmUpExercise,
                                        exercise = exercise,
                                        onDelete = { showDeleteConfirmation = warmUpExercise }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    if (templateName.isNotEmpty() && warmUpExercises.isNotEmpty()) {
                        coroutineScope.launch {
                            try {
                                                                 val exercises = warmUpExercises
                                val templateId = warmUpManager.createWarmUpTemplate(
                                    name = templateName,
                                    description = description,
                                    category = selectedCategory,
                                    targetMuscleGroups = selectedMuscleGroups,
                                    difficulty = selectedDifficulty,
                                    estimatedDuration = estimatedDuration ?: 0, // Use 0 if null
                                    userId = "user", // TODO: Get actual user ID
                                    exercises = exercises
                                )
                                
                                withContext(Dispatchers.Main) {
                                    // Clear the saved state after successful save
                                    savedStateHandle?.remove<String>("templateName")
                                    savedStateHandle?.remove<String>("description")
                                    savedStateHandle?.remove<String>("selectedCategory")
                                    savedStateHandle?.remove<String>("selectedDifficulty")
                                    savedStateHandle?.remove<Int?>("estimatedDuration")
                                    savedStateHandle?.remove<List<String>>("selectedMuscleGroups")
                                    savedStateHandle?.remove<List<WarmUpExercise>>("warmUpExercises")
                                    savedStateHandle?.remove<Map<Int, EntityExercise>>("exerciseDetails")
                                    navController.popBackStack()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = templateName.isNotEmpty() && warmUpExercises.isNotEmpty()
            ) {
                Text("Save Warm-Up Template")
            }
        }
    }
    
    // Listen for new exercises added from AddExerciseToWorkoutScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Int?>(
            "newExerciseId",
            null
        )?.collect { newExerciseId ->
            if (newExerciseId != null) {
                Log.d("CreateWarmUpScreen", "New exercise added with ID: $newExerciseId")
                // Get the exercise details and add to warm-up
                coroutineScope.launch {
                    try {
                        val actualExercise = exerciseDao.getExerciseById(newExerciseId)
                        if (actualExercise != null) {
                            // Get exercise configuration from savedStateHandle
                            val exerciseSets = navController.currentBackStackEntry?.savedStateHandle?.get<Int>("exerciseSets") ?: 1
                            val exerciseReps = navController.currentBackStackEntry?.savedStateHandle?.get<Int>("exerciseReps") ?: (if (actualExercise.useTime) 30 else 10)
                            val exerciseWeight = navController.currentBackStackEntry?.savedStateHandle?.get<Int>("exerciseWeight") ?: 0
                            val exerciseIsTimeBased = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("exerciseIsTimeBased") ?: actualExercise.useTime
                            
                            // Debug logging
                            Log.d("CreateWarmUpScreen", "Exercise config from savedStateHandle:")
                            Log.d("CreateWarmUpScreen", "  Sets: $exerciseSets")
                            Log.d("CreateWarmUpScreen", "  Reps: $exerciseReps")
                            Log.d("CreateWarmUpScreen", "  Weight: $exerciseWeight")
                            Log.d("CreateWarmUpScreen", "  IsTimeBased: $exerciseIsTimeBased")
                            Log.d("CreateWarmUpScreen", "  Exercise useTime: ${actualExercise.useTime}")
                            
                            // Create WarmUpExercise with user-defined configuration
                            val warmUpExercise = WarmUpExercise(
                                templateId = 0, // Will be set when template is saved
                                exerciseId = newExerciseId,
                                order = warmUpExercises.size,
                                sets = exerciseSets,
                                reps = if (exerciseIsTimeBased) 0 else exerciseReps, // Reps only for non-time-based exercises
                                duration = if (exerciseIsTimeBased) exerciseReps else 0, // Duration only for time-based exercises
                                isTimeBased = exerciseIsTimeBased,
                                restBetweenSets = 0,
                                weight = exerciseWeight,
                                notes = ""
                            )
                            
                            // Debug logging for created WarmUpExercise
                            Log.d("CreateWarmUpScreen", "Created WarmUpExercise:")
                            Log.d("CreateWarmUpScreen", "  Sets: ${warmUpExercise.sets}")
                            Log.d("CreateWarmUpScreen", "  Reps: ${warmUpExercise.reps}")
                            Log.d("CreateWarmUpScreen", "  Duration: ${warmUpExercise.duration}")
                            Log.d("CreateWarmUpScreen", "  Weight: ${warmUpExercise.weight}")
                            Log.d("CreateWarmUpScreen", "  IsTimeBased: ${warmUpExercise.isTimeBased}")
                            
                            val newWarmUpExercises = warmUpExercises + warmUpExercise
                            val newExerciseDetails = exerciseDetails + (newExerciseId to actualExercise)
                            warmUpExercises = newWarmUpExercises
                            exerciseDetails = newExerciseDetails
                            savedStateHandle?.set("warmUpExercises", newWarmUpExercises)
                            savedStateHandle?.set("exerciseDetails", newExerciseDetails)
                            
                            // Clear all exercise configuration flags AFTER successfully adding the exercise
                            navController.currentBackStackEntry?.savedStateHandle?.set("newExerciseId", null)
                            navController.currentBackStackEntry?.savedStateHandle?.set("exerciseSets", null)
                            navController.currentBackStackEntry?.savedStateHandle?.set("exerciseReps", null)
                            navController.currentBackStackEntry?.savedStateHandle?.set("exerciseWeight", null)
                            navController.currentBackStackEntry?.savedStateHandle?.set("exerciseIsTimeBased", null)
                        }
                    } catch (e: Exception) {
                        Log.e("CreateWarmUpScreen", "Error adding exercise: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Exercise") },
                         text = { 
                 val exercise = exerciseDetails[showDeleteConfirmation!!.exerciseId]
                 Text("Are you sure you want to remove '${exercise?.name ?: "Unknown Exercise"}' from this warm-up?") 
             },
            confirmButton = {
                TextButton(
                    onClick = {
                        val exerciseToDelete = showDeleteConfirmation!!
                        val newWarmUpExercises = warmUpExercises.filter { it != exerciseToDelete }
                        val newExerciseDetails = exerciseDetails.filterKeys { it != exerciseToDelete.exerciseId }
                        warmUpExercises = newWarmUpExercises
                        exerciseDetails = newExerciseDetails
                        savedStateHandle?.set("warmUpExercises", newWarmUpExercises)
                        savedStateHandle?.set("exerciseDetails", newExerciseDetails)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WarmUpExerciseCard(
    warmUpExercise: WarmUpExercise,
    exercise: EntityExercise,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sets: ${warmUpExercise.sets}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (warmUpExercise.isTimeBased) {
                        Text(
                            text = "Duration: ${warmUpExercise.duration}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "Reps: ${warmUpExercise.reps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (warmUpExercise.restBetweenSets > 0) {
                        Text(
                            text = "Rest: ${warmUpExercise.restBetweenSets}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (warmUpExercise.weight > 0) {
                        Text(
                            text = "Weight: ${warmUpExercise.weight}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Exercise",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


