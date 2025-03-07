package com.example.gymtracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.TypeConverter
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.Converter
import com.example.gymtracker.data.ExerciseEntity
import com.example.gymtracker.data.WorkoutEntity
import com.example.gymtracker.data.WorkoutExerciseCrossRef
import kotlinx.coroutines.launch


@Composable
fun WorkoutCreationScreen(navController: NavController) {
    var workoutName by remember { mutableStateOf("") }
    var exercisesList by remember { mutableStateOf(listOf<Exercise>()) }
    var currentExercise by remember { mutableStateOf("") }
    var currentSets by remember { mutableIntStateOf(3) }
    var currentReps by remember { mutableIntStateOf(12) }
    var currentMuscle by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

    val musclePartsMap = mapOf(
        "Chest" to listOf("Upper Chest", "Middle Chest", "Lower Chest"),
        "Shoulders" to listOf("Front Shoulders", "Side Shoulders", "Rear Shoulders"),
        "Back" to listOf("Upper Back", "Lats", "Lower Back"),
        "Arms" to listOf("Biceps", "Triceps", "Forearms"),
        "Legs" to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"),
        "Core" to listOf("Abs", "Obliques", "Lower Back"),
        "Neck" to listOf("Side neck muscle", "Upper Traps")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create a New Workout",
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = workoutName,
            onValueChange = { workoutName = it },
            label = { Text("Workout Name") },
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sets")
                Row {
                    IconButton(onClick = { if (currentSets > 1) currentSets-- }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease Sets")
                    }
                    Text(currentSets.toString(), Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (currentSets < 50) currentSets++ }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase Sets")
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reps")
                Row {
                    IconButton(onClick = { if (currentReps > 1) currentReps-- }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease Reps")
                    }
                    Text(currentReps.toString(), Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (currentSets < 50) currentReps++ }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase Reps")
                    }
                }
            }
        }
        // State to control dropdown visibility
        var isMuscleGroupDropdownExpanded by remember { mutableStateOf(false) }
        var isPartDropdownExpanded by remember { mutableStateOf(false) }

        var selectedParts by remember { mutableStateOf<List<String>>(emptyList()) } // List to store selected parts

        // List of muscle groups
        val muscleGroups = musclePartsMap.keys.toList()

        // Reset currentPart when currentMuscle changes
        LaunchedEffect(currentMuscle) {
            currentPart = "" // Reset the part when the muscle group changes
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            // Muscle Group Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMuscleGroupDropdownExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (currentMuscle.isNotEmpty()) currentMuscle else "Muscle Group")
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Icon"
                        )
                    }
                }

                DropdownMenu(
                    expanded = isMuscleGroupDropdownExpanded,
                    onDismissRequest = { isMuscleGroupDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    muscleGroups.forEach { muscle ->
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
            val parts = musclePartsMap[currentMuscle] ?: emptyList()

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { isPartDropdownExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (currentPart.isNotEmpty()) currentPart else "Specific Part")
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Icon"
                        )
                    }
                }

                DropdownMenu(
                    expanded = isPartDropdownExpanded,
                    onDismissRequest = { isPartDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    parts.forEach { part ->
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
            // Add Additional Muscle Part Button
            Button(
                onClick = {
                    if (currentPart.isNotEmpty()) {
                        selectedParts = selectedParts + currentPart // Add the current part to the list
                        currentPart = "" // Reset the current part for the next selection
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Add Part")
            }

            // Display Selected Parts with Delete Buttons
            if (selectedParts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    selectedParts.forEachIndexed { index, part ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(part)
                            IconButton(
                                onClick = {
                                    selectedParts = selectedParts.toMutableList().apply {
                                        removeAt(index) // Remove the part at the current index
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                if (currentExercise.isNotEmpty() && currentMuscle.isNotEmpty() && selectedParts.isNotEmpty()) {
                    if (editIndex == null) {
                        exercisesList = exercisesList + Exercise(currentExercise, currentSets, currentReps, currentMuscle, selectedParts)
                    } else {
                        exercisesList = exercisesList.toMutableList().also {
                            it[editIndex!!] = Exercise(currentExercise, currentSets, currentReps, currentMuscle, selectedParts)
                        }
                        editIndex = null
                    }
                    currentExercise = ""
                    currentSets = 3
                    currentReps = 12
                    currentMuscle = ""
                    currentPart = ""
                    selectedParts = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editIndex == null) "Add Exercise" else "Update Exercise")
        }

        exercisesList.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${exercise.name}: ${exercise.sets} Sets, ${exercise.reps} Reps, ${exercise.muscle} - ${exercise.part}",
                    modifier = Modifier
                        .weight(1f) // Takes remaining space
                        .padding(end = 8.dp), // Add padding to separate from buttons
                    maxLines = 3, // Limit to 2 lines (optional)
                    overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
                )

                Row {
                    Button(onClick = {
                        currentExercise = exercise.name
                        currentSets = exercise.sets
                        currentReps = exercise.reps
                        currentMuscle = exercise.muscle
                        selectedParts = exercise.part
                        editIndex = index
                    },
                        modifier = Modifier.width(80.dp) // Fixed width for the button
                    ) {
                        Text("Edit")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(onClick = {
                        exercisesList = exercisesList.filterIndexed { i, _ -> i != index }
                    },
                        modifier = Modifier.width(100.dp) // Fixed width for the button
                    ) {
                        Text("Delete")
                    }
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    try {
                        val workoutId = dao.insertWorkout(WorkoutEntity(name = workoutName)).toInt()
                        exercisesList.forEach { exercise ->
                            val exerciseId = dao.insertExercise(
                                ExerciseEntity(
                                    name = exercise.name,
                                    sets = exercise.sets,
                                    reps = exercise.reps,
                                    muscle = exercise.muscle,
                                    part = exercise.part
                                )
                            ).toInt()
                            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workoutId, exerciseId))
                        }
                        println("Workout Saved!")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        println("Error saving workout: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Workout")
        }
    }
}

data class Exercise(val name: String, val sets: Int, val reps: Int, val muscle: String, val part: List<String>)

