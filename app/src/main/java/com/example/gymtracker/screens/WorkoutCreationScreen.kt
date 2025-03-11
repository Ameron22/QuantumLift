package com.example.gymtracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.CrossRefWorkoutExercise
import kotlinx.coroutines.launch


@Composable
fun WorkoutCreationScreen(navController: NavController) {
    var workoutName by remember { mutableStateOf("") }
    var exercisesList by remember { mutableStateOf(listOf<Exercise>()) }
    var currentExercise by remember { mutableStateOf("") }
    var currentSets by remember { mutableIntStateOf(3) }
    var currentRepsTime by remember { mutableIntStateOf(12) }
    var currentMinutes by remember { mutableIntStateOf(1) } // Minutes (default: 1)
    var currentSeconds by remember { mutableIntStateOf(0) } // Seconds (default: 0)
    var useTime by remember { mutableStateOf(false) } // Switch between reps and time
    var currentMuscle by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf("") }
    var weight by remember { mutableIntStateOf(5) }
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
        // Switch between Reps and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Reps/Time")
            Switch(
                checked = useTime,
                onCheckedChange = { useTime = it }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { if (currentSets < 50) currentSets++ }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Sets")
                }
                Text("$currentSets Sets", Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { if (currentSets > 1) currentSets-- }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Sets")
                }
            }

            if (useTime) {
                // Time Input
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Minutes
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { if (currentMinutes < 59) currentMinutes++ }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Minutes")
                            }
                            Text("$currentMinutes min", Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (currentMinutes > 0) currentMinutes-- }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Minutes")
                            }
                        }

                        // Seconds
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { if (currentSeconds < 59) currentSeconds++ }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Seconds")
                            }
                            Text("$currentSeconds sec", Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (currentSeconds > 0) currentSeconds-- }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Seconds")
                            }
                        }
                    }
                }
            } else {
                //Weight Input
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { if (weight < 500) weight++ }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Reps")
                    }
                    Text("$weight Kg", Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (weight > 1) weight-- }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Reps")
                    }
                }
                // Reps Input
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { if (currentRepsTime < 50) currentRepsTime++ }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase Reps")
                    }
                    Text("$currentRepsTime Reps", Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (currentRepsTime > 1) currentRepsTime-- }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease Reps")
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

                // Save exercise with reps or time
                val reps = if (useTime) {
                    // Convert time to seconds and add a threshold (e.g., 1000 to ensure it's > 50)
                    (currentMinutes * 60 + currentSeconds) + 1000
                } else {
                    // Use reps as is
                    currentRepsTime
                }

                if (currentExercise.isNotEmpty() && currentMuscle.isNotEmpty() && selectedParts.isNotEmpty()) {
                    if (editIndex == null) {
                        exercisesList = exercisesList + Exercise(currentExercise, currentSets, weight, reps, currentMuscle, selectedParts)
                    } else {
                        exercisesList = exercisesList.toMutableList().also {
                            it[editIndex!!] = Exercise(currentExercise, currentSets, weight, reps, currentMuscle, selectedParts)
                        }
                        editIndex = null
                    }
                    currentExercise = ""
                    currentSets = 3
                    currentRepsTime = 12
                    currentMuscle = ""
                    currentPart = ""
                    selectedParts = emptyList()
                    currentMinutes = 1
                    currentSeconds = 0
                    weight = 5
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
                // Print exercise with reps or time
                val textReps = if (exercise.reps > 50) {
                    // Convert time to seconds and subtract the threshold (e.g., 1000)
                    val timeInSeconds = exercise.reps - 1000
                    val minutes = timeInSeconds / 60
                    val seconds = timeInSeconds % 60
                    // Format as mm:ss
                    var time = String.format("%02d:%02d", minutes, seconds)
                    "${exercise.name}: ${exercise.sets} Sets, ${time} Time, ${exercise.muscle} - ${exercise.part}"
                } else {
                    // Use reps as is
                    "${exercise.name}: ${exercise.sets} Sets, ${exercise.reps} Reps, ${exercise.weight} Kg, ${exercise.muscle} - ${exercise.part}"
                }
                Text(
                    text = textReps,
                    modifier = Modifier
                        .weight(1f) // Takes remaining space
                        .padding(end = 8.dp), // Add padding to separate from buttons
                    maxLines = 3, // Limit to 2 lines (optional)
                    overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
                )

                Row {
                    IconButton(onClick = {
                        currentExercise = exercise.name
                        currentSets = exercise.sets
                        currentRepsTime = exercise.reps
                        currentMuscle = exercise.muscle
                        selectedParts = exercise.part
                        editIndex = index
                    },
                        modifier = Modifier.width(80.dp) // Fixed width for the button
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(onClick = {
                        exercisesList = exercisesList.filterIndexed { i, _ -> i != index }
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    try {
                        val workoutId = dao.insertWorkout(EntityWorkout(name = workoutName)).toInt()
                        exercisesList.forEach { exercise ->
                            val exerciseId = dao.insertExercise(
                                EntityExercise(
                                    name = exercise.name,
                                    sets = exercise.sets,
                                    weight = exercise.weight,
                                    reps = exercise.reps,
                                    muscle = exercise.muscle,
                                    part = exercise.part
                                )
                            ).toInt()
                            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workoutId, exerciseId))
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

data class Exercise(val name: String, val sets: Int, val weight: Int, val reps: Int, val muscle: String, val part: List<String>)

