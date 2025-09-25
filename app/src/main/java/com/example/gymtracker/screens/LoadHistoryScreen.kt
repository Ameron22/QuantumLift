package com.example.gymtracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.classes.MuscleSorenessData
import com.example.gymtracker.classes.SessionWorkoutWithMuscles
import com.example.gymtracker.data.SessionEntityExercise
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutHistoryCard
import com.example.gymtracker.components.WorkoutIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.gymtracker.classes.WeightProgressionData
import com.example.gymtracker.classes.VolumeProgressionData
import com.example.gymtracker.components.LoadingSpinner

private val SectionSpacing = 16.dp
private val ItemPadding = 8.dp

// Function to format duration in MM:SS or HH:MM:SS format
private fun formatDuration(durationInMillis: Long): String {
    val durationInSeconds = durationInMillis / 1000
    val hours = durationInSeconds / 3600
    val minutes = (durationInSeconds % 3600) / 60
    val seconds = durationInSeconds % 60
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadHistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel,
    generalViewModel: GeneralViewModel
) {
    val workoutSessions by viewModel.workoutSessions.collectAsState()
    val muscleSoreness by viewModel.muscleSoreness.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // State for tab selection
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Workouts", "Graphs", "Weight", "Volume")
    
    // Debug logging
    LaunchedEffect(workoutSessions) {
        android.util.Log.d("LoadHistoryScreen", "Workout sessions loaded: ${workoutSessions.size}")
        workoutSessions.forEach { session ->
            android.util.Log.d("LoadHistoryScreen", "Session: ID=${session.sessionId}, Name='${session.workoutName}', Start=${session.startTime}, End=${session.endTime}, Duration=${(session.endTime - session.startTime) / (60 * 1000)} min")
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("History") },
                    actions = {
                        WorkoutIndicator(generalViewModel = generalViewModel, navController = navController)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                )
                
                // Tab bar under TopAppBar
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = { BottomNavBar(navController) }
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
                val exerciseSessions by viewModel.exerciseSessions.collectAsState()
                WorkoutsTab(
                    workoutSessions = workoutSessions,
                    muscleSoreness = muscleSoreness,
                    paddingValues = paddingValues,
                    exerciseSessions = exerciseSessions,
                    viewModel = viewModel
                )
            }
            1 -> {
                val exerciseSessions by viewModel.exerciseSessions.collectAsState()
                GraphsTab(
                    workoutSessions = workoutSessions,
                    exerciseSessions = exerciseSessions,
                    paddingValues = paddingValues,
                    viewModel = viewModel
                )
            }
            2 -> {
                val weightProgression by viewModel.weightProgression.collectAsState()
                WeightProgressionTab(
                    weightProgression = weightProgression,
                    paddingValues = paddingValues
                )
            }
            3 -> {
                val volumeProgression by viewModel.volumeProgression.collectAsState()
                VolumeProgressionTab(
                    volumeProgression = volumeProgression,
                    paddingValues = paddingValues
                )
            }
        }
    }
    }
}

