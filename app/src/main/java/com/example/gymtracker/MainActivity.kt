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
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
}


class MainActivity : ComponentActivity() {
    // Define the permissions you need
    private val requiredPermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    // Register a launcher for requesting multiple permissions
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                // All permissions granted
                println("All permissions granted")
            } else {
                // Some or all permissions denied
                println("Some or all permissions denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            // Check and request permissions when the activity is created
            val permissionsGranted = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!hasPermissions()) {
                    requestPermissionsLauncher.launch(requiredPermissions)
                } else {
                    permissionsGranted.value = true
                }
            }

            if (permissionsGranted.value) {
                // Your app's UI goes here
                Text("Permissions granted! App is ready.")
            } else {
                Text("Requesting permissions...")
            }

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

    // Check if all required permissions are granted
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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


data class Exercise(val name: String, val sets: String, val reps: String)

@Composable
fun WorkoutCreationScreen(navController: NavController) {
    var workoutName by remember { mutableStateOf("") }
    var exercisesList by remember { mutableStateOf(listOf<Exercise>()) }
    var currentExercise by remember { mutableStateOf("") }
    var currentSets by remember { mutableStateOf("") }
    var currentReps by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf<Int?>(null) } // Track which exercise is being edited

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val scope = rememberCoroutineScope() // This replaces lifecycleScope

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create a New Workout",
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.headlineMedium

        )

        // Workout Name Input
        OutlinedTextField(
            value = workoutName,
            onValueChange = { workoutName = it },
            label = { Text("Workout Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Current Exercise Input
        OutlinedTextField(
            value = currentExercise,
            onValueChange = { currentExercise = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Sets Input
        OutlinedTextField(
            value = currentSets,
            onValueChange = { currentSets = it },
            label = { Text("Sets") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        // Reps Input
        OutlinedTextField(
            value = currentReps,
            onValueChange = { currentReps = it },
            label = { Text("Reps") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        // Add/Update Exercise Button
        Button(
            onClick = {
                if (currentExercise.isNotEmpty() && currentSets.isNotEmpty() && currentReps.isNotEmpty()) {
                    if (editIndex == null) {
                        // Add new exercise
                        exercisesList = exercisesList + Exercise(currentExercise, currentSets, currentReps)
                    } else {
                        // Update existing exercise
                        exercisesList = exercisesList.toMutableList().also {
                            it[editIndex!!] = Exercise(currentExercise, currentSets, currentReps)
                        }
                        editIndex = null // Reset index
                    }
                    currentExercise = ""
                    currentSets = ""
                    currentReps = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editIndex == null) "Add Exercise" else "Update Exercise")
        }

        // List Exercises with Edit and Delete Buttons
        exercisesList.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${exercise.name}: ${exercise.sets} Sets, ${exercise.reps} Reps")

                Row {
                    Button(onClick = {
                        currentExercise = exercise.name
                        currentSets = exercise.sets
                        currentReps = exercise.reps
                        editIndex = index
                    }) {
                        Text("Edit")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        exercisesList = exercisesList.filterIndexed { i, _ -> i != index }
                    }) {
                        Text("Delete")
                    }
                }
            }
        }

        // Save Workout Button
        Button(
            onClick = {
                scope.launch {
                    try {
                        exercisesList.forEach { exercise ->
                            dao.insert(
                                ExerciseEntity(
                                    name = exercise.name,
                                    sets = exercise.sets,
                                    reps = exercise.reps
                                )
                            )
                        }
                        println("Workout Saved!")
                        navController.popBackStack()
                    } catch (e: Exception){
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
