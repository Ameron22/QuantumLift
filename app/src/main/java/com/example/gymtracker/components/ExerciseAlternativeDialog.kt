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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlowRow(
    items: List<String>,
    selectedItems: List<String>,
    onItemClick: (String) -> Unit,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Int = 8
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        FilterChip(
            selected = selectedItems.isEmpty(),
            onClick = onAllClick,
            label = { 
                Text(
                    "All", 
                    maxLines = 1,
                    color = Color(0xFF2196F3) // Blue color for the first "All"
                ) 
            },
        )
        items.forEach { item ->
            FilterChip(
                selected = selectedItems.contains(item),
                onClick = { onItemClick(item) },
                label = {
                    Text(
                        text = item,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = when (item) {
                            "Beginner" -> Color(0xFF4CAF50)
                            "Intermediate" -> Color(0xFFFFA000)
                            "Advanced" -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            )
        }
    }
}

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

    // Filter states - start with no filters applied (using lists for multiple selection)
    var selectedEquipment by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDifficulty by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Get unique values for filter chips - from all exercises in the muscle group
    var allEquipments by remember { mutableStateOf<List<String>>(emptyList()) }
    var allDifficulties by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load filter options based on all exercises in the muscle group (not filtered results)
    LaunchedEffect(currentExercise) {
        coroutineScope.launch {
            try {
                // Get all exercises for this muscle group to populate filter options
                val muscleGroupExercises = dao.getExercisesByMuscleGroup(
                    muscleGroup = currentExercise.muscle,
                    excludeId = currentExercise.id,
                    limit = 1000 // Get a large number to ensure we have all options
                )
                
                // Split equipment combinations into individual items
                allEquipments = muscleGroupExercises.flatMap { exercise ->
                    if (exercise.equipment.isNotBlank()) {
                        exercise.equipment.split(",").map { it.trim() }
                    } else {
                        listOf("None")
                    }
                }.distinct().sorted()
                allDifficulties = muscleGroupExercises.map { it.difficulty }.distinct().sorted()
            } catch (e: Exception) {
                // Handle error - keep empty lists
                allEquipments = emptyList()
                allDifficulties = emptyList()
            }
        }
    }

    // Update available filter options based on current selections (cross-filtering)
    LaunchedEffect(selectedEquipment, selectedDifficulty) {
        coroutineScope.launch {
            try {
                // Get all exercises for this muscle group
                val muscleGroupExercises = dao.getExercisesByMuscleGroup(
                    muscleGroup = currentExercise.muscle,
                    excludeId = currentExercise.id,
                    limit = 1000
                )
                
                // Apply cross-filtering based on current selections
                val filteredForOptions = muscleGroupExercises.filter { exercise ->
                    val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                        exercise.equipment.split(",").map { it.trim() }
                    } else {
                        listOf("None")
                    }
                    
                    val matchesEquipment = selectedEquipment.isEmpty() || 
                        selectedEquipment.any { selected -> exerciseEquipment.contains(selected) }
                    val matchesDifficulty = selectedDifficulty.isEmpty() || 
                        selectedDifficulty.contains(exercise.difficulty)
                    
                    matchesEquipment && matchesDifficulty
                }
                
                // Update available equipment options based on current difficulty selection
                if (selectedDifficulty.isNotEmpty()) {
                    // Difficulty is selected - filter equipment options
                    allEquipments = filteredForOptions.flatMap { exercise ->
                        if (exercise.equipment.isNotBlank()) {
                            exercise.equipment.split(",").map { it.trim() }
                        } else {
                            listOf("None")
                        }
                    }.distinct().sorted()
                } else {
                    // Difficulty is "All" - show all equipment options
                    allEquipments = muscleGroupExercises.flatMap { exercise ->
                        if (exercise.equipment.isNotBlank()) {
                            exercise.equipment.split(",").map { it.trim() }
                        } else {
                            listOf("None")
                        }
                    }.distinct().sorted()
                }
                
                // Update available difficulty options based on current equipment selection
                if (selectedEquipment.isNotEmpty()) {
                    // Equipment is selected - filter difficulty options
                    allDifficulties = filteredForOptions.map { it.difficulty }.distinct().sorted()
                } else {
                    // Equipment is "All" - show all difficulty options
                    allDifficulties = muscleGroupExercises.map { it.difficulty }.distinct().sorted()
                }
                
                // Clear invalid selections that are no longer available
                selectedEquipment = selectedEquipment.filter { it in allEquipments }
                selectedDifficulty = selectedDifficulty.filter { it in allDifficulties }
            } catch (e: Exception) {
                // Handle error - keep current options
            }
        }
    }

    // Load similar exercises when dialog opens or filters change
    LaunchedEffect(currentExercise, selectedEquipment, selectedDifficulty) {
        coroutineScope.launch {
            try {
                isLoadingSimilar = true
                
                // Get exercises - use filtered method only if filters are applied
                val filteredExercises = if (selectedEquipment.isNotEmpty() || selectedDifficulty.isNotEmpty()) {
                    // For multiple selections, we need to get all exercises and filter them manually
                    // since the DAO method only supports single equipment/difficulty
                    val allMuscleGroupExercises = dao.getExercisesByMuscleGroup(
                        muscleGroup = currentExercise.muscle,
                        excludeId = currentExercise.id,
                        limit = 1000
                    )
                    
                    // Apply manual filtering for multiple selections
                    allMuscleGroupExercises.filter { exercise ->
                        val matchesEquipment = selectedEquipment.isEmpty() || 
                            selectedEquipment.any { selected -> 
                                val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                                    exercise.equipment.split(",").map { it.trim() }
                                } else {
                                    listOf("None")
                                }
                                exerciseEquipment.contains(selected)
                            }
                        val matchesDifficulty = selectedDifficulty.isEmpty() || 
                            selectedDifficulty.contains(exercise.difficulty)
                        matchesEquipment && matchesDifficulty
                    }.take(20)
                } else {
                    // Use simple method for initial load (no filters applied)
                    dao.getSimilarExercisesWithParsedParts(
                        muscleGroup = currentExercise.muscle,
                        muscleParts = currentExercise.parts,
                        excludeId = currentExercise.id,
                        limit = 20
                    )
                }
                
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { showFilters = false }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Hide Filters",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Equipment filter chips
                            Text(
                                text = "Equipment",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            FilterChipFlowRow(
                                items = allEquipments,
                                selectedItems = selectedEquipment,
                                onItemClick = { equipment ->
                                    if (selectedEquipment.contains(equipment)) {
                                        selectedEquipment = selectedEquipment.filter { it != equipment }
                                    } else {
                                        selectedEquipment = selectedEquipment + equipment
                                    }
                                },
                                onAllClick = { selectedEquipment = emptyList() },
                                modifier = Modifier.fillMaxWidth(),
                                spacing = 8
                            )
                            
                            // Difficulty filter chips
                            Text(
                                text = "Difficulty",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            FilterChipFlowRow(
                                items = allDifficulties,
                                selectedItems = selectedDifficulty,
                                onItemClick = { difficulty ->
                                    if (selectedDifficulty.contains(difficulty)) {
                                        selectedDifficulty = selectedDifficulty.filter { it != difficulty }
                                    } else {
                                        selectedDifficulty = selectedDifficulty + difficulty
                                    }
                                },
                                onAllClick = { selectedDifficulty = emptyList() },
                                modifier = Modifier.fillMaxWidth(),
                                spacing = 8
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Show similar exercises with filter icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Similar Exercises (${similarExercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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


