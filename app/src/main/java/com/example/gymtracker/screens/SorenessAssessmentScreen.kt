package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.SorenessAssessment

/**
 * Soreness assessment screen for collecting user feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SorenessAssessmentScreen(
    sessionId: Long,
    muscleGroups: List<String>,
    assessmentDay: Int,
    onAssessmentComplete: (SorenessAssessment) -> Unit,
    onCancel: () -> Unit
) {
    var sorenessRatings by remember { mutableStateOf(mapOf<String, Int>()) }
    var overallSoreness by remember { mutableStateOf(5) }
    var notes by remember { mutableStateOf("") }
    var wasActive by remember { mutableStateOf(true) }
    var sleepQuality by remember { mutableStateOf(5) }
    var stressLevel by remember { mutableStateOf(5) }
    
    val timeText = if (assessmentDay == 1) "3 minutes ago" else "6 minutes ago"
    val workoutText = if (assessmentDay == 1) "3 minutes ago" else "6 minutes ago"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Soreness Assessment",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How are you feeling after your workout ($timeText)?",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Muscle group specific ratings
        Text(
            text = "Rate soreness for each muscle group:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                muscleGroups.forEach { muscleGroup ->
                    SorenessRatingItem(
                        muscleGroup = muscleGroup,
                        rating = sorenessRatings[muscleGroup] ?: 5,
                        onRatingChange = { newRating ->
                            sorenessRatings = sorenessRatings + (muscleGroup to newRating)
                        }
                    )
                    if (muscleGroup != muscleGroups.last()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Overall soreness
        Text(
            text = "Overall body soreness:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            OverallSorenessRating(
                rating = overallSoreness,
                onRatingChange = { overallSoreness = it }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Context questions
        Text(
            text = "Additional context (optional):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Activity level
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Were you active between the workout and now?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { wasActive = true },
                        label = { Text("Yes") },
                        selected = wasActive
                    )
                    FilterChip(
                        onClick = { wasActive = false },
                        label = { Text("No") },
                        selected = !wasActive
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Sleep quality
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Sleep quality last night (1-10):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sleepQuality.toFloat(),
                    onValueChange = { sleepQuality = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text(
                    text = "Quality: $sleepQuality/10",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Stress level
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Stress level today (1-10):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = stressLevel.toFloat(),
                    onValueChange = { stressLevel = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text(
                    text = "Stress: $stressLevel/10",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val assessment = SorenessAssessment(
                        sessionId = sessionId,
                        exerciseId = 0, // Will be updated when we have exercise context
                        muscleGroups = muscleGroups,
                        muscleParts = emptyList(), // Will be populated from exercise data
                        soreness24hr = sorenessRatings,
                        soreness48hr = null,
                        overallSoreness = overallSoreness,
                        assessmentDay = assessmentDay,
                        timestamp = System.currentTimeMillis(),
                        notes = notes.ifEmpty { null },
                        wasActive = wasActive,
                        sleepQuality = sleepQuality,
                        stressLevel = stressLevel
                    )
                    onAssessmentComplete(assessment)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit Assessment")
            }
        }
    }
}

@Composable
fun SorenessRatingItem(
    muscleGroup: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = muscleGroup.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..10).forEach { score ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (score <= rating) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                }
                            )
                            .clickable { onRatingChange(score) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (score <= rating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getSorenessDescription(rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun OverallSorenessRating(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Overall Body Soreness",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..10).forEach { score ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (score <= rating) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                }
                            )
                            .clickable { onRatingChange(score) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (score <= rating) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getSorenessDescription(rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getSorenessDescription(rating: Int): String {
    return when (rating) {
        1 -> "No soreness"
        2, 3 -> "Very mild soreness"
        4, 5 -> "Mild soreness"
        6, 7 -> "Moderate soreness"
        8, 9 -> "Severe soreness"
        10 -> "Extreme soreness"
        else -> ""
    }
}
