package com.example.gymtracker.screens

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.example.gymtracker.data.WorkoutWithExercises


@Composable
fun WorkoutDetailsScreen(workoutId: Int, navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }

    LaunchedEffect(workoutId) {
        try {
            workoutWithExercises = dao.getWorkoutWithExercises(workoutId)
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = workoutWithExercises?.firstOrNull()?.workout?.name ?: "Loading...",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        workoutWithExercises?.flatMap { it.exercises }?.forEach { exercise ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${exercise.muscle} - ${exercise.part}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var sets by remember { mutableStateOf(exercise.sets) }
                            var reps by remember { mutableStateOf(exercise.reps) }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { sets+=1 }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Sets")
                                }
                                Text("$sets Sets")
                                IconButton(onClick = { if (sets > 1) sets-=1 }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Sets")
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { reps+=1 }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Reps")
                                }
                                Text("$reps Reps")
                                IconButton(onClick = { if (reps > 1) reps-=1 }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Reps")
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back")
        }
    }
}