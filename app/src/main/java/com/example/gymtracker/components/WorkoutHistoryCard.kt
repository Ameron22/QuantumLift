package com.example.gymtracker.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.classes.SessionWorkoutWithMuscles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryCard(
    session: SessionWorkoutWithMuscles,
    onClick: () -> Unit
) {
    // Debug logging
    android.util.Log.d("WorkoutHistoryCard", "Rendering session: ID=${session.sessionId}, Name='${session.workoutName}', Start=${session.startTime}, End=${session.endTime}")
    
    val instant = Instant.ofEpochMilli(session.startTime)
    val zoneId = ZoneId.of("Europe/London") // Use London timezone
    val localDateTime = instant.atZone(zoneId)
    
    val formattedDate = localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    val formattedTime = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    
    // Function to format duration in MM:SS or HH:MM:SS format
    fun formatDuration(durationInMillis: Long): String {
        val durationInSeconds = durationInMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.workoutName ?: "Unnamed Workout",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${formatDuration(session.endTime - session.startTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Time: $formattedTime",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (session.muscleGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Muscles worked: ${session.muscleGroups.keys.joinToString(", ") { it.replaceFirstChar { char -> char.uppercase() } }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 