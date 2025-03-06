package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymtracker.ui.theme.GymTrackerTheme
import androidx.lifecycle.lifecycleScope
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.ExerciseEntity
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import com.example.gymtracker.data.WorkoutEntity
import com.example.gymtracker.data.WorkoutExerciseCrossRef

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            GymTrackerTheme {

                //creates a NavController to manage navigation
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }
                    composable(Screen.WorkoutCreation.route) {
                        WorkoutCreationScreen(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWorkoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Create Workout")
    }
}


data class Exercise(val name: String, val sets: String, val reps: String, val muscle: String, val part: String)

@Composable
fun WorkoutCreationScreen(navController: NavController) {
    var workoutName by remember { mutableStateOf("") }
    var exercisesList by remember { mutableStateOf(listOf<Exercise>()) }
    var currentExercise by remember { mutableStateOf("") }
    var currentSets by remember { mutableStateOf("") }
    var currentReps by remember { mutableStateOf("") }
    var currentMuscle by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope()

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

        OutlinedTextField(
            value = currentSets,
            onValueChange = { currentSets = it },
            label = { Text("Sets") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
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
            value = currentReps,
            onValueChange = { currentReps = it },
            label = { Text("Reps") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
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
            value = currentMuscle,
            onValueChange = { currentMuscle = it },
            label = { Text("Muscle Group") },
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
            value = currentPart,
            onValueChange = { currentPart = it },
            label = { Text("Specific Part") },
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

        Button(
            onClick = {
                if (currentExercise.isNotEmpty() && currentSets.isNotEmpty() && currentReps.isNotEmpty() && currentMuscle.isNotEmpty() && currentPart.isNotEmpty()) {
                    if (editIndex == null) {
                        exercisesList = exercisesList + Exercise(currentExercise, currentSets, currentReps, currentMuscle, currentPart)
                    } else {
                        exercisesList = exercisesList.toMutableList().also {
                            it[editIndex!!] = Exercise(currentExercise, currentSets, currentReps, currentMuscle, currentPart)
                        }
                        editIndex = null
                    }
                    currentExercise = ""
                    currentSets = ""
                    currentReps = ""
                    currentMuscle = ""
                    currentPart = ""
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
                    maxLines = 2, // Limit to 2 lines (optional)
                    overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
                    )

                Row {
                    Button(onClick = {
                        currentExercise = exercise.name
                        currentSets = exercise.sets
                        currentReps = exercise.reps
                        currentMuscle = exercise.muscle
                        currentPart = exercise.part
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

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            CreateWorkoutButton(
                onClick = { navController.navigate(Screen.WorkoutCreation.route) }
            )
        }
    }
}