@Composable
fun WorkoutsTab(
    workoutSessions: List<SessionWorkoutWithMuscles>,
    muscleSoreness: Map<String, MuscleSorenessData>,
    paddingValues: PaddingValues,
    exerciseSessions: List<SessionEntityExercise>,
    viewModel: HistoryViewModel
) {
    var selectedSession by remember { mutableStateOf<SessionWorkoutWithMuscles?>(null) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display workout history
        items(workoutSessions) { session ->
            WorkoutHistoryCard(
                session = session,
                onClick = { selectedSession = session }
            )
        }
    }
    
    // Show workout detail dialog
    selectedSession?.let { session ->
        WorkoutDetailDialog(
            session = session,
            exerciseSessions = exerciseSessions,
            onDismiss = { selectedSession = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun GraphsTab(
    workoutSessions: List<SessionWorkoutWithMuscles>,
    exerciseSessions: List<SessionEntityExercise>,
    paddingValues: PaddingValues,
    viewModel: HistoryViewModel
) {
    // Use the complete muscle frequency functions that include all muscles
    val muscleGroupFrequency = viewModel.getCompleteMuscleGroupFrequency(workoutSessions)
    val musclePartsFrequency = viewModel.getCompleteMusclePartsFrequency(exerciseSessions)
    
    // Calculate workout durations for the duration chart
    val workoutDurations = workoutSessions
        .map { session ->
            val duration = session.endTime - session.startTime
            val date = Date(session.startTime)
            Triple(session.workoutName ?: "Unnamed", date, duration)
        }
        .sortedBy { it.second }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Muscle Training Analysis",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)
        )
        
        if (workoutSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workout data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            CompactBarChart(
                muscleGroupFrequency = muscleGroupFrequency,
                musclePartsFrequency = musclePartsFrequency
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add undertrained muscles section
            UndertrainedMusclesSection(
                muscleGroupFrequency = muscleGroupFrequency,
                musclePartsFrequency = musclePartsFrequency
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Duration Over Time chart under undertrained muscles
            if (workoutDurations.isNotEmpty()) {
                WorkoutDurationChart(
                    workoutDurations = workoutDurations
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Weekly Activity Heatmap
            WeeklyActivityHeatmapTab(
                workoutSessions = workoutSessions,
                paddingValues = PaddingValues(0.dp) // No padding since we're inside the scrollable column
            )
        }
    }
}

@Composable
fun CompactBarChart(
    muscleGroupFrequency: List<Pair<String, Int>>,
    musclePartsFrequency: List<Pair<String, Int>>
) {
    var showMuscleParts by remember { mutableStateOf(false) }
    
    // Choose which data to display based on switch state
    val currentData = if (showMuscleParts) musclePartsFrequency else muscleGroupFrequency
    val maxFrequency = currentData.maxOfOrNull { it.second } ?: 1
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Training Frequency",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Groups",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!showMuscleParts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Switch(
                        checked = showMuscleParts,
                        onCheckedChange = { showMuscleParts = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    Text(
                        text = "Parts",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showMuscleParts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentData.forEach { (muscleGroup, frequency) ->
                    CompactBarItem(
                        muscleGroup = muscleGroup,
                        frequency = frequency,
                        maxFrequency = maxFrequency
                    )
                }
            }
        }
    }
}

@Composable
fun CompactBarItem(
    muscleGroup: String,
    frequency: Int,
    maxFrequency: Int
) {
    val barHeight = 24.dp
    val maxBarWidth = 150.dp
    val barWidth = if (frequency > 0) {
        (frequency.toFloat() / maxFrequency.toFloat() * maxBarWidth.value).dp
    } else {
        0.dp
    }
    
    val isUndertrained = frequency <= 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = muscleGroup.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = if (isUndertrained) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isUndertrained) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.width(60.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(barHeight)
                .padding(horizontal = 8.dp)
                .background(
                    color = if (isUndertrained) 
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(2.dp)
                )
        ) {
            if (frequency > 0) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .background(
                            color = if (isUndertrained) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            }
        }
        
        Text(
            text = "$frequency",
            style = MaterialTheme.typography.bodySmall,
            color = if (isUndertrained) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun MuscleGroupOverview(muscleSoreness: Map<String, String>) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        muscleSoreness.forEach { (muscle, soreness) ->
            MuscleChip(muscle = muscle, soreness = soreness)
        }
    }
}

@Composable
fun MuscleChip(muscle: String, soreness: String) {
    val backgroundColor = when (soreness) {
        "Fresh" -> MaterialTheme.colorScheme.primary
        "Slightly Sore" -> MaterialTheme.colorScheme.secondary
        "Very Sore" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = MaterialTheme.colorScheme.onSurface // Adjust based on background

    Box(
        modifier = Modifier
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = muscle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            Text(
                text = soreness,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun WorkoutDurationTrendsTab(
    workoutSessions: List<SessionWorkoutWithMuscles>,
    paddingValues: PaddingValues
) {
    // Calculate workout durations and sort by date
    val workoutDurations = workoutSessions
        .map { session ->
            val duration = session.endTime - session.startTime
            val date = Date(session.startTime)
            Triple(session.workoutName ?: "Unnamed", date, duration)
        }
        .sortedBy { it.second }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Workout Duration Trends",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (workoutDurations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workout data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            WorkoutDurationChart(
                workoutDurations = workoutDurations
            )
        }
    }
}

@Composable
fun WorkoutDurationChart(
    workoutDurations: List<Triple<String, Date, Long>>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxDuration = workoutDurations.maxOfOrNull { it.third } ?: 1L
    val minDuration = workoutDurations.minOfOrNull { it.third } ?: 0L
    val durationRange = maxDuration - minDuration
    
    // Show only last 3 workouts when not expanded, all when expanded
    val displayedWorkouts = if (isExpanded) {
        workoutDurations
    } else {
        workoutDurations.takeLast(3)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Duration Over Time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Chart container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(maxDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(minDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Bar chart row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 50.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    workoutDurations.forEach { (_, _, duration) ->
                        val normalizedDuration = (duration - minDuration).toFloat() / durationRange.toFloat()
                        val barHeight = (normalizedDuration * 180).dp
                        
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(barHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                
                // Month labels row at the BOTTOM - below the bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 50.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    var currentMonth = ""
                    workoutDurations.forEach { (_, date, duration) ->
                        val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
                        
                        if (monthName != currentMonth) {
                            currentMonth = monthName
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Empty space to maintain alignment
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show "See Less" button at top when expanded
            if (isExpanded && workoutDurations.size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { isExpanded = false }
                    ) {
                        Text("See Less")
                    }
                }
            }
            
            // Workout list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedWorkouts.forEach { (workoutName, date, duration) ->
                    WorkoutDurationItem(
                        workoutName = workoutName,
                        date = date,
                        duration = duration
                    )
                }
            }
            
            // Show expand button when not expanded and there are more than 3 workouts
            if (!isExpanded && workoutDurations.size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { isExpanded = true }
                    ) {
                        Text("Show All (${workoutDurations.size} workouts)")
                    }
                }
            }
            
            // Show "See Less" button at bottom when expanded
            if (isExpanded && workoutDurations.size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { isExpanded = false }
                    ) {
                        Text("See Less")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDurationItem(
    workoutName: String,
    date: Date,
    duration: Long
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val formattedDate = dateFormat.format(date)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workoutName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WeeklyActivityHeatmapTab(
    workoutSessions: List<SessionWorkoutWithMuscles>,
    paddingValues: PaddingValues
) {
    // Calculate workout frequency by day of the week
    val dayOfWeekFrequency = workoutSessions
        .groupBy { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        .mapValues { it.value.size }
    
    // Create a map for all days of the week (1 = Sunday, 7 = Saturday)
    val allDays = (1..7).associateWith { dayOfWeekFrequency[it] ?: 0 }
    val maxFrequency = allDays.values.maxOrNull() ?: 1
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Weekly Activity Heatmap",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (workoutSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workout data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            WeeklyHeatmap(
                dayFrequency = allDays,
                maxFrequency = maxFrequency
            )
        }
    }
}

@Composable
fun WeeklyHeatmap(
    dayFrequency: Map<Int, Int>,
    maxFrequency: Int
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Workout Frequency by Day",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Heatmap grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayNames.forEachIndexed { index, dayName ->
                    val frequency = dayFrequency[index + 1] ?: 0
                    val intensity = if (maxFrequency > 0) {
                        (frequency.toFloat() / maxFrequency.toFloat())
                    } else 0f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day name
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Heatmap cell
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = when {
                                        intensity == 0f -> MaterialTheme.colorScheme.surfaceVariant
                                        intensity <= 0.25f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        intensity <= 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        intensity <= 0.75f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = frequency.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (intensity > 0.5f) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1-2",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "3-4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "5+",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary statistics
            val totalWorkouts = dayFrequency.values.sum()
            val mostActiveDay = dayFrequency.maxByOrNull { it.value }
            val averageWorkoutsPerDay = if (dayFrequency.isNotEmpty()) {
                totalWorkouts.toFloat() / dayFrequency.size
            } else 0f
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Workouts:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalWorkouts.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Avg per Day:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.1f".format(averageWorkoutsPerDay),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (mostActiveDay != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Most Active:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dayNames[mostActiveDay.key - 1],
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightProgressionTab(
    weightProgression: Map<String, List<WeightProgressionData>>,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Weight Progression",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        if (weightProgression.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No weight progression data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            weightProgression.forEach { (exerciseName, progressionData) ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Simple bar chart for weight progression
                            if (progressionData.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val maxWeight = progressionData.maxOfOrNull { it.maxWeight } ?: 0f
                                    
                                    progressionData.forEach { data ->
                                        val heightRatio = if (maxWeight > 0f) data.maxWeight / maxWeight else 0f
                                        val barHeight = (heightRatio * 100).dp
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(barHeight)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                    )
                                            )
                                            Text(
                                                text = "${data.maxWeight.toInt()}kg",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Progression details
                                Column(
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    progressionData.sortedBy { it.date }.forEach { data ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("MMM dd", Locale.getDefault())
                                                    .format(Date(data.date)),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Max: ${data.maxWeight.toInt()}kg, Avg: ${data.avgWeight.toInt()}kg",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Need more data points to show progression",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeProgressionTab(
    volumeProgression: Map<String, List<VolumeProgressionData>>,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Volume Analysis",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        if (volumeProgression.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No volume analysis data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Group by muscle group and show total volume
            val muscleGroupVolume = volumeProgression.entries.groupBy { (_, data) ->
                // Get muscle group from the first data point (they should all be the same for an exercise)
                data.firstOrNull()?.muscleGroup ?: "Unknown"
            }.mapValues { (_, entries) ->
                entries.sumOf { (_, data) -> data.sumOf { it.totalVolume } }
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total Volume by Muscle Group",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Bar chart for muscle group volume
                        if (muscleGroupVolume.isNotEmpty()) {
                            val maxVolume = muscleGroupVolume.values.maxOrNull() ?: 0
                            
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                muscleGroupVolume.entries.sortedByDescending { it.value }.forEach { (muscleGroup, volume) ->
                                    val heightRatio = if (maxVolume > 0) volume.toFloat() / maxVolume else 0f
                                    val barHeight = (heightRatio * 200).dp
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = muscleGroup,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(start = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = "$volume kg",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Show detailed volume progression by exercise
            volumeProgression.forEach { (exerciseName, progressionData) ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Simple bar chart for volume progression
                            if (progressionData.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val maxVolume = progressionData.maxOfOrNull { it.totalVolume } ?: 0
                                    
                                    progressionData.forEach { data ->
                                        val heightRatio = if (maxVolume > 0) data.totalVolume.toFloat() / maxVolume else 0f
                                        val barHeight = (heightRatio * 100).dp
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(barHeight)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                    )
                                            )
                                            Text(
                                                text = "${data.totalVolume}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Progression details
                                Column(
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    progressionData.sortedBy { it.date }.forEach { data ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("MMM dd", Locale.getDefault())
                                                    .format(Date(data.date)),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Volume: ${data.totalVolume} kg",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Need more data points to show progression",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UndertrainedMusclesSection(
    muscleGroupFrequency: List<Pair<String, Int>>,
    musclePartsFrequency: List<Pair<String, Int>>
) {
    // Find muscles with 0 or very low training frequency
    val undertrainedGroups = muscleGroupFrequency.filter { it.second <= 1 }
    val undertrainedParts = musclePartsFrequency.filter { it.second <= 1 }
    
    if (undertrainedGroups.isNotEmpty() || undertrainedParts.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Undertrained Muscles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (undertrainedGroups.isNotEmpty()) {
                    Text(
                        text = "Muscle Groups:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(undertrainedGroups) { (muscle, count) ->
                            UndertrainedMuscleChip(
                                muscleName = muscle,
                                count = count
                            )
                        }
                    }
                }
                
                if (undertrainedParts.isNotEmpty()) {
                    Text(
                        text = "Muscle Parts:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(undertrainedParts) { (muscle, count) ->
                            UndertrainedMuscleChip(
                                muscleName = muscle,
                                count = count
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UndertrainedMuscleChip(
    muscleName: String,
    count: Int
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = muscleName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count times",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WorkoutDetailDialog(
    session: SessionWorkoutWithMuscles,
    exerciseSessions: List<SessionEntityExercise>,
    onDismiss: () -> Unit,
    viewModel: HistoryViewModel
) {
    // Filter exercises for this specific session
    val sessionExercises = exerciseSessions.filter { it.sessionId == session.sessionId }
    
    // State for exercise names
    var exerciseNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    
    // Fetch exercise names
    LaunchedEffect(sessionExercises) {
        val names = mutableMapOf<Long, String>()
        sessionExercises.forEach { exerciseSession ->
            try {
                val exercise = viewModel.getExerciseById(exerciseSession.exerciseId)
                names[exerciseSession.exerciseId] = exercise?.name ?: "Unknown Exercise"
            } catch (e: Exception) {
                names[exerciseSession.exerciseId] = "Unknown Exercise"
            }
        }
        exerciseNames = names
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                    )
                ),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with close button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D3748).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.workoutName ?: "Workout Details",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE53E3E)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Workout summary with improved design
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        val duration = session.endTime - session.startTime
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            .format(Date(session.startTime))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date & Time",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFCBD5E0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF38B2AC)
                                )
                            ) {
                                Text(
                                    text = formatDuration(duration),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (session.muscleGroups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Muscles worked: ${session.muscleGroups.keys.joinToString(", ") { it.replaceFirstChar { char -> char.uppercase() } }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exercises list
                Text(
                    text = "Exercises (${sessionExercises.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (sessionExercises.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "No exercises recorded for this session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sessionExercises.forEach { exercise ->
                            val exerciseName = exerciseNames[exercise.exerciseId] ?: "Unknown Exercise"
                            ExerciseDetailCard(
                                exercise = exercise,
                                exerciseName = exerciseName
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailCard(
    exercise: SessionEntityExercise,
    exerciseName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D3748).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Exercise name and muscle group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF38B2AC),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE53E3E)
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = exercise.muscleGroup.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (exercise.muscleParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Target: ${exercise.muscleParts}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E0),
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sets information with better styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sets Completed",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.SemiBold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (exercise.completedSets == exercise.sets) 
                            Color(0xFF38B2AC)
                        else 
                            Color(0xFFE53E3E)
                    )
                ) {
                    Text(
                        text = "${exercise.completedSets}/${exercise.sets}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sets details with improved design
            if (exercise.repsOrTime.isNotEmpty() && exercise.weight.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Set Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38B2AC)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        exercise.repsOrTime.forEachIndexed { index, repsOrTime ->
                            val weight = exercise.weight.getOrNull(index)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2D3748).copy(alpha = 0.8f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Set ${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFCBD5E0),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = buildString {
                                            if (weight != null) append("${weight}kg")
                                            if (repsOrTime != null) {
                                                if (weight != null) append(" × ")
                                                append("$repsOrTime")
                                                // Assume it's reps if < 60, otherwise time in seconds
                                                if (repsOrTime >= 60) append("s")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF38B2AC),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Additional details
            if (exercise.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A5568).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38B2AC)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exercise.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E0)
                        )
                    }
                }
            }
            
            // Recovery factors with improved design
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A5568).copy(alpha = 0.8f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Recovery Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38B2AC)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RecoveryMetricCard(
                            label = "RPE",
                            value = "${exercise.rpe}",
                            color = Color(0xFF38B2AC)
                        )
                        RecoveryMetricCard(
                            label = "Soreness",
                            value = "${exercise.subjectiveSoreness}",
                            color = Color(0xFFE53E3E)
                        )
                        RecoveryMetricCard(
                            label = "Novelty",
                            value = "${exercise.noveltyFactor}",
                            color = Color(0xFF9F7AEA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryMetricCard(
    label: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D3748).copy(alpha = 0.8f)
        ),
        modifier = Modifier.padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E0),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
