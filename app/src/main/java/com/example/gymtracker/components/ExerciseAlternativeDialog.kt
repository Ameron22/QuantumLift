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
import androidx.compose.material.icons.filled.FilterList
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
    onDismiss: () -> Unit,
    onSelectAlternative: (EntityExercise) -> Unit,
    dao: ExerciseDao
) {
    var similarExercises by remember { mutableStateOf<List<EntityExercise>>(emptyList()) }
    var isLoadingSimilar by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Filter states
    var selectedEquipment by remember { mutableStateOf(currentExercise.equipment) }
    var selectedDifficulty by remember { mutableStateOf(currentExercise.difficulty) }
    
    // Dropdown expansion states
    var equipmentExpanded by remember { mutableStateOf(false) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    
    // Get unique values for filter dropdowns
    var allEquipments by remember { mutableStateOf<List<String>>(emptyList()) }
    var allDifficulties by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load filter options
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val allExercises = dao.getAllExercises()
                allEquipments = allExercises.map { it.equipment }.distinct().sorted()
                allDifficulties = allExercises.map { it.difficulty }.distinct().sorted()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Load similar exercises when dialog opens or filters change
    LaunchedEffect(currentExercise, selectedEquipment, selectedDifficulty) {
        coroutineScope.launch {
            try {
                isLoadingSimilar = true
                
                // Get exercises based on current filters
                val filteredExercises = dao.getFilteredSimilarExercises(
                    muscleGroup = currentExercise.muscle, // Always use current exercise's muscle group
                    equipment = selectedEquipment,
                    difficulty = selectedDifficulty,
                    excludeId = currentExercise.id,
                    limit = 20
                )
                
                similarExercises = filteredExercises
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
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with filter button
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
                    Row {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters"
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Current exercise info with GIF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exercise GIF
                        if (currentExercise.gifUrl.isNotEmpty()) {
                            ExerciseGif(
                                gifPath = currentExercise.gifUrl,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Current: ${currentExercise.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${currentExercise.muscle} • ${currentExercise.equipment} • ${currentExercise.difficulty}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filter section
                if (showFilters) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Equipment filter
                            ExposedDropdownMenuBox(
                                expanded = equipmentExpanded,
                                onExpandedChange = { equipmentExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedEquipment,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Equipment") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = equipmentExpanded,
                                    onDismissRequest = { equipmentExpanded = false }
                                ) {
                                    allEquipments.forEach { equipment ->
                                        DropdownMenuItem(
                                            text = { Text(equipment) },
                                            onClick = { 
                                                selectedEquipment = equipment
                                                equipmentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Difficulty filter
                            ExposedDropdownMenuBox(
                                expanded = difficultyExpanded,
                                onExpandedChange = { difficultyExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedDifficulty,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Difficulty") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = difficultyExpanded,
                                    onDismissRequest = { difficultyExpanded = false }
                                ) {
                                    allDifficulties.forEach { difficulty ->
                                        DropdownMenuItem(
                                            text = { Text(difficulty) },
                                            onClick = { 
                                                selectedDifficulty = difficulty
                                                difficultyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Show similar exercises
                Text(
                    text = "Similar Exercises (${similarExercises.size})",
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
                    } else if (similarExercises.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No exercises found with current filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(similarExercises) { exercise ->
                            AlternativeExerciseItemWithGif(
                                exercise = exercise,
                                isActive = false,
                                onClick = { onSelectAlternative(exercise) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativeExerciseItemWithGif(
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise GIF
            if (exercise.gifUrl.isNotEmpty()) {
                ExerciseGif(
                    gifPath = exercise.gifUrl,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${exercise.muscle} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Difficulty: ${exercise.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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


