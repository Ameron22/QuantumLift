package com.example.gymtracker.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.Exercise
import com.example.gymtracker.utils.GifUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(navController: NavController) {
    var currentExercise by remember { mutableStateOf("") }
    var currentSets by remember { mutableIntStateOf(3) }
    var currentRepsTime by remember { mutableIntStateOf(12) }
    var currentMinutes by remember { mutableIntStateOf(1) }
    var currentSeconds by remember { mutableIntStateOf(0) }
    var useTime by remember { mutableStateOf(false) }
    var currentMuscle by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf("") }
    var weight by remember { mutableIntStateOf(5) }
    var selectedParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMuscleGroupDropdownExpanded by remember { mutableStateOf(false) }
    var isPartDropdownExpanded by remember { mutableStateOf(false) }
    var gifPath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

    // Launcher for picking GIF from gallery
    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            gifPath = GifUtils.saveGifToInternalStorage(context, it)
        }
    }

    val musclePartsMap = mapOf(
        "Chest" to listOf("Upper Chest", "Middle Chest", "Lower Chest"),
        "Shoulders" to listOf("Front Shoulders", "Side Shoulders", "Rear Shoulders"),
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = currentExercise,
                onValueChange = { currentExercise = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

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

            // Switch between Reps and Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reps/Time")
                Switch(
                    checked = useTime,
                    onCheckedChange = { useTime = it }
                )
            }

            // Number pickers section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sets picker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sets", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    NumberPicker(
                        value = currentSets,
                        onValueChange = { currentSets = it },
                        range = 1..10
                    )
                }

                if (useTime) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Minutes picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Minutes", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPicker(
                            value = currentMinutes,
                            onValueChange = { currentMinutes = it },
                            range = 0..59
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    // Seconds picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Seconds", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPicker(
                            value = currentSeconds,
                            onValueChange = { currentSeconds = it },
                            range = 0..59
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Weight picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Weight (kg)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPicker(
                            value = weight,
                            onValueChange = { weight = it },
                            range = 0..500,
                            unit = "kg"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    // Reps picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reps", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPicker(
                            value = currentRepsTime,
                            onValueChange = { currentRepsTime = it },
                            range = 1..50
                        )
                    }
                }
            }

            // Muscle selection section
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
                            .clickable { isMuscleGroupDropdownExpanded = true },
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
                            .clickable { isPartDropdownExpanded = true },
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
                            val reps = if (useTime) {
                                (currentMinutes * 60 + currentSeconds) + 1000
                            } else {
                                currentRepsTime
                            }

                            val exercise = EntityExercise(
                                name = currentExercise,
                                sets = currentSets,
                                weight = weight,
                                reps = reps,
                                muscle = currentMuscle,
                                part = selectedParts,
                                gifUrl = gifPath ?: ""
                            )
                            dao.insertExercise(exercise)

                            // Check if we should return to workout creation
                            val returnTo = navController.currentBackStackEntry?.savedStateHandle?.get<String>("returnTo")
                            if (returnTo == "workout_creation") {
                                val newExercise = Exercise(
                                    name = currentExercise,
                                    sets = currentSets,
                                    weight = weight,
                                    reps = reps,
                                    muscle = currentMuscle,
                                    part = selectedParts,
                                    gifUrl = gifPath ?: ""
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