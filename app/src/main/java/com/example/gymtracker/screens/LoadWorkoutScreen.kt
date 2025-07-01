package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.gymtracker.data.AppDatabase
import androidx.compose.material3.*
import com.example.gymtracker.data.EntityWorkout
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.Dispatchers
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutCard
import com.example.gymtracker.navigation.Screen
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadWorkoutScreen(navController: NavController) {
    val context = LocalContext.current
    val workouts = remember { mutableStateOf(listOf<EntityWorkout>()) }
    val filteredWorkouts = remember { mutableStateOf(listOf<EntityWorkout>()) }
    val searchQuery = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // State for workout creation dialog
    var showCreateWorkoutDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }

    // Load workouts
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).exerciseDao()
            workouts.value = dao.getAllWorkouts()
            filteredWorkouts.value = workouts.value
        }
    }

    // Filter workouts when search query changes
    LaunchedEffect(searchQuery.value) {
        filteredWorkouts.value = if (searchQuery.value.isEmpty()) {
            workouts.value
        } else {
            workouts.value.filter { workout ->
                workout.name.contains(searchQuery.value, ignoreCase = true)
            }
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Workouts") },
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
                        if (searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) { }
            }
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateExercise.route) },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
                FloatingActionButton(
                    onClick = { showCreateWorkoutDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Workout")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredWorkouts.value) { workout ->
                WorkoutCard(
                    workout = workout,
                    onClick = { navController.navigate(Screen.Routes.workoutDetails(workout.id)) }
                )
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
}