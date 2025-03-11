package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.classes.NumberPicker
import com.example.gymtracker.classes.ValuePicker
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.EntityExercise


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(exerciseId: Int, navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var exercise by remember { mutableStateOf<EntityExercise?>(null) }
    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }


    // Map to store weights for each set (set number to weight)
    val setWeights = remember { mutableStateMapOf<Int, Int>() }
    // Map to store repetitions for each set (set number to reps)
    val setReps = remember { mutableStateMapOf<Int, Int>() }
    // Track which set is currently being edited (1-indexed)
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }


    // Fetch exercise data
    LaunchedEffect(exerciseId) {
        try {
            exercise = dao.getExerciseById(exerciseId)?.also { ex ->
                // Initialize weights for all sets with the exercise weight
                if (ex.weight != 0) {
                    for (set in 1..ex.sets) {
                        setWeights[set] = ex.weight
                    }
                }
                if (ex.reps < 50) {
                    for (set in 1..ex.sets) {
                        setReps[set] = ex.reps
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExerciseScreen", "Database error: ${e.message}")
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display exercise details
            exercise?.let { ex ->
                // Display sets with weight and reps
                for (set in 1..ex.sets) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // Distribute space evenly
                            verticalAlignment = Alignment.CenterVertically // Align items vertically in the center
                        ) {
                            Text(
                                text = "Set: $set",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f) // Occupy equal space
                            )
                            if (ex.weight != 0) {
                                if (showWeightPicker && editingSetIndex == set) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showWeightPicker = false
                                            editingSetIndex = null
                                        },
                                        title = {
                                            Text(
                                                "Select Weight",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        text = {
                                            NumberPicker(
                                                value = setWeights[set] ?: ex.weight,
                                                range = 0..200,
                                                onValueChange = { weight ->
                                                    setWeights[set] = weight
                                                },
                                                unit = "Kg"
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showWeightPicker = false
                                                editingSetIndex = null
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                }

                                Text(
                                    text = "${setWeights[set] ?: ex.weight} Kg",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showWeightPicker = true
                                            editingSetIndex = set
                                        }
                                        .padding(horizontal = 8.dp),
                                    textAlign = TextAlign.Center
                                )

                            }


                            if (ex.reps < 50) {
                                if (showRepsPicker && editingSetIndex == set) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            showRepsPicker = false
                                            editingSetIndex = null
                                        },
                                        title = {
                                            Text(
                                                "Select Repetitions",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        text = {
                                            NumberPicker(
                                                value = setReps[set] ?: ex.reps,
                                                range = 0..50,
                                                onValueChange = { reps ->
                                                    setReps[set] = reps
                                                },
                                                unit = "reps"
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showRepsPicker = false
                                                editingSetIndex = null
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                            Text(
                                text = "${setReps[set] ?: ex.reps} Reps",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showRepsPicker = true
                                        editingSetIndex = set
                                    }
                                    .padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } ?: run {
                // Show loading or error message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (exercise == null) "Loading..." else "Exercise not found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}