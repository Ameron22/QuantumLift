package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.classes.SessionWorkoutWithMuscles
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutHistoryCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SectionSpacing = 16.dp
private val ItemPadding = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadHistoryScreen(navController: NavController, viewModel: HistoryViewModel) {
    val workoutSessions by viewModel.workoutSessions.collectAsState()
    val muscleSoreness by viewModel.muscleSoreness.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display muscle soreness section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Muscle Recovery Status",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        muscleSoreness.forEach { (muscle, soreness) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = muscle)
                                Text(
                                    text = soreness,
                                    color = when (soreness) {
                                        "Very Sore" -> Color.Red
                                        "Slightly Sore" -> Color.Yellow
                                        else -> Color.Green
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Display workout history
            items(workoutSessions) { session ->
                WorkoutHistoryCard(
                    session = session,
                    onClick = { /* Handle click if needed */ }
                )
            }
        }
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