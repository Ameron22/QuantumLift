package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.classes.WorkoutSessionWithMuscles
import com.example.gymtracker.data.WorkoutSessionEntity


@Composable
fun LoadHistoryScreen(navController: NavController, viewModel: HistoryViewModel) {
    val workoutSessions by viewModel.workoutSessions.collectAsState()
    // Calculate muscle soreness and progress data dynamically
    val muscleSoreness = viewModel.calculateMuscleSoreness()

    // Render the UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Workout Sessions List
        Text("Workout Sessions", style = MaterialTheme.typography.headlineMedium)
        LazyColumn {
            items(workoutSessions) { session ->
                WorkoutSessionCard(session)
            }
        }

        // Muscle Soreness
        Text("Muscle Soreness", style = MaterialTheme.typography.headlineMedium)
        MuscleGroupOverview(muscleSoreness)
/*
        // Progress Graphs    Later
        Text("Progress Over Time", style = MaterialTheme.typography.headlineMedium)
        ProgressGraphs(progressData)

         For later
         */
    }
}


@Composable
fun WorkoutSessionCard(session: WorkoutSessionWithMuscles) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = session.workoutName!!, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Duration: ${session.duration} mins", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Muscles: ${session.muscleGroups.keys.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MuscleGroupOverview(muscleSoreness: Map<String, String>) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        muscleSoreness.forEach { (muscle, soreness) ->
            MuscleChip(muscle = muscle, soreness = soreness)
        }
    }
}

@Composable
fun MuscleChip(muscle: String, soreness: String) {
    val backgroundColor = when (soreness) {
        "Fresh" -> Color.Green
        "Slightly Sore" -> Color.Yellow
        "Very Sore" -> Color.Red
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = muscle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = soreness,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}