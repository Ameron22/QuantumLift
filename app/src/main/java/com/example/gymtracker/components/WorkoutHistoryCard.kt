package com.example.gymtracker.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D3748).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with workout name and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.workoutName ?: "Unnamed Workout",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF38B2AC),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E0),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Duration and time info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E0),
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF38B2AC)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = formatDuration(session.endTime - session.startTime),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E0),
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4A5568).copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Muscle groups
            if (session.muscleGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Muscles Worked",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF38B2AC),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.muscleGroups.keys.joinToString(", ") { it.replaceFirstChar { char -> char.uppercase() } },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
} 