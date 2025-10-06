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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.Converter
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.WorkoutExerciseWithDetails
import com.example.gymtracker.classes.NumberPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlowRow(
    items: List<String>,
    selectedItems: List<String>,
    onItemClick: (String) -> Unit,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Int = 8
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        FilterChip(
            selected = selectedItems.isEmpty(),
            onClick = onAllClick,
            label = { 
                Text(
                    "All", 
                    maxLines = 1,
                    color = Color(0xFF2196F3) // Blue color for the first "All"
                ) 
            },
        )
        items.forEach { item ->
            FilterChip(
                selected = selectedItems.contains(item),
                onClick = { onItemClick(item) },
                label = {
                    Text(
                        text = item,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = when (item) {
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
    var selectedMuscleGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDifficulties by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMuscleParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedEquipment by remember { mutableStateOf<List<String>>(emptyList()) }

    // Create a single interaction source for all cards
    val interactionSource = remember { MutableInteractionSource() }

    // Use the same muscle parts map as CreateExerciseScreen
    val musclePartsMap = mapOf(
        "All" to listOf(" "), // "All" for showing all exercises
        "Neck" to listOf("Neck", "Upper Traps"),
        "Chest" to listOf("Chest"),
        "Shoulders" to listOf("Deltoids"),
        "Arms" to listOf("Biceps", "Triceps", "Forearms"),
        "Core" to listOf("Abs", "Obliques", "Lower Back"),
        "Back" to listOf("Upper Back", "Lats", "Lower Back"),
        "Legs" to listOf("Quadriceps", "Hamstrings", "Adductors", "Glutes", "Calves")
    )

    // Function to extract muscle groups from workout name
    // Examples of workout names and expected muscle groups:
    // "Chest & Abs" -> ["Chest", "Core"]
    // "Back + Biceps" -> ["Back", "Arms"] 
    // "Push Day" -> ["Chest", "Shoulders", "Arms"]
    // "Pull Day" -> ["Back", "Arms"]
    // "Upper Body" -> ["Chest", "Back", "Shoulders", "Arms", "Core"]
    // "Lower Body" -> ["Legs", "Core"]
    // "Full Body" -> ["All"]
    // "Leg Day" -> ["Legs"]
    // "Core & Cardio" -> ["Core"]
    // "Shoulder Press" -> ["Shoulders"]
    // "Bench Press" -> ["Chest"]
    fun extractMuscleGroupsFromName(workoutName: String): List<String> {
        val name = workoutName.lowercase()
        val extractedGroups = mutableListOf<String>()
        
        Log.d("AddExerciseToWorkoutScreen", "Analyzing workout name: '$workoutName'")
        
        // First, check for explicit muscle group combinations (e.g., "Chest & Abs", "Back + Biceps")
        val muscleGroupPatterns = listOf(
            "chest" to "Chest",
            "back" to "Back", 
            "shoulder" to "Shoulders",
            "arm" to "Arms",
            "bicep" to "Arms",
            "tricep" to "Arms",
            "core" to "Core",
            "ab" to "Core",
            "abs" to "Core",
            "oblique" to "Core",
            "leg" to "Legs",
            "quad" to "Legs",
            "hamstring" to "Legs",
            "glute" to "Legs",
            "calf" to "Legs",
            "neck" to "Neck",
            "trap" to "Neck"
        )
        
        // Check for explicit muscle group mentions first
        muscleGroupPatterns.forEach { (pattern, muscleGroup) ->
            if (name.contains(pattern)) {
                extractedGroups.add(muscleGroup)
                Log.d("AddExerciseToWorkoutScreen", "Found muscle group '$muscleGroup' from pattern '$pattern'")
            }
        }
        
        Log.d("AddExerciseToWorkoutScreen", "Explicit muscle groups found: $extractedGroups")
        
        // Check for specific workout types (push/pull splits) - only if no explicit muscle groups found
        if (extractedGroups.isEmpty()) {
            if (name.contains("push") && !name.contains("pull")) {
                extractedGroups.addAll(listOf("Chest", "Shoulders", "Arms"))
                Log.d("AddExerciseToWorkoutScreen", "Detected push workout - adding: Chest, Shoulders, Arms")
                return extractedGroups.distinct()
            }
            if (name.contains("pull") && !name.contains("push")) {
                extractedGroups.addAll(listOf("Back", "Arms"))
                Log.d("AddExerciseToWorkoutScreen", "Detected pull workout - adding: Back, Arms")
                return extractedGroups.distinct()
            }
        }
        
        // Check for workout type keywords if no explicit muscle groups found
        if (extractedGroups.isEmpty()) {
            if (name.contains("bench") || name.contains("pec") || name.contains("incline") || 
                name.contains("decline") || name.contains("fly") || name.contains("dumbbell") || 
                name.contains("barbell")) {
                extractedGroups.add("Chest")
                Log.d("AddExerciseToWorkoutScreen", "Detected chest workout from exercise keywords")
            }
            if (name.contains("row") || name.contains("lat") || name.contains("rhomboid") || 
                name.contains("chin") || name.contains("pullup") || name.contains("pulldown")) {
                extractedGroups.add("Back")
                Log.d("AddExerciseToWorkoutScreen", "Detected back workout from exercise keywords")
            }
            if (name.contains("press") || name.contains("deltoid") || name.contains("lateral") || 
                name.contains("rear") || name.contains("military") || name.contains("arnold") || 
                name.contains("upright") || name.contains("shrug")) {
                extractedGroups.add("Shoulders")
                Log.d("AddExerciseToWorkoutScreen", "Detected shoulder workout from exercise keywords")
            }
            if (name.contains("curl") || name.contains("extension") || name.contains("forearm") ||
                name.contains("hammer") || name.contains("concentration") || name.contains("skull")) {
                extractedGroups.add("Arms")
                Log.d("AddExerciseToWorkoutScreen", "Detected arm workout from exercise keywords")
            }
            if (name.contains("crunch") || name.contains("plank") || name.contains("situp") ||
                name.contains("leg") || name.contains("raise") || name.contains("twist")) {
                extractedGroups.add("Core")
                Log.d("AddExerciseToWorkoutScreen", "Detected core workout from exercise keywords")
            }
            if (name.contains("squat") || name.contains("deadlift") || name.contains("lunge") || 
                name.contains("extension") || name.contains("curl") || name.contains("adduction") ||
                name.contains("abduction") || name.contains("step") || name.contains("jump")) {
                extractedGroups.add("Legs")
                Log.d("AddExerciseToWorkoutScreen", "Detected leg workout from exercise keywords")
            }
        }
        
        // Check for general workout types
        if (name.contains("full") || name.contains("body") || name.contains("total") || 
            name.contains("complete") || name.contains("whole") || name.contains("upper") || 
            name.contains("lower") || name.contains("split") || name.contains("circuit") ||
            name.contains("hiit") || name.contains("cardio") || name.contains("strength")) {
            // For split workouts, we'll need to analyze more carefully
            if (name.contains("upper")) {
                extractedGroups.addAll(listOf("Chest", "Back", "Shoulders", "Arms", "Core"))
                Log.d("AddExerciseToWorkoutScreen", "Detected upper body workout")
            } else if (name.contains("lower")) {
                extractedGroups.addAll(listOf("Legs", "Core"))
                Log.d("AddExerciseToWorkoutScreen", "Detected lower body workout")
            } else if (extractedGroups.isEmpty()) {
                extractedGroups.add("All")
                Log.d("AddExerciseToWorkoutScreen", "Detected full body workout")
            }
        }
        
        val finalGroups = extractedGroups.distinct()
        Log.d("AddExerciseToWorkoutScreen", "Final extracted muscle groups: $finalGroups")
        return finalGroups
    }

    // Function to get muscle groups from existing exercises in workout
    suspend fun getMuscleGroupsFromExistingExercises(workoutId: Int): List<String> {
        return try {
            val workoutExercises = dao.getWorkoutExercisesForWorkout(workoutId)
            val muscleGroups = workoutExercises.mapNotNull { workoutExercise ->
                val exercise = dao.getExerciseById(workoutExercise.exerciseId)
                exercise?.muscle
            }.distinct()
            
            // Map exercise muscle values to our muscle group categories
            muscleGroups.mapNotNull { muscle ->
                when (muscle) {
                    "Neck" -> "Neck"
                    "Chest" -> "Chest"
                    "Shoulder" -> "Shoulders"
                    "Arms" -> "Arms"
                    "Core" -> "Core"
                    "Back" -> "Back"
                    "Legs" -> "Legs"
                    "All" -> "All"
                    else -> null
                }
            }.distinct()
        } catch (e: Exception) {
            Log.e("AddExerciseToWorkoutScreen", "Error getting muscle groups from existing exercises: ${e.message}")
            emptyList()
        }
    }

    // Load all exercises and auto-select muscle groups for workouts
    LaunchedEffect(Unit) {
        try {
            Log.d("AddExerciseToWorkoutScreen", "Starting to load exercises")
            withContext(Dispatchers.IO) {
                val allExercises = dao.getAllExercises()
                Log.d("AddExerciseToWorkoutScreen", "Loaded ${allExercises.size} exercises from database")
                
                // Auto-select muscle groups if this is for a workout (not warm-up creation)
                if (workoutId != -1) {
                    try {
                        Log.d("AddExerciseToWorkoutScreen", "Auto-selecting muscle groups for workout ID: $workoutId")
                        
                        // Get workout name
                        val workout = dao.getAllWorkouts().find { it.id == workoutId }
                        val workoutName = workout?.name ?: ""
                        
                        Log.d("AddExerciseToWorkoutScreen", "Found workout: '$workoutName' (ID: $workoutId)")
                        
                        // Extract muscle groups from workout name
                        val nameBasedGroups = extractMuscleGroupsFromName(workoutName)
                        
                        // Get muscle groups from existing exercises
                        val exerciseBasedGroups = getMuscleGroupsFromExistingExercises(workoutId)
                        
                        // Combine both sources, prioritizing exercise-based groups
                        val combinedGroups = if (exerciseBasedGroups.isNotEmpty()) {
                            Log.d("AddExerciseToWorkoutScreen", "Using exercise-based muscle groups (priority)")
                            exerciseBasedGroups
                        } else {
                            Log.d("AddExerciseToWorkoutScreen", "Using name-based muscle groups")
                            nameBasedGroups
                        }
                        
                        Log.d("AddExerciseToWorkoutScreen", "Workout name: '$workoutName'")
                        Log.d("AddExerciseToWorkoutScreen", "Name-based muscle groups: $nameBasedGroups")
                        Log.d("AddExerciseToWorkoutScreen", "Exercise-based muscle groups: $exerciseBasedGroups")
                        Log.d("AddExerciseToWorkoutScreen", "Final selected muscle groups: $combinedGroups")
                        
                        withContext(Dispatchers.Main) {
                            exercises = allExercises
                            selectedMuscleGroups = combinedGroups
                            
                            // Also update muscle parts if muscle groups are selected
                            if (combinedGroups.isNotEmpty() && !combinedGroups.contains("All")) {
                                val availableParts = combinedGroups.flatMap { muscleGroup ->
                                    musclePartsMap[muscleGroup] ?: emptyList()
                                }.filter { it.isNotBlank() && it != " " }.distinct().sorted()
                                Log.d("AddExerciseToWorkoutScreen", "Auto-updating available muscle parts: $availableParts")
                            }
                            
                            Log.d("AddExerciseToWorkoutScreen", "Updated UI with exercises and auto-selected muscle groups")
                        }
                    } catch (e: Exception) {
                        Log.e("AddExerciseToWorkoutScreen", "Error auto-selecting muscle groups: ${e.message}")
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            exercises = allExercises
                        }
                    }
                } else {
                    // This is for warm-up creation, just load exercises
                    Log.d("AddExerciseToWorkoutScreen", "Warm-up creation mode - no auto-selection")
                    withContext(Dispatchers.Main) {
                        exercises = allExercises
                        Log.d("AddExerciseToWorkoutScreen", "Updated UI with exercises (warm-up mode)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AddExerciseToWorkoutScreen", "Error loading exercises: ${e.message}")
            e.printStackTrace()
        }
    }

    // Get muscle groups from the musclePartsMap
    val muscleGroups = musclePartsMap.keys.toList()
    
    // Filter exercises based on all current selections for dynamic filtering
    val filteredExercisesForOptions = exercises.filter { exercise ->
        // Muscle group filtering
        val matchesMuscleGroup = if (selectedMuscleGroups.isEmpty()) {
            true
        } else {
            val exerciseMuscleGroup = when (exercise.muscle) {
                "Neck" -> "Neck"
                "Chest" -> "Chest"
                "Shoulder" -> "Shoulders"
                "Arms" -> "Arms"
                "Core" -> "Core"
                "Back" -> "Back"
                "Legs" -> "Legs"
                else -> exercise.muscle
            }
            
            if (selectedMuscleGroups.contains("All")) {
                // When "All" is selected, show only exercises with muscle="All"
                exercise.muscle == "All"
            } else {
                selectedMuscleGroups.contains(exerciseMuscleGroup)
            }
        }
        
        // Difficulty filtering
        val matchesDifficulty = selectedDifficulties.isEmpty() || selectedDifficulties.contains(exercise.difficulty)
        
        // Equipment filtering
        val matchesEquipment = selectedEquipment.isEmpty() || run {
            val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                exercise.equipment.split(",").map { it.trim() }
            } else {
                listOf("None")
            }
            selectedEquipment.any { selected -> exerciseEquipment.contains(selected) }
        }
        
        matchesMuscleGroup && matchesDifficulty && matchesEquipment
    }
    
    // Dynamic equipment list based on filtered exercises
    val equipmentList = filteredExercisesForOptions.flatMap { exercise ->
        if (exercise.equipment.isNotBlank()) {
            exercise.equipment.split(",").map { it.trim() }
        } else {
            listOf("None")
        }
    }.distinct().let { equipment ->
        // Put "None" and "Other" at the top of the list
        val noneAndOther = equipment.filter { it in listOf("None", "Other") }
        val others = equipment.filter { it !in listOf("None", "Other") }.sorted()
        noneAndOther + others
    }
    
    // Dynamic difficulties list based on filtered exercises
    val difficulties = filteredExercisesForOptions.map { it.difficulty }.distinct().sorted()
    
    // Filter muscle parts based on selected muscle groups
    val availableMuscleParts = if (selectedMuscleGroups.isEmpty()) {
        // Show all muscle parts when no muscle group is selected
        musclePartsMap.values.flatten().filter { it.isNotBlank() && it != " " }.distinct().sorted()
    } else if (selectedMuscleGroups.contains("All")) {
        // Show no muscle parts when "All" is selected
        emptyList()
    } else {
        // Show only muscle parts from selected muscle groups
        selectedMuscleGroups.flatMap { muscleGroup ->
            musclePartsMap[muscleGroup] ?: emptyList()
        }.filter { it.isNotBlank() && it != " " }.distinct().sorted()
    }
    
    // Debug logging for muscle parts availability
    if (selectedMuscleGroups.isNotEmpty()) {
        Log.d("AddExerciseToWorkoutScreen", "Selected muscle groups: $selectedMuscleGroups")
        Log.d("AddExerciseToWorkoutScreen", "Available muscle parts: $availableMuscleParts")
        Log.d("AddExerciseToWorkoutScreen", "Selected muscle parts: $selectedMuscleParts")
    }
    
    // Debug logging for Upper Back filter
    if (selectedMuscleParts.contains("Upper Back")) {
        Log.d("AddExerciseToWorkoutScreen", "Upper Back selected - Available muscle parts: $availableMuscleParts")
        Log.d("AddExerciseToWorkoutScreen", "Selected muscle parts: $selectedMuscleParts")
        Log.d("AddExerciseToWorkoutScreen", "Selected muscle groups: $selectedMuscleGroups")
    }
    
    // Debug logging for muscle parts availability
    if (selectedMuscleGroups.contains("Back")) {
        Log.d("AddExerciseToWorkoutScreen", "Back muscle group selected - Available muscle parts: $availableMuscleParts")
        Log.d("AddExerciseToWorkoutScreen", "Back muscle parts from map: ${musclePartsMap["Back"]}")
    }
    
    // Clear muscle parts when muscle groups change
    LaunchedEffect(selectedMuscleGroups) {
        if (selectedMuscleGroups.isNotEmpty() && !selectedMuscleGroups.contains("All")) {
            val validMuscleParts = selectedMuscleGroups.flatMap { muscleGroup ->
                musclePartsMap[muscleGroup] ?: emptyList()
            }.filter { it.isNotBlank() && it != " " }
            selectedMuscleParts = selectedMuscleParts.filter { it in validMuscleParts }
            Log.d("AddExerciseToWorkoutScreen", "Muscle groups changed - Updated muscle parts: $selectedMuscleParts")
        } else if (selectedMuscleGroups.contains("All")) {
            // Clear muscle parts when "All" is selected
            selectedMuscleParts = emptyList()
            Log.d("AddExerciseToWorkoutScreen", "All muscle groups selected - Cleared muscle parts")
        }
    }

    // Filter exercises based on search query and filters
    val filteredExercises = exercises.filter { exercise ->
        val searchTerms = searchQuery.lowercase().split(" ").filter { it.isNotEmpty() }
        val matchesSearch = if (searchTerms.isEmpty()) {
            true
        } else {
            val exerciseNameLower = exercise.name.lowercase()
            searchTerms.all { term -> exerciseNameLower.contains(term) }
        }
        
        // TODO: REMOVE THIS MAPPING ONCE CSV FILE IS UPDATED WITH NEW MUSCLE GROUP NAMES
        // This mapping is needed for backward compatibility with old exercise data
        // that uses "Shoulder" instead of "Shoulders"
        val exerciseMuscleGroup = when (exercise.muscle) {
            "Neck" -> "Neck"
            "Chest" -> "Chest"
            "Shoulder" -> "Shoulders"
            "Arms" -> "Arms"
            "Core" -> "Core"
            "Back" -> "Back"
            "Legs" -> "Legs"
            else -> exercise.muscle // Fallback for any other muscle groups
        }
        
        // Handle "All" filter: when selected shows only exercises with muscle="All"
        val matchesMuscle = if (selectedMuscleGroups.isEmpty()) {
            true // Show all exercises when no filter is selected
        } else if (selectedMuscleGroups.contains("All")) {
            // When "All" is selected, show only exercises with muscle="All"
            exercise.muscle == "All"
        } else {
            selectedMuscleGroups.contains(exerciseMuscleGroup)
        }
        
        val matchesDifficulty = selectedDifficulties.isEmpty() || selectedDifficulties.contains(exercise.difficulty)
        val exerciseParts = try {
            Converter().fromString(exercise.parts)
        } catch (e: Exception) {
            Log.e("AddExerciseToWorkoutScreen", "Error parsing exercise parts for ${exercise.name}: ${e.message}")
            emptyList<String>()
        }
        Log.d("AddExerciseToWorkoutScreen", "Exercise: ${exercise.name}, Parts JSON: ${exercise.parts}, Parsed parts: $exerciseParts")
        val matchesMusclePart = selectedMuscleParts.isEmpty() || exerciseParts.any { exercisePart -> 
            selectedMuscleParts.any { selectedPart -> 
                val matches = when (selectedPart.lowercase()) {
                    "adductors" -> exercisePart.equals("adductors", ignoreCase = true) || exercisePart.equals("adductor", ignoreCase = true)
                    "adductor" -> exercisePart.equals("adductors", ignoreCase = true) || exercisePart.equals("adductor", ignoreCase = true)
                    else -> exercisePart.equals(selectedPart, ignoreCase = true)
                }
                if (selectedMuscleParts.contains("Upper Back")) {
                    Log.d("AddExerciseToWorkoutScreen", "Checking Upper Back filter - Exercise part: '$exercisePart', Selected part: '$selectedPart', Matches: $matches")
                }
                matches
            }
        }
        val matchesEquipment = selectedEquipment.isEmpty() || run {
            val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                exercise.equipment.split(",").map { it.trim() }
            } else {
                listOf("None")
            }
            selectedEquipment.any { selected -> exerciseEquipment.contains(selected) }
        }
        val finalMatch = matchesSearch && matchesMuscle && matchesDifficulty && matchesMusclePart && matchesEquipment
        
        // Debug logging for Upper Back filter
        if (selectedMuscleParts.contains("Upper Back") && exercise.muscle == "Back") {
            Log.d("AddExerciseToWorkoutScreen", "Upper Back filter - Exercise: ${exercise.name}, Matches: search=$matchesSearch, muscle=$matchesMuscle, difficulty=$matchesDifficulty, musclePart=$matchesMusclePart, equipment=$matchesEquipment, final=$finalMatch")
        }
        
        finalMatch
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
                            text = if (workoutId == -1) "Select Exercise for Warm-Up" else "Add Exercise",
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
                if (selectedMuscleGroups.isNotEmpty() || selectedDifficulties.isNotEmpty() || selectedMuscleParts.isNotEmpty() || selectedEquipment.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show auto-selection indicator for workouts
                        if (workoutId != -1 && selectedMuscleGroups.isNotEmpty()) {
                            Text(
                                text = "Auto-selected based on workout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(selectedMuscleGroups) { muscleGroup ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedMuscleGroups = selectedMuscleGroups.filter { it != muscleGroup } },
                                    label = { Text(muscleGroup) }
                                )
                            }
                            items(selectedDifficulties) { difficulty ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedDifficulties = selectedDifficulties.filter { it != difficulty } },
                                    label = { Text(difficulty) }
                                )
                            }
                            items(selectedMuscleParts) { musclePart ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedMuscleParts = selectedMuscleParts.filter { it != musclePart } },
                                    label = { Text(musclePart) }
                                )
                            }
                            items(selectedEquipment) { equipment ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedEquipment = selectedEquipment.filter { it != equipment } },
                                    label = { Text(equipment) }
                                )
                            }
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
                                if (exercise.equipment.isNotBlank()) {
                                    Text(
                                        text = "Equipment: ${exercise.equipment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                                        if (workoutId == -1) {
                                            // This is for warm-up creation, pass exercise ID and configuration
                                            val exerciseReps = if (selectedExercise!!.useTime) (minutes * 60 + seconds) else reps
                                            val exerciseWeight = if (selectedExercise!!.useTime) 0 else weight
                                            
                                            // Debug logging
                                            Log.d("AddExerciseToWorkoutScreen", "Setting warm-up exercise config:")
                                            Log.d("AddExerciseToWorkoutScreen", "  Exercise ID: ${selectedExercise!!.id}")
                                            Log.d("AddExerciseToWorkoutScreen", "  Sets: $sets")
                                            Log.d("AddExerciseToWorkoutScreen", "  Reps: $exerciseReps")
                                            Log.d("AddExerciseToWorkoutScreen", "  Weight: $exerciseWeight")
                                            Log.d("AddExerciseToWorkoutScreen", "  IsTimeBased: ${selectedExercise!!.useTime}")
                                            
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("newExerciseId", selectedExercise!!.id)
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("exerciseSets", sets)
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("exerciseReps", exerciseReps)
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("exerciseWeight", exerciseWeight)
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("exerciseIsTimeBased", selectedExercise!!.useTime)
                                        } else {
                                            // Normal workout exercise addition
                                            val workoutExercise = WorkoutExercise(
                                                exerciseId = selectedExercise!!.id,
                                                workoutId = workoutId,
                                                sets = sets,
                                                reps = if (selectedExercise!!.useTime) (minutes * 60 + seconds) else reps,
                                                weight = if (selectedExercise!!.useTime) 0 else weight,
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
                                        }
                                    } else {
                                        Log.e("AddExerciseToWorkoutScreen", "EntityExercise not found for id: ${selectedExercise!!.id}")
                                    }
                                    showNumberPicker = false
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text(if (workoutId == -1) "Select" else "Add")
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
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)                    
                ) {
                    // Muscle Group Filter
                    Text(
                        text = "Muscle Group",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRow(
                        items = muscleGroups,
                        selectedItems = selectedMuscleGroups,
                        onItemClick = { muscle ->
                            if (selectedMuscleGroups.contains(muscle)) {
                                selectedMuscleGroups = selectedMuscleGroups.filter { it != muscle }
                            } else {
                                selectedMuscleGroups = selectedMuscleGroups + muscle
                            }
                        },
                        onAllClick = { selectedMuscleGroups = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Muscle Part Filter
                    Text(
                        text = "Muscle Part",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRow(
                        items = availableMuscleParts,
                        selectedItems = selectedMuscleParts,
                        onItemClick = { musclePart ->
                            if (selectedMuscleParts.contains(musclePart)) {
                                selectedMuscleParts = selectedMuscleParts.filter { it != musclePart }
                            } else {
                                selectedMuscleParts = selectedMuscleParts + musclePart
                            }
                        },
                        onAllClick = { selectedMuscleParts = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Difficulty Level Filter
                    Text(
                        text = "Difficulty Level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    FilterChipFlowRow(
                        items = difficulties,
                        selectedItems = selectedDifficulties,
                        onItemClick = { difficulty ->
                            if (selectedDifficulties.contains(difficulty)) {
                                selectedDifficulties = selectedDifficulties.filter { it != difficulty }
                            } else {
                                selectedDifficulties = selectedDifficulties + difficulty
                            }
                        },
                        onAllClick = { selectedDifficulties = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Equipment Filter
                    Text(
                        text = "Equipment",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    FilterChipFlowRow(
                        items = equipmentList,
                        selectedItems = selectedEquipment,
                        onItemClick = { equipment ->
                            if (selectedEquipment.contains(equipment)) {
                                selectedEquipment = selectedEquipment.filter { it != equipment }
                            } else {
                                selectedEquipment = selectedEquipment + equipment
                            }
                        },
                        onAllClick = { selectedEquipment = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )
                
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