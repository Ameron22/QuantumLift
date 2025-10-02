package com.example.gymtracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.ExerciseAlternativeWithDetails
import com.example.gymtracker.data.ExerciseDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAlternativeDialog(
    currentExercise: EntityExercise,
    workoutExerciseId: Int,
    alternatives: List<ExerciseAlternativeWithDetails>,
    onDismiss: () -> Unit,
    onSelectAlternative: (EntityExercise) -> Unit,
    onAddAlternative: () -> Unit,
    dao: ExerciseDao
) {
    var similarExercises by remember { mutableStateOf<List<EntityExercise>>(emptyList()) }
    var isLoadingSimilar by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Load similar exercises when dialog opens
    LaunchedEffect(currentExercise) {
        coroutineScope.launch {
            try {
                // First try to find exercises with same muscle group and equipment
                val sameEquipment = dao.getSimilarExercises(
                    muscleGroup = currentExercise.muscle,
                    equipment = currentExercise.equipment,
                    excludeId = currentExercise.id,
                    limit = 5
                )
                
                // If not enough, get more from same muscle group
                val moreFromMuscleGroup = if (sameEquipment.size < 5) {
                    dao.getExercisesByMuscleGroup(
                        muscleGroup = currentExercise.muscle,
                        excludeId = currentExercise.id,
                        limit = 10 - sameEquipment.size
                    )
                } else {
                    emptyList()
                }
                
                similarExercises = sameEquipment + moreFromMuscleGroup
            } catch (e: Exception) {
                similarExercises = emptyList()
            } finally {
                isLoadingSimilar = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exercise Alternatives",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Current exercise info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Current: ${currentExercise.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${currentExercise.muscle} • ${currentExercise.equipment}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Existing alternatives section
                if (alternatives.isNotEmpty()) {
                    Text(
                        text = "Your Alternatives",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alternatives) { alternativeWithDetails ->
                            AlternativeExerciseItem(
                                exercise = alternativeWithDetails.exercise,
                                isActive = alternativeWithDetails.alternative.isActive,
                                onClick = { onSelectAlternative(alternativeWithDetails.exercise) }
                            )
                        }
                        
                        // Add new alternative button
                        item {
                            AddAlternativeButton(
                                onClick = onAddAlternative
                            )
                        }
                    }
                } else {
                    // No alternatives yet - show similar exercises
                    Text(
                        text = "Similar Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoadingSimilar) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            items(similarExercises) { exercise ->
                                AlternativeExerciseItem(
                                    exercise = exercise,
                                    isActive = false,
                                    onClick = { onSelectAlternative(exercise) }
                                )
                            }
                            
                            // Add new alternative button
                            item {
                                AddAlternativeButton(
                                    onClick = onAddAlternative
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativeExerciseItem(
    exercise: EntityExercise,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${exercise.muscle} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun AddAlternativeButton(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alternative",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add New Alternative",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

