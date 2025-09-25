package com.example.gymtracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.gymtracker.data.AppDatabase
import androidx.compose.material3.*
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.WorkoutWithExercises
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutCard
import com.example.gymtracker.navigation.Screen
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.withContext
import com.example.gymtracker.components.LoadingSpinner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.components.WorkoutIndicator
import android.util.Log
import com.example.gymtracker.components.LevelUpDialog
import com.example.gymtracker.data.XPSystem

// Data class for level-up information
data class LevelUpData(
    val xpGained: Int,
    val currentLevel: Int,
    val newLevel: Int,
    val currentXP: Int,
    val xpForNextLevel: Int,
    val previousLevelXP: Int
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadWorkoutScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel
) {
    val context = LocalContext.current
    val workouts = remember { mutableStateOf(listOf<WorkoutWithExercises>()) }
    val filteredWorkouts = remember { mutableStateOf(listOf<WorkoutWithExercises>()) }
    val searchQuery = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    // State for workout creation dialog
    var showCreateWorkoutDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }

    // State for workout deletion confirmation dialog
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<EntityWorkout?>(null) }

    // State for filter dialog
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
    
    // State for level-up dialog
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var levelUpData by remember { mutableStateOf<LevelUpData?>(null) }


    // Load workouts with exercises
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).exerciseDao()
            val allWorkouts = dao.getAllWorkouts()
            val workoutsWithExercises = allWorkouts.mapNotNull { workout ->
                dao.getWorkoutWithExercises(workout.id).firstOrNull()
            }
            
            withContext(Dispatchers.Main) {
                workouts.value = workoutsWithExercises
                filteredWorkouts.value = workouts.value
                isLoading = false
            }
        }
    }
    
    // Check for level-up using XP buffer
    val xpBuffer by generalViewModel.xpBuffer.collectAsState()
    
    LaunchedEffect(xpBuffer) {
        if (xpBuffer != null) {
            Log.d("LoadWorkoutScreen", "XP buffer detected: ${xpBuffer}")
            
            // Use XPSystem utility functions for consistent calculations
            val xpSystem = XPSystem(AppDatabase.getDatabase(context).userXPDao())
            
            // Calculate XP for next level
            val xpForNextLevel = xpSystem.getXPNeededForLevel(xpBuffer!!.newLevel)
            
            // Show level-up dialog
            levelUpData = LevelUpData(
                xpGained = xpBuffer!!.xpGained,
                currentLevel = xpBuffer!!.previousLevel,
                newLevel = xpBuffer!!.newLevel,
                currentXP = xpBuffer!!.newTotalXP,
                xpForNextLevel = xpForNextLevel,
                previousLevelXP = xpBuffer!!.previousTotalXP  // Use actual XP before gain, not level start
            )
            showLevelUpDialog = true
            Log.d("LoadWorkoutScreen", "Level-up dialog triggered from XP buffer")
        }
    }

    // Get unique muscle groups from all workouts
    val muscleGroups = workouts.value.flatMap { workoutWithExercises ->
        workoutWithExercises.exercises.map { it.muscle }
    }.distinct().sorted()

    // Filter workouts when search query or muscle group filter changes
    LaunchedEffect(searchQuery.value, selectedMuscleGroup) {
        filteredWorkouts.value = workouts.value.filter { workoutWithExercises ->
            val matchesSearch = searchQuery.value.isEmpty() ||
                    workoutWithExercises.workout.name.contains(searchQuery.value, ignoreCase = true)
            val matchesMuscle = selectedMuscleGroup == null ||
                    workoutWithExercises.exercises.any { it.muscle == selectedMuscleGroup }
            matchesSearch && matchesMuscle
        }
    }

    // Function to create new workout and navigate to details
    fun createNewWorkout() {
        if (newWorkoutName.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(context).exerciseDao()
                val newWorkout = EntityWorkout(name = newWorkoutName)
                val workoutId = dao.insertWorkout(newWorkout).toInt()

                // Navigate to workout details screen with the new workout ID on main thread
                withContext(Dispatchers.Main) {
                    navController.navigate(Screen.Routes.workoutDetails(workoutId))
                    // Clear the dialog and reset name only after successful creation
                    newWorkoutName = ""
                    showCreateWorkoutDialog = false
                }
            }
        }
    }

    // Function to show delete confirmation dialog
    fun showDeleteConfirmation(workout: EntityWorkout) {
        workoutToDelete = workout
        showDeleteConfirmationDialog = true
    }

    // Function to delete workout
    fun deleteWorkout(workout: EntityWorkout) {
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).exerciseDao()

            // Delete all workout exercises first (foreign key constraint)
            dao.deleteWorkoutExercisesForWorkout(workout.id)

            // Delete the workout
            dao.deleteWorkout(workout)

            // Refresh the workouts list
            withContext(Dispatchers.Main) {
                val allWorkouts = dao.getAllWorkouts()
                val workoutsWithExercises = allWorkouts.mapNotNull { w ->
                    dao.getWorkoutWithExercises(w.id).firstOrNull()
                }
                workouts.value = workoutsWithExercises
                filteredWorkouts.value = if (searchQuery.value.isEmpty()) {
                    workouts.value
                } else {
                    workouts.value.filter { workoutWithExercises ->
                        workoutWithExercises.workout.name.contains(
                            searchQuery.value,
                            ignoreCase = true
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Workouts") },
                    actions = {
                        WorkoutIndicator(generalViewModel = generalViewModel, navController = navController)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                )
                SearchBar(
                    query = searchQuery.value,
                    onQueryChange = { searchQuery.value = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search workouts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        Row {
                            if (searchQuery.value.isNotEmpty()) {
                                IconButton(onClick = { searchQuery.value = "" }) {
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
                ) {}

                // Active filters in top bar
                if (selectedMuscleGroup != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedMuscleGroup = null },
                            label = { Text(selectedMuscleGroup!!) }
                        )
                    }
                }
            }
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Warm-Up Creation Button
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateWarmUp.route) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Create Warm-Up",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Existing Create Exercise Button
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateExercise.route) },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        if (isLoading) {
            // Loading indicator in center of page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingSpinner(
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New Workout Button at the top
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clickable { showCreateWorkoutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Workout",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Workout",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Spacer between New Workout button and workout cards
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Existing workout cards
                items(filteredWorkouts.value) { workoutWithExercises ->
                    val currentWorkout by generalViewModel.currentWorkout.collectAsState()
                    val isActive = currentWorkout?.workoutId == workoutWithExercises.workout.id && currentWorkout?.isActive == true
                    
                    WorkoutCard(
                        workout = workoutWithExercises.workout,
                        muscleGroups = workoutWithExercises.exercises.map { it.muscle }.distinct(),
                        onClick = {
                            navController.navigate(
                                Screen.Routes.workoutDetails(
                                    workoutWithExercises.workout.id
                                )
                            )
                        },
                        onDelete = { showDeleteConfirmation(workoutWithExercises.workout) },
                        isActive = isActive
                    )
                }
            }
        }
    }

    // Create Workout Dialog
    if (showCreateWorkoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateWorkoutDialog = false
                newWorkoutName = ""
            },
            title = { Text("Create New Workout") },
            text = {
                OutlinedTextField(
                    value = newWorkoutName,
                    onValueChange = { newWorkoutName = it },
                    label = { Text("Workout Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { createNewWorkout() },
                    enabled = newWorkoutName.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateWorkoutDialog = false
                        newWorkoutName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Workout Confirmation Dialog
    if (showDeleteConfirmationDialog && workoutToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                workoutToDelete = null
            },
            title = { Text("Delete Workout") },
            text = {
                Text("Are you sure you want to delete '${workoutToDelete!!.name}'? This action cannot be undone and will also remove all exercises in this workout.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteWorkout(workoutToDelete!!)
                        showDeleteConfirmationDialog = false
                        workoutToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        workoutToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Workouts") },
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Level Up Dialog
    if (showLevelUpDialog && levelUpData != null) {
        Log.d("LoadWorkoutScreen", "Rendering LevelUpDialog - showLevelUpDialog: $showLevelUpDialog, levelUpData: $levelUpData")
        LevelUpDialog(
            onDismiss = {
                Log.d("LoadWorkoutScreen", "LevelUpDialog dismissed")
                showLevelUpDialog = false
                levelUpData = null
                // Clear the XP buffer to prevent showing dialog again
                generalViewModel.clearXPBuffer()
            },
            xpGained = levelUpData!!.xpGained,
            currentLevel = levelUpData!!.currentLevel,
            newLevel = levelUpData!!.newLevel,
            currentXP = levelUpData!!.currentXP,
            xpForNextLevel = levelUpData!!.xpForNextLevel,
            previousLevelXP = levelUpData!!.previousLevelXP
        )
    } else {
        Log.d("LoadWorkoutScreen", "LevelUpDialog condition not met - showLevelUpDialog: $showLevelUpDialog, levelUpData: $levelUpData")
    }
}
