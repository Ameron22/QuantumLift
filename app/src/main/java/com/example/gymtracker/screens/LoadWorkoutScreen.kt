package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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


import com.example.gymtracker.Screen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gymtracker.data.ExerciseEntity
import com.example.gymtracker.data.WorkoutExerciseCrossRef

@Composable
fun LoadWorkoutScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

    // State to hold the list of workouts
    var workouts: List<WorkoutEntity> by remember { mutableStateOf(emptyList()) }
    var exercisesMap: Map<Int, List<ExerciseEntity>> by remember { mutableStateOf(emptyMap()) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Load Workout",
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.headlineMedium
        )


        // Display the list of workouts
        if (workouts.isEmpty()) {
            Text(
                text = "No workouts found.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workouts) { workout ->
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