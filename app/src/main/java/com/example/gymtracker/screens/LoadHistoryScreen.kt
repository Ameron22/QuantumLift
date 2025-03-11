package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.classes.SessionWorkoutWithMuscles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SectionSpacing = 16.dp
private val ItemPadding = 8.dp

@Composable
fun LoadHistoryScreen(navController: NavController, viewModel: HistoryViewModel) {
    val workoutSessions by viewModel.workoutSessions.collectAsState()
    // Calculate muscle soreness and progress data dynamically
    val muscleSoreness by remember { derivedStateOf { viewModel.calculateMuscleSoreness() } }



    // Render the UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SectionSpacing)
            //.verticalScroll(rememberScrollState())
    ) {
        // Workout Sessions List
        Text(
            text = "Workout Sessions",
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Flexible height for dynamic content
                .padding(top = 8.dp)
        ) {
            items(workoutSessions) { session ->
                WorkoutSessionCard(session)
            }
        }

        // Muscle Soreness
        Spacer(modifier = Modifier.height(SectionSpacing))
        Text(
            text = "Muscle Soreness",
            style = MaterialTheme.typography.headlineMedium
        )
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
fun WorkoutSessionCard(session: SessionWorkoutWithMuscles) {
    val formattedDate = Instant.ofEpochMilli(session.startTime)
        .atZone(ZoneId.systemDefault()) // Use device's time zone
        .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) // e.g., "March 10, 2025"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ItemPadding),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.workoutName ?: "Unnamed Workout",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Duration: ${session.duration} mins",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Muscles: ${session.muscleGroups.keys.joinToString(", ") { it.capitalize() }}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Date: $formattedDate",
                style = MaterialTheme.typography.bodyMedium
            )
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
        "Fresh" -> MaterialTheme.colorScheme.primary
        "Slightly Sore" -> MaterialTheme.colorScheme.secondary
        "Very Sore" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = MaterialTheme.colorScheme.onSurface // Adjust based on background

    Box(
        modifier = Modifier
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = muscle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            Text(
                text = soreness,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}