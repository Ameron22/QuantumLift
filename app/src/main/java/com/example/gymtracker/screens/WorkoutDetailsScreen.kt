package com.example.gymtracker.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.gymtracker.R
import com.example.gymtracker.data.AppDatabase
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.gymtracker.components.SliderWithLabel
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutWithExercises
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.ExerciseWithWorkoutData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.viewmodels.CurrentWorkoutState
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.data.ExerciseDao
import java.util.Calendar
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import com.example.gymtracker.navigation.Screen
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.WorkoutExerciseWithDetails
import com.example.gymtracker.components.LoadingSpinner
import com.example.gymtracker.components.ExerciseGif
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import android.widget.Toast
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.example.gymtracker.data.WorkoutAchievementData
import com.example.gymtracker.services.AuthRepository
import com.example.gymtracker.data.WorkoutCompletionRequest
import com.example.gymtracker.components.ShareWorkoutDialog
import com.example.gymtracker.data.ShareWorkoutRequest
import com.example.gymtracker.data.Friend
import com.example.gymtracker.data.WorkoutExerciseShare

fun isCustomExercise(exerciseId: Int): Boolean = exerciseId > 750

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    workoutId: Int,
    navController: NavController,
    viewModel: WorkoutDetailsViewModel = viewModel(),
    generalViewModel: GeneralViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    var workoutWithExercises by remember { mutableStateOf<List<WorkoutWithExercises>?>(null) }
    var workoutName by remember { mutableStateOf("") }
    var startTimeWorkout: Long by remember { mutableLongStateOf(0L) }
    var workoutStarted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showSaveNotification by remember { mutableStateOf(false) }

    // Collect StateFlow values
    val workoutSession by viewModel.workoutSession.collectAsState()
    val recoveryFactors by viewModel.recoveryFactors.collectAsState()
    val exercisesList by viewModel.exercisesList.collectAsState()
    val currentWorkout by generalViewModel.currentWorkout.collectAsState()
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var currentRecoveryFactor by remember { mutableStateOf("") }
    var showRecoveryInfoDialog by remember { mutableStateOf(false) }
    
    // State for workout rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }

    // Drag and drop state
    var isDragging by remember { mutableStateOf(false) }
    var draggedItem by remember { mutableStateOf<WorkoutExerciseWithDetails?>(null) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var reorderedExercises by remember { mutableStateOf<List<WorkoutExerciseWithDetails>>(emptyList()) }
    var dragStartY by remember { mutableStateOf(0f) }
    var hasStartedDragging by remember { mutableStateOf(false) }
    var hasLoadedInitialData by remember { mutableStateOf(false) }
    
    // Sharing state
    var showShareDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isSharing by remember { mutableStateOf(false) }
    val authRepository = remember { AuthRepository(context) }
    
    // Initialize reorderedExercises when exercisesList changes
    LaunchedEffect(exercisesList) {
        // Only update reorderedExercises if we're not currently dragging
        // and if this is the initial load (reorderedExercises is empty and we haven't loaded data yet)
        if (!isDragging && reorderedExercises.isEmpty() && !hasLoadedInitialData) {
            reorderedExercises = exercisesList
        }
    }
    
    // Function to update database with new exercise order
    fun updateDatabaseOrder(fromIndex: Int, toIndex: Int) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(context).exerciseDao()
                    
                    // Only update exercises that actually changed position
                    val minIndex = minOf(fromIndex, toIndex)
                    val maxIndex = maxOf(fromIndex, toIndex)
                    
                    for (i in minIndex..maxIndex) {
                        val exerciseWithDetails = reorderedExercises[i]
                        // Update only the order field in the database - much more efficient
                        dao.updateWorkoutExerciseOrder(exerciseWithDetails.workoutExercise.id, i)
                    }
                    
                    // Update ViewModel with the reordered data
                    withContext(Dispatchers.Main) {
                        viewModel.updateExercisesOrder(reorderedExercises)
                    }
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error updating database order: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Function to handle real-time reordering during drag
    fun handleDragMove(dragAmount: Offset) {
        val fromIndex = draggedItemIndex ?: return
        
        // Only start processing after we've actually started dragging
        if (!hasStartedDragging) {
            hasStartedDragging = true
            return
        }
        
        // Accumulate the drag amount
        dragOffset += dragAmount
        
        // Use a fixed threshold that's approximately the card height in pixels
        // For most devices, 136dp converts to roughly 300-400 pixels
        val threshold = 300f
        
        if (kotlin.math.abs(dragOffset.y) > threshold) {
            val newIndex = when {
                dragOffset.y < -threshold && fromIndex > 0 -> fromIndex - 1 // Moving up
                dragOffset.y > threshold && fromIndex < reorderedExercises.size - 1 -> fromIndex + 1 // Moving down
                else -> fromIndex // No movement
            }
            
            if (newIndex != fromIndex) {
                val newList = reorderedExercises.toMutableList()
                val itemToMove = newList.removeAt(fromIndex)
                newList.add(newIndex, itemToMove)
                reorderedExercises = newList
                draggedItemIndex = newIndex
                
                // Update the database with the new order
                updateDatabaseOrder(fromIndex, newIndex)
                
                // Reset drag offset to prevent immediate jumping to next position
                dragOffset = Offset.Zero
            }
        }
    }

    // Scrolling text composable for long titles
    @Composable
    fun ScrollingText(
        text: String,
        modifier: Modifier = Modifier,
        style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
        color: Color = MaterialTheme.colorScheme.primary,
        maxWidth: androidx.compose.ui.unit.Dp = 200.dp
    ) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        var textWidth by remember { mutableStateOf(0f) }
        
        // Measure text width with safety checks
        LaunchedEffect(text, style) {
            if (text.isNotEmpty()) {
                try {
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = style,
                        constraints = Constraints()
                    )
                    textWidth = textLayoutResult.size.width.toFloat().coerceAtLeast(1f)
                } catch (e: Exception) {
                    textWidth = 100f // Fallback value
                }
            } else {
                textWidth = 1f // Avoid zero width
            }
        }
        
        // Convert maxWidth to pixels with safety check
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        
        // Only animate if text is longer than container
        val shouldScroll = textWidth > maxWidthPx && textWidth > 1f
        
        if (shouldScroll) {
            // Circular scrolling animation
            val infiniteTransition = rememberInfiniteTransition(label = "scrolling_text")
            val spacing = 50f // Space between text instances
            
            // For seamless loop, we need to scroll exactly the width of one text + spacing
            // so when the first text disappears, the second text is perfectly positioned
            val scrollDistance = textWidth + spacing
            
            val translateX by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -scrollDistance,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ((scrollDistance / 30f) * 1000).toInt()
                            .coerceAtLeast(3000) // Minimum 3 seconds
                            .coerceAtMost(8000), // Maximum 8 seconds
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "text_scroll"
            )
            
            Box(
                modifier = modifier
                    .width(maxWidth)
                    .clipToBounds()
                    .height(with(LocalDensity.current) { style.fontSize.toDp() * 1.5f }) // Ensure proper height
            ) {
                // First text instance
                Text(
                    text = text,
                    style = style,
                    color = color,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = translateX
                        }
                )
                
                // Second text instance positioned to create seamless loop
                Text(
                    text = text,
                    style = style,
                    color = color,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = translateX + scrollDistance
                        }
                )
            }
        } else {
            // Static text when no scrolling is needed
            Box(
                modifier = modifier.width(maxWidth)
            ) {
                Text(
                    text = text,
                    style = style,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
    
    // Sharing functions
    fun loadFriends() {
        coroutineScope.launch {
            try {
                val result = authRepository.getFriendsList()
                if (result.isSuccess) {
                    friends = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("WorkoutDetailsScreen", "Failed to load friends: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error loading friends: ${e.message}")
            }
        }
    }
    
    fun shareWorkout(targetUserIds: List<String>) {
        if (targetUserIds.isEmpty()) return
        
        coroutineScope.launch {
            try {
                isSharing = true
                
                // Get local workout data
                val localWorkout = dao.getAllWorkouts().find { it.id == workoutId }
                val localExercises = dao.getExercisesWithWorkoutData(workoutId)
                
                if (localWorkout == null) {
                    Toast.makeText(context, "Workout not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Convert to sharing format
                val exercisesForSharing = localExercises.map { exerciseWithDetails ->
                    val exercise = exerciseWithDetails.exercise
                    val isCustom = isCustomExercise(exercise.id)
                    
                    WorkoutExerciseShare(
                        exerciseId = if (!isCustom) exercise.id else null,
                        exerciseName = exercise.name,
                        isCustomExercise = isCustom,
                        customExerciseData = if (isCustom) exercise else null
                    )
                }
                
                val request = ShareWorkoutRequest(
                    workoutId = workoutId,
                    workoutName = localWorkout.name,
                    difficulty = null, // EntityWorkout doesn't have difficulty field
                    exercises = exercisesForSharing,
                    targetUserIds = targetUserIds
                )
                
                val result = authRepository.shareWorkout(request)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        Toast.makeText(context, "Workout shared successfully!", Toast.LENGTH_SHORT).show()
                        showShareDialog = false
                    } else {
                        Toast.makeText(context, response?.message ?: "Failed to share workout", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to share workout: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error sharing workout: ${e.message}")
                Toast.makeText(context, "Error sharing workout: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSharing = false
            }
        }
    }

    // Add LaunchedEffect to sync workoutStarted with CurrentWorkoutViewModel state
    LaunchedEffect(currentWorkout) {
        workoutStarted = currentWorkout?.isActive ?: false
        startTimeWorkout = currentWorkout?.startTime ?: 0L
        Log.d("WorkoutDetailsScreen", "Synced workoutStarted state: $workoutStarted, currentWorkout isActive: ${currentWorkout?.isActive}, startTime: ${currentWorkout?.startTime}")
    }
    
    // Load friends when share dialog is shown
    LaunchedEffect(showShareDialog) {
        if (showShareDialog && friends.isEmpty()) {
            loadFriends()
        }
    }

    // Fetch workout data and sync with ViewModel
    LaunchedEffect(workoutId) {
        try {
            Log.d("WorkoutDetailsScreen", "Loading workout data for ID: $workoutId")
            
            // Reset flags for new workout
            hasLoadedInitialData = false
            reorderedExercises = emptyList()
            
            // Clear ViewModel state at the beginning to ensure clean state for each workout
            viewModel.clearExercises()
            
                        // Check if we have an active workout for this workoutId
            val hasActiveWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
            val hasCompletedExercises = currentWorkout?.completedExercises?.isNotEmpty() == true
            val hasInitializedWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == false
            
            Log.d("WorkoutDetailsScreen", "Current workout state: workoutId=${currentWorkout?.workoutId}, isActive=${currentWorkout?.isActive}, completedExercises=${currentWorkout?.completedExercises}")
            Log.d("WorkoutDetailsScreen", "hasActiveWorkout=$hasActiveWorkout, hasCompletedExercises=$hasCompletedExercises, hasInitializedWorkout=$hasInitializedWorkout")
            
            if (hasActiveWorkout || hasCompletedExercises) {
                Log.d("WorkoutDetailsScreen", "Resuming existing workout - preserving completed exercises")
                viewModel.resetWorkoutSession()
            } else if (hasInitializedWorkout) {
                Log.d("WorkoutDetailsScreen", "Using existing initialized workout")
                viewModel.resetWorkoutSession()
            } else {
                Log.d("WorkoutDetailsScreen", "Starting fresh workout - clearing completed exercises")
                viewModel.resetWorkoutSessionAndClearCompleted()
                // Initialize a new workout session in GeneralViewModel (not active yet)
                // We'll initialize it later after we get the workout name from database
            }

            withContext(Dispatchers.IO) {
                // Get exercises with their workout-specific data
                val exercisesData = dao.getExercisesWithWorkoutData(workoutId)
                // Get the workout info
                val workout = dao.getAllWorkouts().find { it.id == workoutId }
                
                withContext(Dispatchers.Main) {
                    Log.d("WorkoutDetailsScreen", "Workout data loaded: ${exercisesData.size} exercises")

                    // Set workout name regardless of whether there are exercises or not
                    workoutName = workout?.name ?: "Unknown Workout"
                   

                    if (exercisesData.isEmpty()) {
                        Log.e("WorkoutDetailsScreen", "No exercises found for workout ID: $workoutId")
                        isLoading = false
                    } else {
                        // Convert to WorkoutExerciseWithDetails and sync with ViewModel
                        val workoutExerciseWithDetails = exercisesData.map { 
                            WorkoutExerciseWithDetails(it.workoutExercise, it.exercise) 
                        }
                        // Initialize reorderedExercises with the fetched data
                        reorderedExercises = workoutExerciseWithDetails
                        hasLoadedInitialData = true
                        workoutExerciseWithDetails.forEach { exerciseWithDetails ->
                            viewModel.addExercise(exerciseWithDetails)
                        }

                        // Check if we have an active workout in GeneralViewModel
                        val hasActiveWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
                        val hasInitializedWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == false
                        
                        if (hasActiveWorkout) {
                            Log.d("WorkoutDetailsScreen", "Using existing active workout session from GeneralViewModel")
                            workoutStarted = currentWorkout?.isActive ?: false
                            if (workoutStarted) {
                                startTimeWorkout = currentWorkout?.startTime ?: System.currentTimeMillis()
                            }
                        } else if (hasInitializedWorkout) {
                            Log.d("WorkoutDetailsScreen", "Using existing initialized workout session from GeneralViewModel")
                            workoutStarted = false // Not started yet
                        } else {
                            Log.d("WorkoutDetailsScreen", "Initializing new workout session")
                            viewModel.initializeWorkoutSession(workoutId, workoutName)
                            // Only initialize a new workout session if there's no active workout
                            val hasAnyWorkout = currentWorkout != null
                            val hasActiveWorkout = hasAnyWorkout && currentWorkout?.isActive == true
                            val isDifferentWorkout = hasAnyWorkout && currentWorkout?.workoutId != workoutId
                            
                            // Only initialize if there's no workout at all, or if there's no active workout and it's a different workout
                            if (!hasAnyWorkout || (!hasActiveWorkout && isDifferentWorkout)) {
                                // Initialize a new workout session in GeneralViewModel (not active yet)
                                coroutineScope.launch {
                                    generalViewModel.initializeWorkoutWithName(workoutId, context)
                                }
                            }
                        }

                        Log.d("WorkoutDetailsScreen", "Workout name: $workoutName")
                        Log.d(
                            "WorkoutDetailsScreen",
                            "Number of exercises: ${exercisesData.size}"
                        )
                        
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Database error: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                navController.popBackStack()
            }
        }
    }

    // Listen for new exercises added from AddExerciseToWorkoutScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Int?>("newExerciseId", null)
            ?.collect { newExerciseId ->
                if (newExerciseId != null) {
                    Log.d("WorkoutDetailsScreen", "New exercise added with ID: $newExerciseId")
                    // Refresh exercises from database
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val updatedExercisesData = dao.getExercisesWithWorkoutData(workoutId)
                            withContext(Dispatchers.Main) {
                                // Update ViewModel and reorderedExercises
                                val workoutExerciseWithDetails = updatedExercisesData.map { 
                                    WorkoutExerciseWithDetails(it.workoutExercise, it.exercise) 
                                }
                                reorderedExercises = workoutExerciseWithDetails
                                viewModel.updateExercisesOrder(workoutExerciseWithDetails)
                            }
                        }
                    }
                    // Clear the flag
                    navController.currentBackStackEntry?.savedStateHandle?.set("newExerciseId", null)
                }
            }
    }

    // Function to rename the workout
    fun renameWorkout() {
        if (newWorkoutName.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.getDatabase(context).exerciseDao()
                        val workout = dao.getAllWorkouts().find { it.id == workoutId }
                        workout?.let {
                            val updatedWorkout = it.copy(name = newWorkoutName)
                            dao.updateWorkout(updatedWorkout)
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        workoutName = newWorkoutName
                        newWorkoutName = ""
                        showRenameDialog = false
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutDetailsScreen", "Error renaming workout: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // Function to reorder exercises
    fun reorderExercises(fromIndex: Int, toIndex: Int) {
        if (fromIndex != toIndex && fromIndex in reorderedExercises.indices && toIndex in reorderedExercises.indices) {
            val newList = reorderedExercises.toMutableList()
            val itemToMove = newList.removeAt(fromIndex)
            newList.add(toIndex, itemToMove)
            reorderedExercises = newList
            
            // Update database in background - only update the order field for affected exercises
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.getDatabase(context).exerciseDao()
                        
                        // Only update exercises that actually changed position
                        val minIndex = minOf(fromIndex, toIndex)
                        val maxIndex = maxOf(fromIndex, toIndex)
                        
                        for (i in minIndex..maxIndex) {
                            val exerciseWithDetails = reorderedExercises[i]
                            // Update only the order field in the database - much more efficient
                            dao.updateWorkoutExerciseOrder(exerciseWithDetails.workoutExercise.id, i)
                        }
                        
                        // Update ViewModel with the reordered data
                        withContext(Dispatchers.Main) {
                            viewModel.updateExercisesOrder(reorderedExercises)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutDetailsScreen", "Error reordering exercises: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // Function to end the workout session
    fun endWorkoutSession() {
        Log.d("WorkoutDetailsScreen", "Ending workout session")
        coroutineScope.launch {
            try {
                // First, save the workout session data
                withContext(Dispatchers.IO) {
                    val endTime = System.currentTimeMillis()
                    val currentWorkoutState = currentWorkout
                    
                    if (currentWorkoutState == null || !currentWorkoutState.isActive) {
                        Log.d("WorkoutDetailsScreen", "No active workout to save")
                        return@withContext
                    }
                    
                    val duration = endTime - currentWorkoutState.startTime
                    
                    Log.d("WorkoutDetailsScreen", "Saving workout session - Start: ${currentWorkoutState.startTime}, End: $endTime, Duration: $duration")
                    Log.d("WorkoutDetailsScreen", "Session details - ID: ${currentWorkoutState.sessionId}, Workout ID: ${currentWorkoutState.workoutId}, Name: ${currentWorkoutState.workoutName}")

                    val sessionWorkout = SessionWorkoutEntity(
                        sessionId = currentWorkoutState.sessionId,
                        workoutId = currentWorkoutState.workoutId,
                        workoutName = currentWorkoutState.workoutName,
                        startTime = currentWorkoutState.startTime,
                        endTime = endTime
                    )

                    Log.d("WorkoutDetailsScreen", "Saving session: ID=${sessionWorkout.sessionId}, Name='${sessionWorkout.workoutName}', Start=${sessionWorkout.startTime}, End=${sessionWorkout.endTime}, Duration=${(sessionWorkout.endTime - sessionWorkout.startTime) / (60 * 1000)} min")

                    // Check if session already exists
                    val existingSession = dao.getWorkoutSession(sessionWorkout.sessionId)
                    if (existingSession != null) {
                        Log.d("WorkoutDetailsScreen", "Updating existing session: ${sessionWorkout.sessionId}")
                        dao.updateWorkoutSession(sessionWorkout)
                    } else {
                        Log.d("WorkoutDetailsScreen", "Inserting new session: ${sessionWorkout.sessionId}")
                        dao.insertWorkoutSession(sessionWorkout)
                    }
                    
                    // Verify the session was saved
                    val savedSession = dao.getWorkoutSession(sessionWorkout.sessionId)
                    Log.d("WorkoutDetailsScreen", "Verification - Saved session: ${savedSession?.workoutName}, Duration: ${savedSession?.let { (it.endTime - it.startTime) / (60 * 1000) }} min")

                    // Update achievements
                    val totalWorkouts = dao.getTotalWorkoutCount()
                    val isNightWorkout = isNightWorkout(sessionWorkout.startTime)
                    
                    // Update workout count achievement
                    val achievementManager = AchievementManager.getInstance()
                    achievementManager.updateWorkoutCount(totalWorkouts)
                    
                    // Update night workout achievement if applicable
                    if (isNightWorkout) {
                        achievementManager.updateSpecialChallenges("night_owl")
                    }

                    // Calculate and update streak
                    val streak = calculateWorkoutStreak(dao)
                    achievementManager.updateConsistencyStreak(streak)
                    
                    // Get newly unlocked achievements that haven't been posted to feed yet
                    val newlyUnlockedAchievementsForFeed = achievementManager.getNewlyUnlockedAchievementsForFeed()
                    
                    // Convert achievement IDs to WorkoutAchievementData objects
                    val achievementDataList = newlyUnlockedAchievementsForFeed.map { achievementId ->
                        WorkoutAchievementData(
                            id = achievementId,
                            additionalInfo = when (achievementId) {
                                "bench_press_100" -> "New PR: 100kg bench press!"
                                "workout_warrior" -> "Completed 10 workouts!"
                                "workout_master" -> "Completed 50 workouts!"
                                "consistency_week" -> "7-day streak achieved!"
                                "consistency_month" -> "30-day streak achieved!"
                                "night_owl" -> "Late night workout completed!"
                                else -> null
                            }
                        )
                    }
                    
                    // Share workout completion to feed if user is authenticated
                    try {
                        val authRepository = AuthRepository(context)
                        
                        // Get completed exercises for this session
                        val completedExercises = dao.getExerciseSessionsForSession(sessionWorkout.sessionId)
                        val exerciseNames = completedExercises.map { exercise ->
                            // Get exercise name from exercise ID
                            val exerciseEntity = dao.getExerciseById(exercise.exerciseId.toInt())
                            exerciseEntity?.name ?: "Unknown Exercise"
                        }
                        
                        // Calculate total sets and weight
                        val totalSets = completedExercises.sumOf { it.completedSets }
                        val totalWeight = completedExercises.sumOf { exercise ->
                            exercise.weight.filterNotNull().sum()
                        }.toDouble()
                        
                        val request = WorkoutCompletionRequest(
                            workoutId = currentWorkoutState.workoutId,
                            workoutName = currentWorkoutState.workoutName,
                            duration = duration,
                            exercises = exerciseNames,
                            totalSets = totalSets,
                            totalWeight = totalWeight,
                            achievements = achievementDataList,
                            shareToFeed = false, // Let the server decide based on user settings
                            privacyLevel = "FRIENDS"
                        )
                        
                        val result = authRepository.completeWorkout(request)
                        result.fold(
                            onSuccess = { completionResponse ->
                                Log.d("WorkoutDetailsScreen", "Workout shared to feed: ${completionResponse.shared}")
                                // Mark achievements as posted to feed to prevent duplicates in future posts
                                if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                                    achievementManager.markAchievementsAsPostedToFeed(newlyUnlockedAchievementsForFeed)
                                    Log.d("WorkoutDetailsScreen", "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed")
                                }
                            },
                            onFailure = { exception ->
                                Log.w("WorkoutDetailsScreen", "Failed to share workout to feed: ${exception.message}")
                                // Even if sharing fails, mark achievements as posted to prevent duplicates
                                // The achievements are still properly unlocked in the database
                                if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                                    achievementManager.markAchievementsAsPostedToFeed(newlyUnlockedAchievementsForFeed)
                                    Log.d("WorkoutDetailsScreen", "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed despite failure")
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("WorkoutDetailsScreen", "Error sharing workout to feed: ${e.message}")
                        // Even if there's an error, mark achievements as posted to prevent duplicates
                        if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                            achievementManager.markAchievementsAsPostedToFeed(newlyUnlockedAchievementsForFeed)
                            Log.d("WorkoutDetailsScreen", "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed despite error")
                        }
                        // Don't fail the workout completion if sharing fails
                    }
                }

                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    // Stop the workout session in ViewModel
                    viewModel.stopWorkoutSession()
                    
                    // End the current workout in GeneralViewModel
                    generalViewModel.endWorkout()
                    
                    // Show toast message
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    
                    // Navigate back immediately
                    viewModel.resetWorkoutSession()
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error ending workout session: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    viewModel.resetWorkoutSession()
                    navController.popBackStack()
                }
            }
        }
    }

    // Handle back navigation - just go back without confirmation
    BackHandler {
        navController.popBackStack()
    }

    // Update the Recovery Factor Dialog
    if (showRecoveryDialog) {
        val context = LocalContext.current
        
        fun vibrateOnValueChange() {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(currentRecoveryFactor) },
            text = {
                when {
                    currentRecoveryFactor.contains("Protein") -> {
                        SliderWithLabel(
                            value = recoveryFactors.proteinIntake.toFloat(),
                            onValueChange = {
                                vibrateOnValueChange()
                                viewModel.updateRecoveryFactors(proteinIntake = it.toInt())
                            },
                            label = "",
                            valueRange = 0f..300f,
                            steps = 30
                        )
                    }

                    else -> {
                        SliderWithLabel(
                            value = when {
                                currentRecoveryFactor.contains("Sleep") -> recoveryFactors.sleepQuality
                                currentRecoveryFactor.contains("Hydration") -> recoveryFactors.hydration
                                currentRecoveryFactor.contains("Stress") -> recoveryFactors.stressLevel
                                else -> 5
                            }.toFloat(),
                            onValueChange = {
                                vibrateOnValueChange()
                                when {
                                    currentRecoveryFactor.contains("Sleep") ->
                                        viewModel.updateRecoveryFactors(sleepQuality = it.toInt())

                                    currentRecoveryFactor.contains("Hydration") ->
                                        viewModel.updateRecoveryFactors(hydration = it.toInt())

                                    currentRecoveryFactor.contains("Stress") ->
                                        viewModel.updateRecoveryFactors(stressLevel = it.toInt())
                                }
                            },
                            label = "",
                            valueRange = 1f..10f,
                            steps = 9
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }





    // Function to format duration in HH:MM:SS format
    fun formatDuration(durationInMillis: Long): String {
        val durationInSeconds = durationInMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Function to calculate current duration
    fun calculateCurrentDuration(startTimeWorkout: Long, workoutStarted: Boolean): String {
        return if (startTimeWorkout > 0 && workoutStarted) {
            val currentDuration = System.currentTimeMillis() - startTimeWorkout
            formatDuration(currentDuration)
        } else {
            "00:00"
        }
    }

    @Composable
    fun WorkoutDurationDisplay(startTimeWorkout: Long, workoutStarted: Boolean) {
        var durationText by remember { mutableStateOf("00:00") }
        var lastUpdateTime by remember { mutableLongStateOf(0L) }

        LaunchedEffect(workoutStarted, startTimeWorkout) {
            while (workoutStarted && startTimeWorkout > 0) {
                lastUpdateTime = System.currentTimeMillis()
                durationText = calculateCurrentDuration(startTimeWorkout, workoutStarted)
                delay(1000) // Update every second
            }
        }

        Text(
            text = "Duration: $durationText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    // Function to display exercise details
    @Composable
    fun ExerciseItem(
        exercise: EntityExercise,
        workoutExercise: WorkoutExercise,
        index: Int,
        context: Context,
        currentWorkout: CurrentWorkoutState?,
        onDelete: () -> Unit = {},
        onDragStart: (WorkoutExerciseWithDetails, Int) -> Unit = { _, _ -> },
        onDragEnd: () -> Unit = {},
        onDragMove: (Offset) -> Unit = {}
    ) {
        val isBeingDragged = draggedItemIndex == index
        val scale by animateFloatAsState(
            targetValue = if (isBeingDragged) 1.05f else 1f,
            animationSpec = tween(200),
            label = "scale"
        )
        
        val elevation by animateFloatAsState(
            targetValue = if (isBeingDragged) 8f else 2f,
            animationSpec = tween(200),
            label = "elevation"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .scale(scale)
                .zIndex(if (isBeingDragged) 1000f else 0f)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            isDragging = true
                            draggedItem = WorkoutExerciseWithDetails(workoutExercise, exercise)
                            draggedItemIndex = index
                            dragOffset = offset
                            onDragStart(WorkoutExerciseWithDetails(workoutExercise, exercise), index)
                        },
                        onDragEnd = {
                            isDragging = false
                            draggedItem = null
                            draggedItemIndex = null
                            dragOffset = Offset.Zero
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging = false
                            draggedItem = null
                            draggedItemIndex = null
                            dragOffset = Offset.Zero
                            onDragEnd()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragMove(dragAmount)
                        }
                    )
                }
                .clickable(enabled = !isDragging) {
                    // Check if there's an active workout from a different workout
                    val hasActiveWorkoutFromDifferentWorkout = currentWorkout?.isActive == true && currentWorkout?.workoutId != workoutId
                    
                    if (hasActiveWorkoutFromDifferentWorkout) {
                        // Show toast message and block navigation
                        Toast.makeText(context, "Please finish your active workout first", Toast.LENGTH_SHORT).show()
                    } else {
                        // Navigate to exercise screen - workout session will be started when timer starts
                        val sessionId = workoutSession?.sessionId?.toLong() ?: currentWorkout?.sessionId ?: System.currentTimeMillis()
                        navController.navigate(Screen.Exercise.createRoute(exercise.id, sessionId, workoutId))
                    }
                },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Exercise GIF
                    ExerciseGif(
                        gifPath = exercise.gifUrl,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    // Exercise details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sets: ${workoutExercise.sets}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (exercise.useTime && workoutExercise.reps > 1000) {
                                val timeInSeconds = workoutExercise.reps - 1000
                                val minutes = timeInSeconds / 60
                                val seconds = timeInSeconds % 60
                                "Time: ${minutes}:${String.format("%02d", seconds)}"
                            } else {
                                "Reps: ${workoutExercise.reps}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (!exercise.useTime) {
                            Text(
                                text = "Weight: ${workoutExercise.weight}kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "Difficulty: ${exercise.difficulty}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Muscle Group: ${exercise.muscle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Delete button (top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.minus_icon),
                        contentDescription = "Delete Exercise",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Drag handle (middle right)
                Icon(
                    painter = painterResource(id = R.drawable.drag_handle_icon),
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(24.dp)
                )

                // Completion indicator moved to bottom right
                // Only show completed exercises when we're viewing the active workout
                val isActiveWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
                val isCompleted = if (isActiveWorkout) {
                    generalViewModel.isExerciseCompleted(exercise.id)
                } else {
                    false
                }
                Log.d("WorkoutDetailsScreen", "Exercise ${exercise.id} (${exercise.name}) completed: $isCompleted, isActiveWorkout: $isActiveWorkout")
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(14.dp)
                            .background(
                                color = Color.Green,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }

    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScrollingText(
                            text = workoutName,
                            maxWidth = 200.dp,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                newWorkoutName = workoutName
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Workout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
                actions = {
                    val isActiveWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
                    if (workoutStarted && isActiveWorkout) {
                        Button(
                            onClick = { endWorkoutSession() },
                            modifier = Modifier.padding(start = 4.dp, end = 0.dp, top = 2.dp, bottom = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !showSaveNotification,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Save", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { showExitDialog = true },
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Exit", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        // Share button - only show when workout is not active
                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Workout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            // Remove Finish Workout button from bottom bar
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout duration display
                if (workoutStarted) {
                    WorkoutDurationDisplay(startTimeWorkout, workoutStarted)
                }

                // Exercise list
                reorderedExercises.forEachIndexed { index, exerciseWithDetails ->
                    ExerciseItem(
                        exercise = exerciseWithDetails.entityExercise,
                        workoutExercise = exerciseWithDetails.workoutExercise,
                        index = index,
                        context = context,
                        currentWorkout = currentWorkout,
                        onDelete = {
                            coroutineScope.launch {
                                // Remove the exercise from the workout
                                dao.deleteWorkoutExercise(exerciseWithDetails.workoutExercise)
                                // Update reorderedExercises by removing the deleted item
                                val updatedReorderedExercises = reorderedExercises.filter { 
                                    it.workoutExercise.id != exerciseWithDetails.workoutExercise.id 
                                }
                                reorderedExercises = updatedReorderedExercises
                                // Update ViewModel efficiently
                                viewModel.updateExercisesOrder(updatedReorderedExercises)
                            }
                        },
                        onDragStart = { _, _ ->
                            // Drag started
                            dragStartY = 0f
                            hasStartedDragging = false
                        },
                                                onDragEnd = {
                            // Reset dragging state
                            hasStartedDragging = false
                            
                            // Save any final changes to the database
                            draggedItemIndex?.let { finalIndex ->
                                // The database updates are already handled in handleDragMove
                                // but we can add a final save here if needed
                                Log.d("WorkoutDetailsScreen", "Drag ended at index: $finalIndex")
                            }
                        },
                        onDragMove = { offset ->
                            handleDragMove(offset)
                        }
                    )
                }

                // Add Exercise Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            navController.navigate(Screen.AddExerciseToWorkout.createRoute(workoutId))
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus_icon),
                            contentDescription = "Add Exercise",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Exercise",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Recovery Factors Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recovery Factors",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            IconButton(
                                onClick = { showRecoveryInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Recovery Factors Info",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Add Recovery Factors Info Dialog
                        if (showRecoveryInfoDialog) {
                            AlertDialog(
                                onDismissRequest = { showRecoveryInfoDialog = false },
                                title = { Text("Recovery Factors") },
                                text = {
                                    Text(
                                        "Recovery factors help calculate muscle stress and recovery more accurately. " +
                                        "If you provide all recovery factors, the app will use a more detailed formula " +
                                        "that takes into account your sleep quality, protein intake, hydration, and stress levels. " +
                                        "If any factor is not provided (set to 0), the app will use a simplified formula " +
                                        "based only on exercise load and volume.\n\n" +
                                        "Factors:\n" +
                                        "• Sleep Quality (1-10): How well you slept\n" +
                                        "• Protein Intake (g): Amount of protein consumed\n" +
                                        "• Hydration (1-10): Your hydration level\n" +
                                        "• Stress Level (1-10): Your current stress level"
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showRecoveryInfoDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Sleep", recoveryFactors.sleepQuality, "Sleep Quality (1-10)"),
                                Triple("Protein", recoveryFactors.proteinIntake, "Protein Intake (g)"),
                                Triple("Hydration", recoveryFactors.hydration, "Hydration Level (1-10)"),
                                Triple("Stress Lvl", recoveryFactors.stressLevel, "Stress Level (1-10)")
                            ).forEach { (shortLabel, value, fullLabel) ->
                                Button(
                                    onClick = {
                                        currentRecoveryFactor = fullLabel
                                        showRecoveryDialog = true
                                    },
                                    modifier = Modifier
                                        .height(80.dp)  // Increased height
                                        .weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (value > 0)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (value == 0) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.plus_icon),
                                                    contentDescription = "Add $shortLabel",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Factor-specific icon
                                        when (shortLabel) {
                                            "Sleep" -> Icon(
                                                painter = painterResource(id = R.drawable.sleep_icon),
                                                contentDescription = "Sleep Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Protein" -> Icon(
                                                painter = painterResource(id = R.drawable.food_icon),
                                                contentDescription = "Food Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Hydration" -> Icon(
                                                painter = painterResource(id = R.drawable.drop_icon),
                                                contentDescription = "Hydration Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            "Stress Lvl" -> Icon(
                                                painter = painterResource(id = R.drawable.stress_icon),
                                                contentDescription = "Stress Icon",
                                                modifier = Modifier.size(24.dp),
                                                tint = if (value > 0)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (value == 0) shortLabel
                                            else if (shortLabel == "Protein") "${value}g"
                                            else "$value",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (value > 0)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
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

    // Rename Workout Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                newWorkoutName = ""
            },
            title = { Text("Rename Workout") },
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
                    onClick = { renameWorkout() },
                    enabled = newWorkoutName.isNotEmpty()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        newWorkoutName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exit Workout confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Workout") },
            text = { Text("Are you sure you want to exit and delete this workout session? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    coroutineScope.launch {
                        // Remove session from DB
                        val sessionId = currentWorkout?.sessionId
                        if (sessionId != null && sessionId > 0) {
                            withContext(Dispatchers.IO) {
                                dao.deleteWorkoutSessionById(sessionId)
                            }
                        }
                        // Reset view models
                        viewModel.stopWorkoutSession()
                        generalViewModel.endWorkout()
                        navController.popBackStack()
                    }
                }) {
                    Text("Exit", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Share Workout Dialog
    if (showShareDialog) {
        ShareWorkoutDialog(
            friends = friends,
            onDismiss = { showShareDialog = false },
            onShare = { targetUserIds ->
                shareWorkout(targetUserIds)
            }
        )
    }
}

private suspend fun calculateWorkoutStreak(dao: ExerciseDao): Int {
    val sessions = dao.getAllWorkoutSessionsOrderedByDate()
    if (sessions.isEmpty()) return 0

    val calendar = Calendar.getInstance()
    var currentStreak = 1
    var lastWorkoutDate = calendar.apply { 
        timeInMillis = sessions.first().startTime 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    for (i in 1 until sessions.size) {
        val session = sessions[i]
        val sessionDate = calendar.apply { 
            timeInMillis = session.startTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Check if this workout was on the consecutive previous day
        if ((lastWorkoutDate - sessionDate) == TimeUnit.DAYS.toMillis(1)) {
            currentStreak++
            lastWorkoutDate = sessionDate
        } else {
            break
        }
    }

    return currentStreak
}

private fun isNightWorkout(startTime: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startTime
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    return currentHour >= 22 || currentHour < 4
}