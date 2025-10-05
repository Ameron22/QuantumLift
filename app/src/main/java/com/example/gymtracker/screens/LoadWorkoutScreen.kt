package com.example.gymtracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.components.LevelUpDialog
import com.example.gymtracker.components.LoadingSpinner
import com.example.gymtracker.components.WorkoutCard
import com.example.gymtracker.components.WorkoutIndicator
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.Converter
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.WarmUpTemplateWithExercises
import com.example.gymtracker.data.WorkoutWithExercises
import com.example.gymtracker.data.XPSystem
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.viewmodels.GeneralViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class for level-up information
data class LevelUpData(
    val xpGained: Int,
    val currentLevel: Int,
    val newLevel: Int,
    val currentXP: Int,
    val xpForNextLevel: Int,
    val previousLevelXP: Int
)


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlowRowCustom(
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
                        overflow = TextOverflow.Ellipsis,
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
fun LoadWorkoutScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel
) {
    val context = LocalContext.current
    val workouts = remember { mutableStateOf(listOf<WorkoutWithExercises>()) }
    val filteredWorkouts = remember { mutableStateOf(listOf<WorkoutWithExercises>()) }
    val searchQuery = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    // State for workout creation dialog
    var showCreateWorkoutDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }

    // State for workout deletion confirmation dialog
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<EntityWorkout?>(null) }

    // State for filter dialog
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }

    // State for level-up dialog
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var levelUpData by remember { mutableStateOf<LevelUpData?>(null) }

    // State for tab selection
    var selectedTabIndex by remember { mutableStateOf(2) }
    val tabs = listOf("Exercises", "Warm-ups", "Workouts")


    // Load workouts with exercises
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).exerciseDao()
            val allWorkouts = dao.getAllWorkouts()
            val workoutsWithExercises = allWorkouts.mapNotNull { workout ->
                dao.getWorkoutWithExercises(workout.id).firstOrNull()
            }

            withContext(Dispatchers.Main) {
                workouts.value = workoutsWithExercises
                filteredWorkouts.value = workouts.value
                isLoading = false
            }
        }
    }

    // Check for level-up using XP buffer
    val xpBuffer by generalViewModel.xpBuffer.collectAsState()

    LaunchedEffect(xpBuffer) {
        if (xpBuffer != null) {
            Log.d("LoadWorkoutScreen", "XP buffer detected: ${xpBuffer}")

            // Use XPSystem utility functions for consistent calculations
            val xpSystem = XPSystem(AppDatabase.getDatabase(context).userXPDao())

            // Calculate XP for next level
            val xpForNextLevel = xpSystem.getXPNeededForLevel(xpBuffer!!.newLevel)

            // Show level-up dialog
            levelUpData = LevelUpData(
                xpGained = xpBuffer!!.xpGained,
                currentLevel = xpBuffer!!.previousLevel,
                newLevel = xpBuffer!!.newLevel,
                currentXP = xpBuffer!!.newTotalXP,
                xpForNextLevel = xpForNextLevel,
                previousLevelXP = xpBuffer!!.previousTotalXP  // Use actual XP before gain, not level start
            )
            showLevelUpDialog = true
            Log.d("LoadWorkoutScreen", "Level-up dialog triggered from XP buffer")
        }
    }

    // Get unique muscle groups from all workouts
    val muscleGroups = workouts.value.flatMap { workoutWithExercises ->
        workoutWithExercises.exercises.map { it.muscle }
    }.distinct().sorted()

    // Filter workouts when search query or muscle group filter changes
    LaunchedEffect(searchQuery.value, selectedMuscleGroup) {
        filteredWorkouts.value = workouts.value.filter { workoutWithExercises ->
            val matchesSearch = searchQuery.value.isEmpty() ||
                    workoutWithExercises.workout.name.contains(searchQuery.value, ignoreCase = true)
            val matchesMuscle = selectedMuscleGroup == null ||
                    workoutWithExercises.exercises.any { it.muscle == selectedMuscleGroup }
            matchesSearch && matchesMuscle
        }
    }

    // Function to create new workout and navigate to details
    fun createNewWorkout() {
        if (newWorkoutName.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(context).exerciseDao()
                val newWorkout = EntityWorkout(name = newWorkoutName)
                val workoutId = dao.insertWorkout(newWorkout).toInt()

                // Navigate to workout details screen with the new workout ID on main thread
                withContext(Dispatchers.Main) {
                    navController.navigate(Screen.Routes.workoutDetails(workoutId))
                    // Clear the dialog and reset name only after successful creation
                    newWorkoutName = ""
                    showCreateWorkoutDialog = false
                }
            }
        }
    }

    // Function to show delete confirmation dialog
    fun showDeleteConfirmation(workout: EntityWorkout) {
        workoutToDelete = workout
        showDeleteConfirmationDialog = true
    }

    // Function to delete workout
    fun deleteWorkout(workout: EntityWorkout) {
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).exerciseDao()

            // Delete all workout exercises first (foreign key constraint)
            dao.deleteWorkoutExercisesForWorkout(workout.id)

            // Delete the workout
            dao.deleteWorkout(workout)

            // Refresh the workouts list
            withContext(Dispatchers.Main) {
                val allWorkouts = dao.getAllWorkouts()
                val workoutsWithExercises = allWorkouts.mapNotNull { w ->
                    dao.getWorkoutWithExercises(w.id).firstOrNull()
                }
                workouts.value = workoutsWithExercises
                filteredWorkouts.value = if (searchQuery.value.isEmpty()) {
                    workouts.value
                } else {
                    workouts.value.filter { workoutWithExercises ->
                        workoutWithExercises.workout.name.contains(
                            searchQuery.value,
                            ignoreCase = true
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Top padding area
                Spacer(modifier = Modifier.height(60.dp))
                // Main tab bar as the primary top bar
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    containerColor = Color.Black,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }

                // WorkoutIndicator in a separate row below tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    WorkoutIndicator(
                        generalViewModel = generalViewModel,
                        navController = navController
                    )
                }
            }
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> navController.navigate(Screen.CreateExercise.route) // Exercises tab
                        1 -> navController.navigate(Screen.CreateWarmUp.route)   // Warm-ups tab
                        2 -> showCreateWorkoutDialog = true                      // Workouts tab
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = when (selectedTabIndex) {
                        0 -> "Create Exercise"
                        1 -> "Create Warm-up"
                        2 -> "Create Workout"
                        else -> "Add"
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        if (isLoading) {
            // Loading indicator in center of page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingSpinner(
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        } else {
            when (selectedTabIndex) {
                0 -> {
                    ExercisesTab(
                        navController = navController,
                        paddingValues = paddingValues
                    )
                }

                1 -> {
                    WarmUpsTab(
                        navController = navController,
                        paddingValues = paddingValues
                    )
                }

                2 -> {
                    WorkoutsTab(
                        filteredWorkouts = filteredWorkouts.value,
                        generalViewModel = generalViewModel,
                        navController = navController,
                        paddingValues = paddingValues,
                        showDeleteConfirmation = { showDeleteConfirmation(it) }
                    )
                }
            }
        }
    }

    // Create Workout Dialog
    if (showCreateWorkoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateWorkoutDialog = false
                newWorkoutName = ""
            },
            title = { Text("Create New Workout") },
            text = {
                OutlinedTextField(
                    value = newWorkoutName,
                    onValueChange = { newWorkoutName = it },
                    label = { Text("Workout Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { createNewWorkout() },
                    enabled = newWorkoutName.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateWorkoutDialog = false
                        newWorkoutName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Workout Confirmation Dialog
    if (showDeleteConfirmationDialog && workoutToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                workoutToDelete = null
            },
            title = { Text("Delete Workout") },
            text = {
                Text("Are you sure you want to delete '${workoutToDelete!!.name}'? This action cannot be undone and will also remove all exercises in this workout.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteWorkout(workoutToDelete!!)
                        showDeleteConfirmationDialog = false
                        workoutToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        workoutToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Workouts") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Muscle Group Filter
                    Text(
                        text = "Muscle Group",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRowCustom(
                        items = muscleGroups,
                        selectedItems = if (selectedMuscleGroup != null) listOf(selectedMuscleGroup!!) else emptyList(),
                        onItemClick = { muscle: String ->
                            selectedMuscleGroup =
                                if (selectedMuscleGroup == muscle) null else muscle
                        },
                        onAllClick = { selectedMuscleGroup = null },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Level Up Dialog
    if (showLevelUpDialog && levelUpData != null) {
        Log.d(
            "LoadWorkoutScreen",
            "Rendering LevelUpDialog - showLevelUpDialog: $showLevelUpDialog, levelUpData: $levelUpData"
        )
        LevelUpDialog(
            onDismiss = {
                Log.d("LoadWorkoutScreen", "LevelUpDialog dismissed")
                showLevelUpDialog = false
                levelUpData = null
                // Clear the XP buffer to prevent showing dialog again
                generalViewModel.clearXPBuffer()
            },
            xpGained = levelUpData!!.xpGained,
            currentLevel = levelUpData!!.currentLevel,
            newLevel = levelUpData!!.newLevel,
            currentXP = levelUpData!!.currentXP,
            xpForNextLevel = levelUpData!!.xpForNextLevel,
            previousLevelXP = levelUpData!!.previousLevelXP
        )
    } else {
        Log.d(
            "LoadWorkoutScreen",
            "LevelUpDialog condition not met - showLevelUpDialog: $showLevelUpDialog, levelUpData: $levelUpData"
        )
    }
}

@Composable
fun WarmUpsTab(
    navController: NavController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val warmUpDao = remember { db.warmUpDao() }
    val exerciseDao = remember { db.exerciseDao() }
    val coroutineScope = rememberCoroutineScope()

    var warmUpTemplates by remember {
        mutableStateOf<List<WarmUpTemplateWithExercises>>(
            emptyList()
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDifficulties by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load warm-up templates
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val templates = warmUpDao.getAllWarmUpTemplatesWithExercises().first()
                withContext(Dispatchers.Main) {
                    warmUpTemplates = templates
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("WarmUpsTab", "Error loading warm-up templates: ${e.message}")
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    // Filter warm-up templates based on search and filters
    val filteredTemplates = warmUpTemplates.filter { templateWithExercises ->
        val template = templateWithExercises.template
        val searchTerms = searchQuery.lowercase().split(" ").filter { it.isNotEmpty() }
        val matchesSearch = if (searchTerms.isEmpty()) {
            true
        } else {
            val templateNameLower = template.name.lowercase()
            val templateDescLower = template.description.lowercase()
            searchTerms.all { term ->
                templateNameLower.contains(term) || templateDescLower.contains(term)
            }
        }

        val matchesCategory =
            selectedCategories.isEmpty() || selectedCategories.contains(template.category)
        val matchesDifficulty =
            selectedDifficulties.isEmpty() || selectedDifficulties.contains(template.difficulty)

        matchesSearch && matchesCategory && matchesDifficulty
    }

    @Composable
    fun WarmUpTemplateCard(
        templateWithExercises: WarmUpTemplateWithExercises,
        exerciseDao: com.example.gymtracker.data.ExerciseDao,
        onClick: () -> Unit
    ) {
        var exerciseNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
        var exerciseDetails by remember { mutableStateOf<Map<Int, EntityExercise>>(emptyMap()) }
        var isExpanded by remember { mutableStateOf(false) }

        // Load exercise names and details
        LaunchedEffect(templateWithExercises.exercises) {
            val names = mutableMapOf<Int, String>()
            val details = mutableMapOf<Int, EntityExercise>()
            templateWithExercises.exercises.forEach { warmUpExercise ->
                try {
                    val exercise = exerciseDao.getExerciseById(warmUpExercise.exerciseId)
                    if (exercise != null) {
                        names[warmUpExercise.exerciseId] = exercise.name
                        details[warmUpExercise.exerciseId] = exercise
                    } else {
                        names[warmUpExercise.exerciseId] = "Unknown Exercise"
                    }
                } catch (e: Exception) {
                    names[warmUpExercise.exerciseId] = "Unknown Exercise"
                }
            }
            exerciseNames = names
            exerciseDetails = details
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Template header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = templateWithExercises.template.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = templateWithExercises.template.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${templateWithExercises.template.estimatedDuration} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = templateWithExercises.template.difficulty,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Exercise list
                if (templateWithExercises.exercises.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exercises (${templateWithExercises.exercises.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isExpanded) {
                        // Show all exercises with full details
                        templateWithExercises.exercises.forEach { warmUpExercise ->
                            val exerciseName =
                                exerciseNames[warmUpExercise.exerciseId] ?: "Loading..."
                            val exercise = exerciseDetails[warmUpExercise.exerciseId]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Display GIF if available
                                    if (exercise?.gifUrl?.isNotEmpty() == true) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        ) {
                                            ExerciseGif(
                                                gifPath = exercise.gifUrl,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = exerciseName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = exercise?.muscle ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                            Text(
                                                text = exercise?.difficulty ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }

                                        // Show muscle parts if available
                                        val exerciseParts = try {
                                            exercise?.parts?.let { Converter().fromString(it) }
                                                ?: emptyList()
                                        } catch (e: Exception) {
                                            emptyList<String>()
                                        }

                                        if (exerciseParts.isNotEmpty()) {
                                            Text(
                                                text = "Parts: ${exerciseParts.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    }

                                    // Exercise parameters
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (warmUpExercise.isTimeBased) {
                                                "${warmUpExercise.duration}s"
                                            } else {
                                                "${warmUpExercise.reps} reps"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (warmUpExercise.sets > 1) {
                                            Text(
                                                text = "${warmUpExercise.sets} sets",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show preview (first 3 exercises)
                        templateWithExercises.exercises.take(3).forEach { warmUpExercise ->
                            val exerciseName =
                                exerciseNames[warmUpExercise.exerciseId] ?: "Loading..."
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exerciseName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (warmUpExercise.isTimeBased) {
                                        "${warmUpExercise.duration}s"
                                    } else {
                                        "${warmUpExercise.reps} reps"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (templateWithExercises.exercises.size > 3) {
                            Text(
                                text = "... and ${templateWithExercises.exercises.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Search and Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search warm-ups...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = { showFilterDialog = true },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Filter")
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(modifier = Modifier.size(80.dp))
            }
        } else {
            // Warm-up Templates List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTemplates) { templateWithExercises ->
                    WarmUpTemplateCard(
                        templateWithExercises = templateWithExercises,
                        exerciseDao = exerciseDao,
                        onClick = {
                            // Navigate to warm-up details or edit
                            navController.navigate(Screen.CreateWarmUp.route)
                        }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Warm-ups") },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Filter
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRowCustom(
                        items = warmUpTemplates.map { it.template.category }.distinct()
                            .sorted(),
                        selectedItems = selectedCategories,
                        onItemClick = { category: String ->
                            if (selectedCategories.contains(category)) {
                                selectedCategories =
                                    selectedCategories.filter { it != category }
                            } else {
                                selectedCategories = selectedCategories + category
                            }
                        },
                        onAllClick = { selectedCategories = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Difficulty Filter
                    Text(
                        text = "Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRowCustom(
                        items = warmUpTemplates.map { it.template.difficulty }.distinct()
                            .sorted(),
                        selectedItems = selectedDifficulties,
                        onItemClick = { difficulty: String ->
                            if (selectedDifficulties.contains(difficulty)) {
                                selectedDifficulties =
                                    selectedDifficulties.filter { it != difficulty }
                            } else {
                                selectedDifficulties = selectedDifficulties + difficulty
                            }
                        },
                        onAllClick = { selectedDifficulties = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun WorkoutsTab(
    filteredWorkouts: List<WorkoutWithExercises>,
    generalViewModel: GeneralViewModel,
    navController: NavController,
    paddingValues: PaddingValues,
    showDeleteConfirmation: (EntityWorkout) -> Unit
) {
    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }

    // Filter workouts based on search query and muscle group
    val filteredWorkoutsList = remember(filteredWorkouts, searchQuery, selectedMuscleGroup) {
        filteredWorkouts.filter { workout ->
            val matchesSearch = searchQuery.isEmpty() ||
                    workout.workout.name.contains(searchQuery, ignoreCase = true) ||
                    workout.exercises.any { exercise ->
                        exercise.name.contains(searchQuery, ignoreCase = true)
                    }

            val matchesMuscleGroup = selectedMuscleGroup == null ||
                    workout.exercises.any { exercise ->
                        exercise.muscle == selectedMuscleGroup
                    }

            matchesSearch && matchesMuscleGroup
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search workouts...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Filter")
                }
            }
        }

        // Active filters
        if (selectedMuscleGroup != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { selectedMuscleGroup = null },
                        label = { Text(selectedMuscleGroup!!) }
                    )
                }
            }
        }


        // Existing workout cards
        items(filteredWorkoutsList) { workoutWithExercises ->
            val currentWorkout by generalViewModel.currentWorkout.collectAsState()
            val isActive =
                currentWorkout?.workoutId == workoutWithExercises.workout.id && currentWorkout?.isActive == true

            WorkoutCard(
                workout = workoutWithExercises.workout,
                muscleGroups = workoutWithExercises.exercises.map { it.muscle }.distinct(),
                onClick = {
                    navController.navigate(
                        Screen.Routes.workoutDetails(
                            workoutWithExercises.workout.id
                        )
                    )
                },
                onDelete = { showDeleteConfirmation(workoutWithExercises.workout) },
                isActive = isActive
            )
        }

        // Bottom padding to allow scrolling last workout up
        item {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Workouts") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Muscle Group Filter
                    Text(
                        text = "Muscle Group",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilterChipFlowRowCustom(
                        items = listOf(
                            "Chest",
                            "Back",
                            "Shoulders",
                            "Arms",
                            "Legs",
                            "Core",
                            "Cardio"
                        ),
                        selectedItems = if (selectedMuscleGroup != null) listOf(selectedMuscleGroup!!) else emptyList(),
                        onItemClick = { muscle: String ->
                            selectedMuscleGroup =
                                if (selectedMuscleGroup == muscle) null else muscle
                        },
                        onAllClick = { selectedMuscleGroup = null },
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFilterDialog = false }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedMuscleGroup = null
                        showFilterDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ExercisesTab(
    navController: NavController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val coroutineScope = rememberCoroutineScope()

    var exercises by remember { mutableStateOf<List<EntityExercise>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedMuscleGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDifficulties by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMuscleParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedEquipment by remember { mutableStateOf<List<String>>(emptyList()) }

    // State for exercise deletion confirmation dialog
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<EntityExercise?>(null) }

    // Use the same muscle parts map as AddExerciseToWorkoutScreen
    val musclePartsMap = mapOf(
        "All" to listOf(" "), // "All" for showing all exercises
        "Neck" to listOf("Neck", "Upper Traps"),
        "Chest" to listOf("Chest"),
        "Shoulders" to listOf("Deltoids"),
        "Arms" to listOf("Biceps", "Triceps", "Forearms"),
        "Core" to listOf("Abs", "Obliques", "Lower Back"),
        "Back" to listOf("Upper Back", "Lats", "Lower Back"),
        "Legs" to listOf("Quadriceps", "Hamstrings", "Adductors", "Glutes", "Calves")
    )

    // Get muscle groups from the musclePartsMap
    val muscleGroups = musclePartsMap.keys.toList()

    // Load exercises
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val allExercises = dao.getAllExercises()
            withContext(Dispatchers.Main) {
                exercises = allExercises
            }
        }
    }

    // Function to show delete confirmation dialog
    fun showDeleteExerciseConfirmation(exercise: EntityExercise) {
        exerciseToDelete = exercise
        showDeleteExerciseDialog = true
    }

    // Function to delete exercise
    fun deleteExercise(exercise: EntityExercise) {
        coroutineScope.launch(Dispatchers.IO) {
            dao.deleteExercise(exercise)

            // Refresh the exercises list
            withContext(Dispatchers.Main) {
                val allExercises = dao.getAllExercises()
                exercises = allExercises
            }
        }
    }

    // Filter exercises based on all current selections for dynamic filtering
    val filteredExercisesForOptions = exercises.filter { exercise ->
        // Muscle group filtering
        val matchesMuscleGroup = if (selectedMuscleGroups.isEmpty()) {
            true
        } else {
            val exerciseMuscleGroup = when (exercise.muscle) {
                "Neck" -> "Neck"
                "Chest" -> "Chest"
                "Shoulder" -> "Shoulders"
                "Arms" -> "Arms"
                "Core" -> "Core"
                "Back" -> "Back"
                "Legs" -> "Legs"
                else -> exercise.muscle
            }

            if (selectedMuscleGroups.contains("All")) {
                // When "All" is selected, show only exercises with muscle="All"
                exercise.muscle == "All"
            } else {
                selectedMuscleGroups.contains(exerciseMuscleGroup)
            }
        }

        // Difficulty filtering
        val matchesDifficulty =
            selectedDifficulties.isEmpty() || selectedDifficulties.contains(exercise.difficulty)

        // Equipment filtering
        val matchesEquipment = selectedEquipment.isEmpty() || run {
            val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                exercise.equipment.split(",").map { it.trim() }
            } else {
                listOf("None")
            }
            selectedEquipment.any { selected -> exerciseEquipment.contains(selected) }
        }

        matchesMuscleGroup && matchesDifficulty && matchesEquipment
    }

    // Dynamic equipment list based on filtered exercises
    val equipmentList = filteredExercisesForOptions.flatMap { exercise ->
        if (exercise.equipment.isNotBlank()) {
            exercise.equipment.split(",").map { it.trim() }
        } else {
            listOf("None")
        }
    }.distinct().let { equipment ->
        // Put "None" and "Other" at the top of the list
        val noneAndOther = equipment.filter { it in listOf("None", "Other") }
        val others = equipment.filter { it !in listOf("None", "Other") }.sorted()
        noneAndOther + others
    }

    // Dynamic difficulties list based on filtered exercises
    val difficulties = filteredExercisesForOptions.map { it.difficulty }.distinct().sorted()

    // Filter muscle parts based on selected muscle groups
    val availableMuscleParts = if (selectedMuscleGroups.isEmpty()) {
        // Show all muscle parts when no muscle group is selected
        musclePartsMap.values.flatten().filter { it.isNotBlank() && it != " " }.distinct().sorted()
    } else if (selectedMuscleGroups.contains("All")) {
        // Show no muscle parts when "All" is selected
        emptyList()
    } else {
        // Show only muscle parts from selected muscle groups
        selectedMuscleGroups.flatMap { muscleGroup ->
            musclePartsMap[muscleGroup] ?: emptyList()
        }.filter { it.isNotBlank() && it != " " }.distinct().sorted()
    }

    // Clear muscle parts when muscle groups change
    LaunchedEffect(selectedMuscleGroups) {
        if (selectedMuscleGroups.isNotEmpty() && !selectedMuscleGroups.contains("All")) {
            val validMuscleParts = selectedMuscleGroups.flatMap { muscleGroup ->
                musclePartsMap[muscleGroup] ?: emptyList()
            }.filter { it.isNotBlank() && it != " " }
            selectedMuscleParts = selectedMuscleParts.filter { it in validMuscleParts }
        } else if (selectedMuscleGroups.contains("All")) {
            // Clear muscle parts when "All" is selected
            selectedMuscleParts = emptyList()
        }
    }

    // Filter exercises based on search query and filters
    val filteredExercises = exercises.filter { exercise ->
        val searchTerms = searchQuery.lowercase().split(" ").filter { it.isNotEmpty() }
        val matchesSearch = if (searchTerms.isEmpty()) {
            true
        } else {
            val exerciseNameLower = exercise.name.lowercase()
            searchTerms.all { term -> exerciseNameLower.contains(term) }
        }

        // TODO: REMOVE THIS MAPPING ONCE CSV FILE IS UPDATED WITH NEW MUSCLE GROUP NAMES
        // This mapping is needed for backward compatibility with old exercise data
        // that uses "Shoulder" instead of "Shoulders"
        val exerciseMuscleGroup = when (exercise.muscle) {
            "Neck" -> "Neck"
            "Chest" -> "Chest"
            "Shoulder" -> "Shoulders"
            "Arms" -> "Arms"
            "Core" -> "Core"
            "Back" -> "Back"
            "Legs" -> "Legs"
            else -> exercise.muscle // Fallback for any other muscle groups
        }

        // Handle "All" filter: when selected shows only exercises with muscle="All"
        val matchesMuscle = if (selectedMuscleGroups.isEmpty()) {
            true // Show all exercises when no filter is selected
        } else if (selectedMuscleGroups.contains("All")) {
            // When "All" is selected, show only exercises with muscle="All"
            exercise.muscle == "All"
        } else {
            selectedMuscleGroups.contains(exerciseMuscleGroup)
        }

        val matchesDifficulty =
            selectedDifficulties.isEmpty() || selectedDifficulties.contains(exercise.difficulty)
        val exerciseParts = try {
            Converter().fromString(exercise.parts)
        } catch (e: Exception) {
            emptyList<String>()
        }
        val matchesMusclePart = selectedMuscleParts.isEmpty() || exerciseParts.any { exercisePart ->
            selectedMuscleParts.any { selectedPart ->
                val matches = when (selectedPart.lowercase()) {
                    "adductors" -> exercisePart.equals(
                        "adductors",
                        ignoreCase = true
                    ) || exercisePart.equals("adductor", ignoreCase = true)

                    "adductor" -> exercisePart.equals(
                        "adductors",
                        ignoreCase = true
                    ) || exercisePart.equals("adductor", ignoreCase = true)

                    else -> exercisePart.equals(selectedPart, ignoreCase = true)
                }
                matches
            }
        }
        val matchesEquipment = selectedEquipment.isEmpty() || run {
            val exerciseEquipment = if (exercise.equipment.isNotBlank()) {
                exercise.equipment.split(",").map { it.trim() }
            } else {
                listOf("None")
            }
            selectedEquipment.any { selected -> exerciseEquipment.contains(selected) }
        }

        matchesSearch && matchesMuscle && matchesDifficulty && matchesMusclePart && matchesEquipment
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Search and Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = { showFilterDialog = true },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Filter")
            }
        }

        // Exercise List with Scroll Bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val listState = rememberLazyListState()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises) { exercise ->
                    // Check if exercise is imported (Ip 0-750)
                    val isImported = exercise.id in 0..750

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Navigate to exercise details or add to workout
                                navController.navigate(Screen.CreateExercise.route)
                            }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Display GIF if available
                            if (exercise.gifUrl.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    ExerciseGif(
                                        gifPath = exercise.gifUrl,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercise.muscle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = exercise.difficulty,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                // Show muscle parts if available
                                val exerciseParts = try {
                                    Converter().fromString(exercise.parts)
                                } catch (e: Exception) {
                                    emptyList<String>()
                                }

                                if (exerciseParts.isNotEmpty()) {
                                    Text(
                                        text = "Parts: ${exerciseParts.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                if (exercise.equipment.isNotBlank()) {
                                    Text(
                                        text = "Equipment: ${exercise.equipment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Action buttons (only for non-imported exercises)
                            if (!isImported) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            // TODO: Implement edit functionality later
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Exercise",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { showDeleteExerciseConfirmation(exercise) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Exercise",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter Dialog
            if (showFilterDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    // Background overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showFilterDialog = false }
                    )

                    // Dialog content
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.8f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filter Exercises",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { showFilterDialog = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Scrollable content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Muscle Group Filter
                                Text(
                                    text = "Muscle Group",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilterChipFlowRowCustom(
                                    items = muscleGroups,
                                    selectedItems = selectedMuscleGroups,
                                    onItemClick = { muscle: String ->
                                        if (selectedMuscleGroups.contains(muscle)) {
                                            selectedMuscleGroups =
                                                selectedMuscleGroups.filter { it != muscle }
                                        } else {
                                            selectedMuscleGroups = selectedMuscleGroups + muscle
                                        }
                                    },
                                    onAllClick = { selectedMuscleGroups = emptyList() },
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 8
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                // Muscle Part Filter
                                Text(
                                    text = "Muscle Part",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilterChipFlowRowCustom(
                                    items = availableMuscleParts,
                                    selectedItems = selectedMuscleParts,
                                    onItemClick = { musclePart: String ->
                                        if (selectedMuscleParts.contains(musclePart)) {
                                            selectedMuscleParts =
                                                selectedMuscleParts.filter { it != musclePart }
                                        } else {
                                            selectedMuscleParts = selectedMuscleParts + musclePart
                                        }
                                    },
                                    onAllClick = { selectedMuscleParts = emptyList() },
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 8
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                // Difficulty Level Filter
                                Text(
                                    text = "Difficulty Level",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                FilterChipFlowRowCustom(
                                    items = difficulties,
                                    selectedItems = selectedDifficulties,
                                    onItemClick = { difficulty: String ->
                                        if (selectedDifficulties.contains(difficulty)) {
                                            selectedDifficulties =
                                                selectedDifficulties.filter { it != difficulty }
                                        } else {
                                            selectedDifficulties = selectedDifficulties + difficulty
                                        }
                                    },
                                    onAllClick = { selectedDifficulties = emptyList() },
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 8
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                // Equipment Filter
                                Text(
                                    text = "Equipment",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                FilterChipFlowRowCustom(
                                    items = equipmentList,
                                    selectedItems = selectedEquipment,
                                    onItemClick = { equipment: String ->
                                        if (selectedEquipment.contains(equipment)) {
                                            selectedEquipment =
                                                selectedEquipment.filter { it != equipment }
                                        } else {
                                            selectedEquipment = selectedEquipment + equipment
                                        }
                                    },
                                    onAllClick = { selectedEquipment = emptyList() },
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 8
                                )
                            }

                            // Bottom padding for better scrolling
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Delete Exercise Confirmation Dialog
            if (showDeleteExerciseDialog && exerciseToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteExerciseDialog = false
                        exerciseToDelete = null
                    },
                    title = { Text("Delete Exercise") },
                    text = {
                        Text("Are you sure you want to delete '${exerciseToDelete!!.name}'? This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteExercise(exerciseToDelete!!)
                                showDeleteExerciseDialog = false
                                exerciseToDelete = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteExerciseDialog = false
                                exerciseToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}