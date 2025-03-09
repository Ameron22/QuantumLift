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
import com.example.gymtracker.data.WorkoutEntity
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.example.gymtracker.data.ExerciseEntity

@Composable
fun LoadWorkoutScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

    // State to hold the list of workouts
    var workouts: List<WorkoutEntity> by remember { mutableStateOf(emptyList()) }
    var exercisesMap: Map<Int, List<ExerciseEntity>> by remember { mutableStateOf(emptyMap()) }

    // State for search text
    var searchText by remember { mutableStateOf("") }

    // Fetch workouts when the screen is loaded
    LaunchedEffect(Unit) {
        scope.launch {
            workouts = dao.getAllWorkouts()
            exercisesMap = workouts.associate { workout ->
                val workoutWithExercises = dao.getWorkoutWithExercises(workout.id)
                workout.id to workoutWithExercises.flatMap { it.exercises }
            }
        }
    }

    // Filter workouts based on search text
    val filteredWorkouts = workouts.filter { workout ->
        workout.name.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center // Center the content horizontally
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search Workouts") },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 40.dp), // Adjust height if needed
                singleLine = true,
                shape = RoundedCornerShape(20.dp), // Make the search bar more rounded
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        modifier = Modifier.size(20.dp) // Adjust icon size
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = { searchText = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                modifier = Modifier.size(20.dp) // Adjust icon size
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Text(
            text = "Load Workout",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.headlineMedium
        )


        // Display the list of filtered workouts
        if (workouts.isEmpty()) {
            Text(
                text = if (searchText.isEmpty()) "No workouts found." else "No matching workouts found.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredWorkouts) { workout ->
                    val exercises = exercisesMap[workout.id] ?: emptyList()
                    WorkoutItem(
                        workout = workout,
                        exercises = exercises,
                        onClick = {
                        // Handle workout selection (e.g., navigate to details)
                        navController.navigate("workoutDetails/${workout.id}")
                    })
                }
            }
        }
    }
}

@Composable
fun WorkoutItem(workout: WorkoutEntity,exercises: List<ExerciseEntity>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercises.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}