package com.example.gymtracker.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.Exercise
import com.example.gymtracker.data.Converter
import com.example.gymtracker.utils.GifUtils
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.data.AchievementManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(navController: NavController) {
    var currentExercise by remember { mutableStateOf("") }
    var currentDescription by remember { mutableStateOf("") }
    var useTime by remember { mutableStateOf(false) }
    var currentMuscle by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf("") }
    var currentDifficulty by remember { mutableStateOf("Intermediate") }
    var currentEquipment by remember { mutableStateOf("") }
    var equipmentSearchQuery by remember { mutableStateOf("") }
    var selectedParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMuscleGroupDropdownExpanded by remember { mutableStateOf(false) }
    var isPartDropdownExpanded by remember { mutableStateOf(false) }
    var isDifficultyDropdownExpanded by remember { mutableStateOf(false) }
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var existingEquipment by remember { mutableStateOf<List<String>>(emptyList()) }
    var gifPath by remember { mutableStateOf<String?>(null) }
    var showGifError by remember { mutableStateOf(false) }
    var gifErrorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    // Load existing equipment from database
    LaunchedEffect(Unit) {
        try {
            val allExercises = dao.getAllExercises()
            val equipmentSet = mutableSetOf<String>()
            allExercises.forEach { exercise ->
                if (exercise.equipment.isNotBlank()) {
                    // Split by comma and add each equipment type
                    exercise.equipment.split(",").forEach { equipment ->
                        equipmentSet.add(equipment.trim())
                    }
                }
            }
            existingEquipment = equipmentSet.toList().sorted()
        } catch (e: Exception) {
            Log.e("CreateExerciseScreen", "Error loading existing equipment: ${e.message}")
        }
    }

    // Launcher for picking GIF from gallery with better error handling
    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                Log.d("CreateExerciseScreen", "GIF selected: $uri")
                
                // Validate that it's actually a GIF file
                if (!GifUtils.isValidGifFile(context, uri)) {
                    showGifError = true
                    gifErrorMessage = "Please select a valid GIF file."
                    Log.e("CreateExerciseScreen", "Invalid file type selected")
                    return@rememberLauncherForActivityResult
                }
                
                val savedPath = GifUtils.saveGifToInternalStorage(context, uri)
                if (savedPath != null) {
                    gifPath = savedPath
                    showGifError = false
                    gifErrorMessage = ""
                    Log.d("CreateExerciseScreen", "GIF saved successfully: $savedPath")
                } else {
                    showGifError = true
                    gifErrorMessage = "Failed to save GIF. Please try again."
                    Log.e("CreateExerciseScreen", "Failed to save GIF")
                }
            } catch (e: Exception) {
                showGifError = true
                gifErrorMessage = "Error processing GIF: ${e.message}"
                Log.e("CreateExerciseScreen", "Error processing GIF", e)
            }
        }
    }

    val musclePartsMap = mapOf(
        "Chest" to listOf("Upper Chest", "Middle Chest", "Lower Chest"),
        "Shoulder" to listOf("Front Shoulders", "Side Shoulders", "Rear Shoulders"),
        "Back" to listOf("Upper Back", "Lats", "Lower Back"),
        "Arms" to listOf("Biceps", "Triceps", "Forearms"),
        "Legs" to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"),
        "Core" to listOf("Abs", "Obliques", "Lower Back"),
        "Neck" to listOf("Side neck muscle", "Upper Traps")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Create a New Exercise",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .clickable { focusManager.clearFocus() },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = currentExercise,
                onValueChange = { currentExercise = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            // Description field
            OutlinedTextField(
                value = currentDescription,
                onValueChange = { currentDescription = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            // GIF Selection Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Exercise GIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // GIF Preview
                    if (gifPath != null) {
                        // Debug logging
                        LaunchedEffect(gifPath) {
                            Log.d("CreateExerciseScreen", "GIF path for preview: $gifPath")
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ExerciseGif(
                                    gifPath = gifPath!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Remove GIF button
                                IconButton(
                                    onClick = {
                                        gifPath = null
                                        showGifError = false
                                        gifErrorMessage = ""
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove GIF",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // GIF Selection Button
                    Button(
                        onClick = { gifPickerLauncher.launch("image/gif") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Select GIF",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (gifPath != null) "Change GIF" else "Select GIF")
                    }
                    
                    // Error message
                    if (showGifError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = gifErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Switch between Reps and Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Track by Repetitions or Time?")
                Switch(
                    checked = useTime,
                    onCheckedChange = { useTime = it }
                )
            }

            // Difficulty selection section (moved up)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                focusManager.clearFocus()
                                isDifficultyDropdownExpanded = true 
                            },
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Difficulty Level",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = currentDifficulty,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when (currentDifficulty) {
                                        "Beginner" -> MaterialTheme.colorScheme.primary
                                        "Intermediate" -> MaterialTheme.colorScheme.secondary
                                        "Advanced" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Icon"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDifficultyDropdownExpanded,
                        onDismissRequest = { isDifficultyDropdownExpanded = false }
                    ) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { difficulty ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = difficulty,
                                        color = when (difficulty) {
                                            "Beginner" -> MaterialTheme.colorScheme.primary
                                            "Intermediate" -> MaterialTheme.colorScheme.secondary
                                            "Advanced" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    currentDifficulty = difficulty
                                    isDifficultyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Equipment selection section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Equipment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Equipment input field
                    OutlinedTextField(
                        value = currentEquipment,
                        onValueChange = { 
                            currentEquipment = it
                            equipmentSearchQuery = it
                        },
                        label = { Text("Enter equipment or select from list") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    isEquipmentDropdownExpanded = !isEquipmentDropdownExpanded
                                    equipmentSearchQuery = ""
                                }
                            ) {
                                androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (isEquipmentDropdownExpanded) 180f else 0f,
                                    label = "arrow_rotation"
                                ).value.let { rotation ->
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Show equipment options",
                                        modifier = Modifier.graphicsLayer(rotationZ = rotation)
                                    )
                                }
                            }
                        }
                    )
                    
                    // Equipment dropdown
                    if (isEquipmentDropdownExpanded) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(8.dp)
                            ) {
                                // Filtered equipment list
                                val filteredEquipment = if (equipmentSearchQuery.isBlank()) {
                                    existingEquipment
                                } else {
                                    existingEquipment.filter { 
                                        it.lowercase().contains(equipmentSearchQuery.lowercase()) 
                                    }
                                }
                                
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredEquipment) { equipment ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentEquipment = equipment
                                                    isEquipmentDropdownExpanded = false
                                                    equipmentSearchQuery = ""
                                                },
                                            shape = MaterialTheme.shapes.small,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Text(
                                                text = equipment,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    
                                    // Add custom equipment option if search query doesn't match existing
                                    if (equipmentSearchQuery.isNotBlank() && !filteredEquipment.contains(equipmentSearchQuery)) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        currentEquipment = equipmentSearchQuery
                                                        isEquipmentDropdownExpanded = false
                                                        equipmentSearchQuery = ""
                                                    },
                                                shape = MaterialTheme.shapes.small,
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add new equipment",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Add \"$equipmentSearchQuery\"",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Muscle selection section (moved down)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Muscle Group Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                focusManager.clearFocus()
                                isMuscleGroupDropdownExpanded = true 
                            },
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (currentMuscle.isNotEmpty()) currentMuscle else "Muscle Group",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Icon"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isMuscleGroupDropdownExpanded,
                        onDismissRequest = { isMuscleGroupDropdownExpanded = false }
                    ) {
                        musclePartsMap.keys.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle) },
                                onClick = {
                                    currentMuscle = muscle
                                    isMuscleGroupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Specific Part Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                focusManager.clearFocus()
                                isPartDropdownExpanded = true 
                            },
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (currentPart.isNotEmpty()) currentPart else "Specific Part",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Icon"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isPartDropdownExpanded,
                        onDismissRequest = { isPartDropdownExpanded = false }
                    ) {
                        musclePartsMap[currentMuscle]?.forEach { part ->
                            DropdownMenuItem(
                                text = { Text(part) },
                                onClick = {
                                    currentPart = part
                                    isPartDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Add Part button centered below the dropdowns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (currentPart.isNotEmpty()) {
                            selectedParts = selectedParts + currentPart
                            currentPart = ""
                        }
                    },
                    modifier = Modifier.width(120.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Add Part", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Display Selected Parts with Delete Buttons
            if (selectedParts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    selectedParts.forEachIndexed { index, part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(part)
                            IconButton(
                                onClick = {
                                    selectedParts = selectedParts.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val exercise = EntityExercise(
                                name = currentExercise,
                                description = currentDescription,
                                muscle = currentMuscle,
                                parts = Converter().fromList(selectedParts),
                                equipment = currentEquipment,
                                difficulty = currentDifficulty,
                                gifUrl = gifPath ?: "",
                                useTime = useTime
                            )
                            dao.insertExercise(exercise)

                            // Show success notification
                            val achievementManager = AchievementManager.getInstance()
                            achievementManager.notificationService.showAchievementNotification("exercise_created")

                            // Check if we should return to workout creation
                            val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
                            if (returnTo == "workout_creation") {
                                val newExercise = Exercise(
                                    name = currentExercise,
                                    muscle = currentMuscle,
                                    part = selectedParts,
                                    gifUrl = gifPath ?: "",
                                    description = currentDescription,
                                    difficulty = currentDifficulty
                                )
                                navController.previousBackStackEntry?.savedStateHandle?.set("newExercise", newExercise)
                                navController.popBackStack()
                            } else {
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            println("Error saving exercise: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = currentExercise.isNotEmpty() && currentMuscle.isNotEmpty() && selectedParts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Exercise")
            }
        }
    }
} 