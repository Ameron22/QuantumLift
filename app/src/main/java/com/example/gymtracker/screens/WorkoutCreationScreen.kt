package com.example.gymtracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.gymtracker.R
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.CrossRefWorkoutExercise
import com.example.gymtracker.data.Exercise
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.viewmodels.WorkoutCreationViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCreationScreen(
    navController: NavController,
    viewModel: WorkoutCreationViewModel = viewModel()
) {
    var editIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

    // Collect state from ViewModel
    val workoutName by viewModel.workoutName.collectAsState()
    val exercisesList by viewModel.exercisesList.collectAsState()

    // Add LaunchedEffect to handle navigation result
    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        println("WorkoutCreationScreen: Checking for new exercise")
        val newExercise = navController.currentBackStackEntry?.savedStateHandle?.get<Exercise>("newExercise")
        println("WorkoutCreationScreen: New exercise from savedStateHandle: ${newExercise?.name}")
        
        if (newExercise != null) {
            println("WorkoutCreationScreen: Adding new exercise to list")
            viewModel.addExercise(newExercise)
            println("WorkoutCreationScreen: Exercise added, clearing savedStateHandle")
            navController.currentBackStackEntry?.savedStateHandle?.remove<Exercise>("newExercise")
            println("WorkoutCreationScreen: Current exercises list size: ${exercisesList.size}")
        }
    }

    // Add LaunchedEffect to observe exercises list changes
    LaunchedEffect(exercisesList) {
        println("WorkoutCreationScreen: exercisesList changed, new size: ${exercisesList.size}")
        exercisesList.forEachIndexed { index, exercise ->
            println("WorkoutCreationScreen: Exercise $index: ${exercise.name}")
        }
    }

    // Add LaunchedEffect to handle exercise updates
    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        println("WorkoutCreationScreen: Checking for updated exercise")
        val updatedExercise = navController.currentBackStackEntry?.savedStateHandle?.get<EntityExercise>("updatedExercise")
        println("WorkoutCreationScreen: Updated exercise from savedStateHandle: ${updatedExercise?.name}")
        
        val currentEditIndex = editIndex
        if (updatedExercise != null && currentEditIndex != null) {
            println("WorkoutCreationScreen: Updating exercise at index $currentEditIndex")
            viewModel.updateExercise(
                currentEditIndex,
                Exercise(
                    name = updatedExercise.name,
                    sets = updatedExercise.sets,
                    weight = updatedExercise.weight,
                    reps = updatedExercise.reps,
                    muscle = updatedExercise.muscle,
                    part = updatedExercise.part
                )
            )
            println("WorkoutCreationScreen: Exercise updated, clearing savedStateHandle")
            navController.currentBackStackEntry?.savedStateHandle?.remove<EntityExercise>("updatedExercise")
        }
    }

    // Add logging for navigation
    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        println("WorkoutCreationScreen: BackStackEntry changed")
        println("WorkoutCreationScreen: Current route: ${navController.currentDestination?.route}")
        println("WorkoutCreationScreen: Current exercises list size: ${exercisesList.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Create a New Workout",
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = workoutName,
                onValueChange = { viewModel.updateWorkoutName(it) },
                label = { Text("Workout Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            // Display Exercises List
            if (exercisesList.isNotEmpty()) {
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                exercisesList.forEachIndexed { index, exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            editIndex = index
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeExercise(index)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${exercise.sets} Sets",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (exercise.reps > 50) {
                                    val timeInSeconds = exercise.reps - 1000
                                    val minutes = timeInSeconds / 60
                                    val seconds = timeInSeconds % 60
                                    Text(
                                        text = "${minutes}:${String.format("%02d", seconds)} Time",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = "${exercise.reps} Reps",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "${exercise.weight}kg",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${exercise.muscle} - ${exercise.part.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Add Exercise Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add Existing Exercise Button
                Button(
                    onClick = {
                        println("WorkoutCreationScreen: Navigating to AddExerciseToWorkout")
                        navController.navigate(Screen.AddExerciseToWorkout.createRoute(0)) {
                            popUpTo(Screen.WorkoutCreation.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        // Set the returnTo value in the destination's savedStateHandle
                        navController.currentBackStackEntry?.savedStateHandle?.set("returnTo", "workout_creation")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus_icon),
                            contentDescription = "Add Exercise",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Exercise")
                    }
                }

                // Create New Exercise Button
                Button(
                    onClick = {
                        navController.navigate(Screen.CreateExercise.route) {
                            popUpTo(Screen.WorkoutCreation.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.currentBackStackEntry?.savedStateHandle?.set("returnTo", "workout_creation")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus_icon),
                            contentDescription = "Create Exercise",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Exercise")
                    }
                }
            }

            // Save Workout Button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val workoutId = dao.insertWorkout(EntityWorkout(name = workoutName)).toInt()
                            exercisesList.forEach { exercise ->
                                val exerciseId = dao.insertExercise(
                                    EntityExercise(
                                        name = exercise.name,
                                        sets = exercise.sets,
                                        weight = exercise.weight,
                                        reps = exercise.reps,
                                        muscle = exercise.muscle,
                                        part = exercise.part,
                                        gifUrl = exercise.gifUrl
                                    )
                                ).toInt()
                                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workoutId, exerciseId))
                            }
                            println("Workout Saved!")
                            viewModel.clearWorkout()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            println("Error saving workout: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = workoutName.isNotEmpty() && exercisesList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Workout")
            }
        }
    }
}

