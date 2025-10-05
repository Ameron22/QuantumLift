package com.example.gymtracker.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.gymtracker.components.SliderWithLabel
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.WorkoutExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.services.SorenessAssessmentService
import com.example.gymtracker.viewmodels.CurrentWorkoutState
import com.example.gymtracker.viewmodels.XPBuffer
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.data.ExerciseDao
import java.util.Calendar
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import com.example.gymtracker.navigation.Screen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.data.WorkoutExerciseWithDetails
import com.example.gymtracker.components.LoadingSpinner
import com.example.gymtracker.components.ExerciseGif
import com.example.gymtracker.data.WarmUpTemplate
import com.example.gymtracker.data.WarmUpExercise
import com.example.gymtracker.data.WarmUpTemplateWithExercises
import com.example.gymtracker.data.WorkoutWarmUp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import android.widget.Toast
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.example.gymtracker.data.WorkoutAchievementData
import com.example.gymtracker.services.AuthRepository
import com.example.gymtracker.data.WorkoutCompletionRequest
import com.example.gymtracker.components.ShareWorkoutDialog
import com.example.gymtracker.data.ShareWorkoutRequest
import com.example.gymtracker.data.Friend
import com.example.gymtracker.data.WorkoutExerciseShare
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import com.example.gymtracker.data.SessionEntityExercise
import com.example.gymtracker.data.XPSystem
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalConfiguration
import com.example.gymtracker.components.SwipeableExerciseCard
import com.example.gymtracker.components.ExerciseAlternativeDialog
import com.example.gymtracker.data.ExerciseAlternative
import com.example.gymtracker.data.ExerciseAlternativeWithDetails
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Test XP amount - change this value to test different XP amounts
private const val TEST_XP_AMOUNT = 666


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
    val warmUpDao = remember { db.warmUpDao() }
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
    val isBreakActive by viewModel.isBreakActive.collectAsState()
    val breakStartTime by viewModel.breakStartTime.collectAsState()
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var currentRecoveryFactor by remember { mutableStateOf("") }
    var showRecoveryInfoDialog by remember { mutableStateOf(false) }

    // State for workout rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }
    
    // State for completed exercise info dialog
    var showCompletedExerciseDialog by remember { mutableStateOf(false) }
    var completedExerciseInfo by remember { mutableStateOf<WorkoutExerciseWithDetails?>(null) }
    
    // Muscle soreness tracking state variables
    var eccentricFactor by remember { mutableStateOf(1.0f) }
    var noveltyFactor by remember { mutableStateOf(5) }
    var adaptationLevel by remember { mutableStateOf(5) }
    var rpe by remember { mutableStateOf(5) }
    var subjectiveSoreness by remember { mutableStateOf(5) }
    var showSorenessInfoDialog by remember { mutableStateOf<String?>(null) }
    var defaultSorenessValues by remember { mutableStateOf(true) }

    // Drag state variables
    var isDragging by remember { mutableStateOf(false) }
    var isFingerDown by remember { mutableStateOf(false) }
    var draggedItem by remember { mutableStateOf<WorkoutExerciseWithDetails?>(null) }
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hasStartedDragging by remember { mutableStateOf(false) }
    var autoScrollDirection by remember { mutableIntStateOf(0) }
    var autoScrollAmount by remember { mutableFloatStateOf(0f) }
    var dragDirection by remember { mutableIntStateOf(0) }
    var dragStartY by remember { mutableFloatStateOf(0f) }

    // Deselection delay state
    var isWaitingForContinuation by remember { mutableStateOf(false) }
    var deselectionTimeLeft by remember { mutableFloatStateOf(0f) }
    val deselectionDelaySeconds = 2f // 2 seconds delay
    var countdownJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastMovementTime by remember { mutableLongStateOf(0L) }
    var movementCheckJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var autoScrollRestartTrigger by remember { mutableIntStateOf(0) }

    // Other state variables
    var reorderedExercises by remember { mutableStateOf<List<WorkoutExerciseWithDetails>>(emptyList()) }
    var hasLoadedInitialData by remember { mutableStateOf(false) }
    
    // XP notification state
    var showXPNotification by remember { mutableStateOf(false) }
    var xpEarned by remember { mutableIntStateOf(0) }
    
    // Exercise alternatives state
    var showAlternativesDialog by remember { mutableStateOf(false) }
    var selectedWorkoutExercise by remember { mutableStateOf<WorkoutExerciseWithDetails?>(null) }
    var alternatives by remember { mutableStateOf<List<ExerciseAlternative>>(emptyList()) }
    var similarExercises by remember { mutableStateOf<List<EntityExercise>>(emptyList()) }
    var exerciseAlternatives by remember { mutableStateOf<Map<Int, List<EntityExercise>>>(emptyMap()) }
    
    // Warm-up state
    var showWarmUpDialog by remember { mutableStateOf(false) }
    var selectedWarmUp by remember { mutableStateOf<WarmUpTemplateWithExercises?>(null) }
    var warmUpExercises by remember { mutableStateOf<List<WarmUpExercise>>(emptyList()) }
    
    // Debug logging for warm-up state changes
    LaunchedEffect(selectedWarmUp) {
        Log.d("WorkoutDetailsScreen", "Warm-up state changed: selectedWarmUp=${selectedWarmUp?.template?.name}, exercises=${warmUpExercises.size}")
    }

    // Function to start countdown
    fun startCountdown() {
        countdownJob?.cancel() // Cancel any existing countdown
        isWaitingForContinuation = true
        deselectionTimeLeft = deselectionDelaySeconds
        Log.d("DRAG_DEBUG", "Starting deselection countdown: ${deselectionDelaySeconds}s")

        countdownJob = coroutineScope.launch {
            try {
                while (deselectionTimeLeft > 0 && isWaitingForContinuation && !isDragging && isFingerDown) {
                    delay(100)
                    deselectionTimeLeft -= 0.1f

                    // Log countdown periodically
                    if ((deselectionTimeLeft * 10).toInt() % 10 == 0) {
                        Log.d(
                            "DRAG_DEBUG",
                            "Deselection countdown: ${deselectionTimeLeft.toInt()}s"
                        )
                    }
                }

                // If countdown completed AND finger is still down, deselect everything
                if (deselectionTimeLeft <= 0 && isWaitingForContinuation && !isDragging && isFingerDown) {
                    Log.d("DRAG_DEBUG", "Deselection countdown completed - deselecting card")
                    isFingerDown = false
                    autoScrollDirection = 0
                    autoScrollAmount = 0f
                    draggedItem = null
                    draggedItemIndex = -1
                    dragOffset = Offset.Zero
                    hasStartedDragging = false
                    dragDirection = 0
                    isWaitingForContinuation = false
                    deselectionTimeLeft = 0f
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("DRAG_DEBUG", "Countdown cancelled via job cancellation")
            }
        }
    }

    // Function to cancel countdown
    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        isWaitingForContinuation = false
        deselectionTimeLeft = 0f
        autoScrollRestartTrigger++ // Force LaunchedEffect to restart
        Log.d("DRAG_DEBUG", "Countdown cancelled - dragging resumed, restart trigger: $autoScrollRestartTrigger")
    }

    // Sharing state
    var showShareDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isSharing by remember { mutableStateOf(false) }
    val authRepository = remember { AuthRepository(context) }

    // Function to order exercises by completion status (completed first, ordered by completion time)
    fun orderExercisesByCompletion(exercises: List<WorkoutExerciseWithDetails>): List<WorkoutExerciseWithDetails> {
        val currentWorkoutState = currentWorkout
        if (currentWorkoutState?.isActive != true) {
            // No active workout, return exercises in original order
            return exercises
        }
        
        val completedExercisesOrder = currentWorkoutState.completedExercisesOrder
        val completedExercises = currentWorkoutState.completedExercises
        
        Log.d("WorkoutDetailsScreen", "Ordering exercises by completion:")
        Log.d("WorkoutDetailsScreen", "  Completed exercises: $completedExercises")
        Log.d("WorkoutDetailsScreen", "  Completion order: $completedExercisesOrder")
        
        // Separate completed and uncompleted exercises
        val completedList = mutableListOf<WorkoutExerciseWithDetails>()
        val uncompletedList = mutableListOf<WorkoutExerciseWithDetails>()
        
        exercises.forEach { exercise ->
            if (completedExercises.contains(exercise.entityExercise.id)) {
                completedList.add(exercise)
            } else {
                uncompletedList.add(exercise)
            }
        }
        
        // Sort completed exercises by completion order
        completedList.sortBy { exercise ->
            completedExercisesOrder.indexOf(exercise.entityExercise.id)
        }
        
        // Combine: completed first (in completion order), then uncompleted (in original order)
        val orderedExercises = completedList + uncompletedList
        
        Log.d("WorkoutDetailsScreen", "Ordered exercises: ${completedList.size} completed + ${uncompletedList.size} uncompleted")
        
        return orderedExercises
    }

    // Function to filter out warm-up exercises from the main exercise list
    fun filterOutWarmUpExercises() {
        // Safety check: don't filter if exercise list is empty
        if (exercisesList.isEmpty()) {
            Log.d("WorkoutDetailsScreen", "Cannot filter warm-ups - exercise list is empty")
            return
        }
        
        // Only filter if there are actually warm-up exercises
        if (warmUpExercises.isEmpty()) {
            // No warm-ups, so all exercises should be shown
            reorderedExercises = orderExercisesByCompletion(exercisesList)
            Log.d("WorkoutDetailsScreen", "No warm-ups - showing all ${exercisesList.size} exercises ordered by completion")
            return
        }
        
        val filteredExercises = exercisesList.filter { workoutExerciseWithDetails ->
            // Check if this exercise is part of the warm-up (by checking if it exists in warmUpExercises)
            val isWarmUpExercise = warmUpExercises.any { warmUpExercise ->
                warmUpExercise.exerciseId == workoutExerciseWithDetails.entityExercise.id
            }
            !isWarmUpExercise // Only include non-warm-up exercises
        }
        
        // Order the filtered exercises by completion
        reorderedExercises = orderExercisesByCompletion(filteredExercises)
        Log.d("WorkoutDetailsScreen", "Filtered and ordered exercises: ${exercisesList.size} -> ${filteredExercises.size} (removed ${exercisesList.size - filteredExercises.size} warm-up exercises)")
    }

    // Reorder exercises when completion status changes
    LaunchedEffect(currentWorkout?.completedExercisesOrder, currentWorkout?.isActive) {
        if (reorderedExercises.isNotEmpty() && currentWorkout?.isActive == true) {
            reorderedExercises = orderExercisesByCompletion(reorderedExercises)
            Log.d("WorkoutDetailsScreen", "Reordered exercises due to completion status change")
        }
    }

    // Initialize reorderedExercises when exercisesList changes
    LaunchedEffect(exercisesList) {
        // Only update reorderedExercises if we're not currently dragging
        // and if this is the initial load (reorderedExercises is empty and we haven't loaded data yet)
        if (!isDragging && reorderedExercises.isEmpty() && !hasLoadedInitialData) {
            // If there are warm-ups, filter them out; otherwise show all exercises
            if (warmUpExercises.isNotEmpty()) {
                filterOutWarmUpExercises()
            } else {
                reorderedExercises = orderExercisesByCompletion(exercisesList)
                Log.d("WorkoutDetailsScreen", "Initial load - no warm-ups, showing all ${exercisesList.size} exercises ordered by completion")
            }
        }
    }
    

    
    // Update filtering when warm-up exercises change
    LaunchedEffect(warmUpExercises) {
        // Only filter if we have exercises loaded and we're not dragging
        if (exercisesList.isNotEmpty() && !isDragging) {
            if (warmUpExercises.isNotEmpty()) {
                filterOutWarmUpExercises()
            } else {
                // No warm-ups, show all exercises
                reorderedExercises = orderExercisesByCompletion(exercisesList)
                Log.d("WorkoutDetailsScreen", "No warm-ups - showing all ${exercisesList.size} exercises ordered by completion")
            }
        } else {
            Log.d("WorkoutDetailsScreen", "Skipping warm-up filtering - exercisesList empty: ${exercisesList.isEmpty()}, isDragging: $isDragging")
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

    // Function to start movement monitoring
    fun startMovementMonitoring() {
        movementCheckJob?.cancel()
        lastMovementTime = System.currentTimeMillis()

        movementCheckJob = coroutineScope.launch {
            var previousMovementTime = lastMovementTime
            Log.d("DRAG_DEBUG", "Movement monitoring started - autoScrollDirection: $autoScrollDirection, draggedItemIndex: $draggedItemIndex")
            
            while (autoScrollDirection != 0 || isWaitingForContinuation) {
                delay(100) // Check more frequently during countdown
                val currentTime = System.currentTimeMillis()
                val timeSinceLastMovement = currentTime - lastMovementTime

                if (isWaitingForContinuation) {
                    // Check if lastMovementTime has changed since last check during countdown
                    if (lastMovementTime > previousMovementTime) {
                        Log.d(
                            "DRAG_DEBUG", 
                            "Movement detected during countdown via monitoring - movement time changed from $previousMovementTime to $lastMovementTime"
                        )
                        cancelCountdown()
                    }
                    previousMovementTime = lastMovementTime
                } else if (timeSinceLastMovement > 500) {
                    // User hasn't moved for 500ms and countdown not already active
                    Log.d(
                        "DRAG_DEBUG",
                        "No movement for ${timeSinceLastMovement}ms - starting countdown"
                    )
                    startCountdown()
                    previousMovementTime = lastMovementTime
                }
            }
            Log.d("DRAG_DEBUG", "Movement monitoring stopped - autoScrollDirection: $autoScrollDirection, isWaitingForContinuation: $isWaitingForContinuation, draggedItemIndex: $draggedItemIndex")
        }
    }

    // Function to handle real-time reordering during drag
    fun handleDragMove(
        dragAmount: Offset,
        swapThreshold: Float,
        cardHeightPx: Float,
        exerciseListState: LazyListState
    ) {
        if (draggedItemIndex < 0) return // Return if no item is being dragged
        val fromIndex = draggedItemIndex

        // Only start processing after we've actually started dragging
        if (!hasStartedDragging) {
            hasStartedDragging = true
            return
        }

        // Track drag direction with threshold to avoid noise
        val previousDragDirection = dragDirection
        val directionThreshold = 0.5f // Minimum movement to change direction

        if (dragAmount.y > directionThreshold) {
            dragDirection = 1 // Dragging down
        } else if (dragAmount.y < -directionThreshold) {
            dragDirection = -1 // Dragging up
        }
        // If movement is below threshold, keep previous direction

        // Log drag direction changes
        if (previousDragDirection != dragDirection) {
            Log.d(
                "DRAG_DEBUG",
                "Drag direction changed: $previousDragDirection -> $dragDirection (dragAmount.y: ${dragAmount.y})"
            )
        }

        // Accumulate the drag amount
        dragOffset += dragAmount

        // Check if we should switch cards and trigger auto-scroll
        if (kotlin.math.abs(dragOffset.y) > swapThreshold) {
            val newIndex = when {
                dragOffset.y < -swapThreshold && fromIndex > 0 -> fromIndex - 1 // Moving up
                dragOffset.y > swapThreshold && fromIndex < reorderedExercises.size - 1 -> fromIndex + 1 // Moving down
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
                dragOffset =
                    dragOffset.copy(y = dragOffset.y - (if (newIndex > fromIndex) swapThreshold else -swapThreshold))

                // Trigger auto-scroll in the direction of the switch
                if (newIndex > fromIndex) {
                    // Moved down - start auto-scroll down
                    autoScrollDirection = 1
                    autoScrollAmount = 0f
                    startMovementMonitoring()
                    Log.d(
                        "DRAG_DEBUG",
                        "Card switched DOWN - starting auto-scroll DOWN and movement monitoring"
                    )
                } else {
                    // Moved up - start auto-scroll up
                    autoScrollDirection = -1
                    autoScrollAmount = 0f
                    startMovementMonitoring()
                    Log.d(
                        "DRAG_DEBUG",
                        "Card switched UP - starting auto-scroll UP and movement monitoring"
                    )
                }
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
                    overflow = TextOverflow.Visible,
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
                    overflow = TextOverflow.Visible,
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
                    Log.e(
                        "WorkoutDetailsScreen",
                        "Failed to load friends: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error loading friends: ${e.message}")
            }
        }
    }
    
    // Function to load warm-up templates
    fun loadWarmUpTemplates() {
        coroutineScope.launch {
            try {
                val templates = warmUpDao.getAllWarmUpTemplatesWithExercises().first()
                // Templates are loaded when dialog is shown
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error loading warm-up templates: ${e.message}")
            }
        }
    }
    
    // Function to load warm-up for current workout
    fun loadWarmUpForWorkout() {
        coroutineScope.launch {
            try {
                val workoutWarmUp = warmUpDao.getWorkoutWarmUp(workoutId)
                if (workoutWarmUp != null) {
                    // Load the warm-up template with exercises
                    val warmUpTemplate = warmUpDao.getWarmUpTemplateWithExercises(workoutWarmUp.templateId)
                    if (warmUpTemplate != null) {
                        selectedWarmUp = warmUpTemplate
                        warmUpExercises = warmUpTemplate.exercises
                        Log.d("WorkoutDetailsScreen", "Loaded warm-up from database: ${warmUpTemplate.template.name} with ${warmUpTemplate.exercises.size} exercises")
                        
                        // Filter out warm-up exercises from main exercise list only if we have exercises
                        withContext(Dispatchers.Main) {
                            if (exercisesList.isNotEmpty()) {
                                filterOutWarmUpExercises()
                            } else {
                                Log.d("WorkoutDetailsScreen", "No exercises to filter yet, warm-up will be filtered when exercises are loaded")
                            }
                        }
                    }
                } else {
                    Log.d("WorkoutDetailsScreen", "No warm-up found for workout $workoutId")
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error loading warm-up from database: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Function to select warm-up template
    fun selectWarmUpTemplate(template: WarmUpTemplateWithExercises) {
        coroutineScope.launch {
            try {
                // Save warm-up selection to database
                val workoutWarmUp = WorkoutWarmUp(
                    workoutId = workoutId,
                    templateId = template.template.id,
                    isCustomized = false,
                    customDuration = null
                )
                warmUpDao.insertWorkoutWarmUp(workoutWarmUp)
                
                // Update local state
                selectedWarmUp = template
                warmUpExercises = template.exercises
                showWarmUpDialog = false
                
                // Filter out warm-up exercises from main exercise list
                withContext(Dispatchers.Main) {
                    filterOutWarmUpExercises()
                }
                
                Log.d("WorkoutDetailsScreen", "Warm-up saved to database: ${template.template.name} with ${template.exercises.size} exercises")
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error saving warm-up to database: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Function to remove warm-up
    fun removeWarmUp() {
        coroutineScope.launch {
            try {
                // Remove warm-up from database
                warmUpDao.deleteWorkoutWarmUpByWorkout(workoutId)
                
                // Update local state
                selectedWarmUp = null
                warmUpExercises = emptyList()
                
                // Since warm-up was removed, show all exercises
                withContext(Dispatchers.Main) {
                    reorderedExercises = orderExercisesByCompletion(exercisesList)
                    Log.d("WorkoutDetailsScreen", "Warm-up removed - showing all ${exercisesList.size} exercises ordered by completion")
                }
                
                Log.d("WorkoutDetailsScreen", "Warm-up removed from database for workout $workoutId")
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error removing warm-up from database: ${e.message}")
                e.printStackTrace()
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
                        Toast.makeText(context, "Workout shared successfully!", Toast.LENGTH_SHORT)
                            .show()
                        showShareDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            response?.message ?: "Failed to share workout",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Failed to share workout: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("WorkoutDetailsScreen", "Error sharing workout: ${e.message}")
                Toast.makeText(context, "Error sharing workout: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isSharing = false
            }
        }
    }

    // Add LaunchedEffect to sync workoutStarted with CurrentWorkoutViewModel state
    LaunchedEffect(currentWorkout) {
        workoutStarted = currentWorkout?.isActive ?: false
        startTimeWorkout = currentWorkout?.startTime ?: 0L
        Log.d(
            "WorkoutDetailsScreen",
            "Synced workoutStarted state: $workoutStarted, currentWorkout isActive: ${currentWorkout?.isActive}, startTime: ${currentWorkout?.startTime}"
        )

        // Start break timer if there's an active workout session and break timer is not already active
        if (currentWorkout?.isActive == true && !isBreakActive) {
            Log.d("WorkoutDetailsScreen", "Active workout detected - starting break timer")
            viewModel.startBreakTimer()
        }
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

    // Load friends when share dialog is shown
    LaunchedEffect(showShareDialog) {
        if (showShareDialog && friends.isEmpty()) {
            loadFriends()
        }
    }

    // Fetch workout data and sync with ViewModel
    LaunchedEffect(workoutId) {
        // Reset warm-up state for new workout
        selectedWarmUp = null
        warmUpExercises = emptyList()
        try {
            // Load warm-up data for this workout
            loadWarmUpForWorkout()
            
            Log.d("WorkoutDetailsScreen", "Loading workout data for ID: $workoutId")

            // Reset flags for new workout
            hasLoadedInitialData = false
            reorderedExercises = emptyList()

            // Clear ViewModel state at the beginning to ensure clean state for each workout
            viewModel.clearExercises()

            // Check if we have an active workout for this workoutId
            val hasActiveWorkout =
                currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
            val hasCompletedExercises = currentWorkout?.completedExercises?.isNotEmpty() == true
            val hasInitializedWorkout =
                currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == false

            Log.d(
                "WorkoutDetailsScreen",
                "Current workout state: workoutId=${currentWorkout?.workoutId}, isActive=${currentWorkout?.isActive}, completedExercises=${currentWorkout?.completedExercises}"
            )
            Log.d(
                "WorkoutDetailsScreen",
                "hasActiveWorkout=$hasActiveWorkout, hasCompletedExercises=$hasCompletedExercises, hasInitializedWorkout=$hasInitializedWorkout"
            )

            if (hasActiveWorkout || hasCompletedExercises) {
                Log.d(
                    "WorkoutDetailsScreen",
                    "Resuming existing workout - preserving completed exercises"
                )
                viewModel.resetWorkoutSession()
            } else if (hasInitializedWorkout) {
                Log.d("WorkoutDetailsScreen", "Using existing initialized workout")
                viewModel.resetWorkoutSession()
            } else {
                Log.d(
                    "WorkoutDetailsScreen",
                    "Starting fresh workout - clearing completed exercises"
                )
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
                    Log.d(
                        "WorkoutDetailsScreen",
                        "Workout data loaded: ${exercisesData.size} exercises"
                    )

                    // Set workout name regardless of whether there are exercises or not
                    workoutName = workout?.name ?: "Unknown Workout"


                    if (exercisesData.isEmpty()) {
                        Log.e(
                            "WorkoutDetailsScreen",
                            "No exercises found for workout ID: $workoutId"
                        )
                        isLoading = false
                    } else {
                        // Convert to WorkoutExerciseWithDetails and sync with ViewModel
                        val workoutExerciseWithDetails = exercisesData.map {
                            WorkoutExerciseWithDetails(it.workoutExercise, it.exercise)
                        }
                        // Initialize reorderedExercises with the fetched data, ordered by completion
                        reorderedExercises = orderExercisesByCompletion(workoutExerciseWithDetails)
                        hasLoadedInitialData = true
                        workoutExerciseWithDetails.forEach { exerciseWithDetails ->
                            viewModel.addExercise(exerciseWithDetails)
                        }
                        
                        // Filter out warm-up exercises from the main exercise list if there are any
                        if (warmUpExercises.isNotEmpty()) {
                            filterOutWarmUpExercises()
                        }

                        // Check if we have an active workout in GeneralViewModel
                        val hasActiveWorkout =
                            currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
                        val hasInitializedWorkout =
                            currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == false

                        if (hasActiveWorkout) {
                            Log.d(
                                "WorkoutDetailsScreen",
                                "Using existing active workout session from GeneralViewModel"
                            )
                            workoutStarted = currentWorkout?.isActive ?: false
                            if (workoutStarted) {
                                startTimeWorkout =
                                    currentWorkout?.startTime ?: System.currentTimeMillis()
                            }
                        } else if (hasInitializedWorkout) {
                            Log.d(
                                "WorkoutDetailsScreen",
                                "Using existing initialized workout session from GeneralViewModel"
                            )
                            workoutStarted = false // Not started yet
                        } else {
                            Log.d("WorkoutDetailsScreen", "Initializing new workout session")
                            viewModel.initializeWorkoutSession(workoutId, workoutName)
                            // Only initialize a new workout session if there's no active workout
                            val hasAnyWorkout = currentWorkout != null
                            val hasActiveWorkout = hasAnyWorkout && currentWorkout?.isActive == true
                            val isDifferentWorkout =
                                hasAnyWorkout && currentWorkout?.workoutId != workoutId

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
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Int?>(
            "newExerciseId",
            null
        )
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
                                reorderedExercises = orderExercisesByCompletion(workoutExerciseWithDetails)
                                viewModel.updateExercisesOrder(reorderedExercises)
                                
                                // Preserve warm-up state when exercises are added
                                // Reload warm-up to ensure state is preserved
                                loadWarmUpForWorkout()
                            }
                        }
                    }
                    // Clear the flag
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "newExerciseId",
                        null
                    )
                }
            }
    }

    // Function to handle left swipe for exercise alternatives
    fun handleExerciseSwipe(exerciseWithDetails: WorkoutExerciseWithDetails) {
        selectedWorkoutExercise = exerciseWithDetails
        coroutineScope.launch {
            // Load existing alternatives
            alternatives = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
            
            // Load similar exercises for suggestions based on muscle group and overlapping muscle parts
            similarExercises = dao.getSimilarExercisesWithParsedParts(
                exerciseWithDetails.entityExercise.muscle,
                exerciseWithDetails.entityExercise.parts,
                exerciseWithDetails.entityExercise.id
            )
            
            showAlternativesDialog = true
        }
    }
    
    // Function to add an alternative exercise
    fun addAlternativeExercise(alternativeExercise: EntityExercise) {
        selectedWorkoutExercise?.let { workoutExercise ->
            coroutineScope.launch {
                val newAlternative = ExerciseAlternative(
                    originalExerciseId = workoutExercise.entityExercise.id,
                    alternativeExerciseId = alternativeExercise.id,
                    workoutExerciseId = workoutExercise.workoutExercise.id,
                    order = alternatives.size,
                    isActive = false
                )
                
                val alternativeId = dao.insertExerciseAlternative(newAlternative)
                
                // Update the hasAlternatives flag
                dao.updateWorkoutExerciseHasAlternatives(workoutExercise.workoutExercise.id, true)
                
                // Refresh alternatives list
                alternatives = dao.getExerciseAlternatives(workoutExercise.workoutExercise.id)
                
                // Refresh exercise alternatives map
                val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
                reorderedExercises.forEach { exerciseWithDetails ->
                    val alternatives = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
                    
                    // Build the full list: original exercise + all alternatives
                    val allExercises = mutableListOf<EntityExercise>()
                    
                    // Add the original exercise if there are any alternatives
                    if (alternatives.isNotEmpty()) {
                        val originalExerciseId = alternatives.firstOrNull()?.originalExerciseId
                        if (originalExerciseId != null) {
                            try {
                                val originalExercise = dao.getExerciseById(originalExerciseId)
                                if (originalExercise != null && originalExercise.id != exerciseWithDetails.entityExercise.id) {
                                    allExercises.add(originalExercise)
                                }
                            } catch (e: Exception) {
                                println("CAROUSEL: Failed to load original exercise $originalExerciseId: ${e.message}")
                            }
                        }
                    }
                    
                    // Add all alternative exercises
                    val alternativeExercises = alternatives.mapNotNull { alt ->
                        try {
                            dao.getExerciseById(alt.alternativeExerciseId)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    allExercises.addAll(alternativeExercises)
                    
                    alternativesMap[exerciseWithDetails.workoutExercise.id] = allExercises
                }
                exerciseAlternatives = alternativesMap
            }
        }
    }
    
    // Function to activate an alternative (replace current exercise)
    fun activateAlternative(alternative: ExerciseAlternative) {
        selectedWorkoutExercise?.let { workoutExercise ->
            coroutineScope.launch {
                // Deactivate all alternatives for this workout exercise
                dao.deactivateAllAlternatives(workoutExercise.workoutExercise.id)
                
                // Activate the selected alternative
                dao.activateAlternative(alternative.id)
                
                // Update the workout exercise to use the alternative exercise
                dao.updateWorkoutExerciseId(workoutExercise.workoutExercise.id, alternative.alternativeExerciseId)
                
                // Refresh the exercise list
                val updatedWorkoutExercises = dao.getWorkoutExercisesForWorkout(workoutId)
                val updatedExercises = updatedWorkoutExercises.map { workoutExercise ->
                    val exercise = dao.getExerciseById(workoutExercise.exerciseId)
                    WorkoutExerciseWithDetails(workoutExercise, exercise!!)
                }
                reorderedExercises = updatedExercises
                viewModel.updateExercisesOrder(updatedExercises)
                
                // Refresh exercise alternatives map
                val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
                reorderedExercises.forEach { exerciseWithDetails ->
                    val alternatives = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
                    
                    // Build the full list: original exercise + all alternatives
                    val allExercises = mutableListOf<EntityExercise>()
                    
                    // Add the original exercise if there are any alternatives
                    if (alternatives.isNotEmpty()) {
                        val originalExerciseId = alternatives.firstOrNull()?.originalExerciseId
                        if (originalExerciseId != null) {
                            try {
                                val originalExercise = dao.getExerciseById(originalExerciseId)
                                if (originalExercise != null && originalExercise.id != exerciseWithDetails.entityExercise.id) {
                                    allExercises.add(originalExercise)
                                }
                            } catch (e: Exception) {
                                println("CAROUSEL: Failed to load original exercise $originalExerciseId: ${e.message}")
                            }
                        }
                    }
                    
                    // Add all alternative exercises
                    val alternativeExercises = alternatives.mapNotNull { alt ->
                        try {
                            dao.getExerciseById(alt.alternativeExerciseId)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    allExercises.addAll(alternativeExercises)
                    
                    alternativesMap[exerciseWithDetails.workoutExercise.id] = allExercises
                }
                exerciseAlternatives = alternativesMap
                
                // Close the dialog
                showAlternativesDialog = false
                selectedWorkoutExercise = null
            }
        }
    }
    
    // Function to remove an alternative
    fun removeAlternative(alternative: ExerciseAlternative) {
        coroutineScope.launch {
            dao.deleteExerciseAlternative(alternative)
            
            // Refresh alternatives list
            selectedWorkoutExercise?.let { workoutExercise ->
                alternatives = dao.getExerciseAlternatives(workoutExercise.workoutExercise.id)
                
                // If no alternatives left, update the flag
                if (alternatives.isEmpty()) {
                    dao.updateWorkoutExerciseHasAlternatives(workoutExercise.workoutExercise.id, false)
                }
            }
        }
    }
    
    // Function to remove an alternative exercise with special handling for original exercise
    suspend fun removeAlternativeExercise(workoutExerciseId: Int, exerciseIdToRemove: Int) {
        println("CAROUSEL: removeAlternativeExercise called - workoutExerciseId=$workoutExerciseId, exerciseIdToRemove=$exerciseIdToRemove")
        
        // Get all alternatives for this workout exercise
        val alternativesList = dao.getExerciseAlternatives(workoutExerciseId)
        
        if (alternativesList.isEmpty()) {
            println("CAROUSEL: No alternatives found")
            return
        }
        
        // Get the original exercise ID
        val originalExerciseId = alternativesList.firstOrNull()?.originalExerciseId
        println("CAROUSEL: Original exercise ID: $originalExerciseId")
        
        // Check if we're removing the original exercise
        val isRemovingOriginal = exerciseIdToRemove == originalExerciseId
        println("CAROUSEL: Is removing original: $isRemovingOriginal")
        
        if (isRemovingOriginal) {
            // Special case: removing the original exercise
            // Make the first alternative the new original
            
            if (alternativesList.isEmpty()) {
                println("CAROUSEL: ERROR - Cannot remove original when there are no alternatives")
                return
            }
            
            // Get the first alternative to become the new original
            val newOriginalId = alternativesList.firstOrNull()?.alternativeExerciseId
            if (newOriginalId == null) {
                println("CAROUSEL: ERROR - No alternative found to become new original")
                return
            }
            
            println("CAROUSEL: Making exercise $newOriginalId the new original")
            
            // Update all alternatives to have the new original ID
            alternativesList.forEach { alt ->
                if (alt.alternativeExerciseId != newOriginalId) {
                    // Update this alternative to point to the new original
                    val updatedAlt = alt.copy(originalExerciseId = newOriginalId)
                    dao.updateExerciseAlternative(updatedAlt)
                }
            }
            
            // Remove the alternative record for the new original (since it's now the original, not an alternative)
            val newOriginalAltRecord = alternativesList.find { it.alternativeExerciseId == newOriginalId }
            if (newOriginalAltRecord != null) {
                dao.deleteExerciseAlternative(newOriginalAltRecord)
            }
            
            // Update the workout exercise to use the new original
            dao.updateWorkoutExerciseId(workoutExerciseId, newOriginalId)
            println("CAROUSEL: Updated workout exercise to use new original: $newOriginalId")
            
        } else {
            // Normal case: removing a non-original alternative
            println("CAROUSEL: Removing alternative exercise: $exerciseIdToRemove")
            
            // Find and delete the alternative record
            val altToDelete = alternativesList.find { it.alternativeExerciseId == exerciseIdToRemove }
            if (altToDelete != null) {
                dao.deleteExerciseAlternative(altToDelete)
                println("CAROUSEL: Deleted alternative record")
            } else {
                println("CAROUSEL: WARNING - Alternative record not found for exercise $exerciseIdToRemove")
            }
        }
        
        // Check if there are any alternatives left
        val remainingAlternatives = dao.getExerciseAlternatives(workoutExerciseId)
        if (remainingAlternatives.isEmpty()) {
            println("CAROUSEL: No alternatives remaining, updating hasAlternatives flag")
            dao.updateWorkoutExerciseHasAlternatives(workoutExerciseId, false)
        }
        
        // Refresh the exercise list
        val updatedWorkoutExercises = dao.getWorkoutExercisesForWorkout(workoutId)
        val updatedExercises = updatedWorkoutExercises.map { workoutExercise ->
            val exercise = dao.getExerciseById(workoutExercise.exerciseId)
            WorkoutExerciseWithDetails(workoutExercise, exercise!!)
        }
        reorderedExercises = updatedExercises
        viewModel.updateExercisesOrder(updatedExercises)
        
        // Refresh exercise alternatives map
        val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
        reorderedExercises.forEach { exerciseWithDetails ->
            val alts = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
            
            // Build the full list: original exercise + all alternatives
            val allExercises = mutableListOf<EntityExercise>()
            
            // Add the original exercise if there are any alternatives
            if (alts.isNotEmpty()) {
                val origExerciseId = alts.firstOrNull()?.originalExerciseId
                if (origExerciseId != null) {
                    try {
                        val originalExercise = dao.getExerciseById(origExerciseId)
                        if (originalExercise != null && originalExercise.id != exerciseWithDetails.entityExercise.id) {
                            allExercises.add(originalExercise)
                        }
                    } catch (e: Exception) {
                        println("CAROUSEL: Failed to load original exercise $origExerciseId: ${e.message}")
                    }
                }
            }
            
            // Add all alternative exercises
            val alternativeExercises = alts.mapNotNull { alt ->
                try {
                    dao.getExerciseById(alt.alternativeExerciseId)
                } catch (e: Exception) {
                    null
                }
            }
            allExercises.addAll(alternativeExercises)
            
            alternativesMap[exerciseWithDetails.workoutExercise.id] = allExercises
        }
        exerciseAlternatives = alternativesMap
        
        println("CAROUSEL: Alternative removal completed successfully")
    }

    // Function to check if exercise is completed and show dialog
    fun checkCompletedExerciseAndShowDialog(exerciseWithDetails: WorkoutExerciseWithDetails) {
        val isActiveWorkout = currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
        val isCompleted = if (isActiveWorkout) {
            generalViewModel.isExerciseCompleted(exerciseWithDetails.entityExercise.id)
        } else {
            false
        }
        
        if (isCompleted) {
            completedExerciseInfo = exerciseWithDetails
            showCompletedExerciseDialog = true
        } else {
            // Navigate to exercise screen normally
            // Use the current workout's session ID to ensure consistency
            val sessionId = currentWorkout?.sessionId ?: System.currentTimeMillis()
            Log.d("WorkoutDetailsScreen", "Navigating to exercise with consistent session ID: $sessionId")
            navController.navigate(
                Screen.Exercise.createRoute(
                    exerciseWithDetails.entityExercise.id,
                    sessionId,
                    workoutId
                )
            )
        }
    }

    // Function to fetch completed exercise session data
    suspend fun getCompletedExerciseSessionData(exerciseId: Int): SessionEntityExercise? {
        return try {
            Log.d("WorkoutDetailsScreen", "Fetching session data for exercise $exerciseId")
            
            withContext(Dispatchers.IO) {
                // Use the foreign key relationship: find exercise session by exerciseId and sessionId
                val currentSessionId = currentWorkout?.sessionId
                if (currentSessionId != null) {
                    Log.d("WorkoutDetailsScreen", "Using consistent session ID: $currentSessionId")
                    val exerciseSessions = dao.getExerciseSessionsForSession(currentSessionId)
                    Log.d("WorkoutDetailsScreen", "Found ${exerciseSessions.size} exercise sessions for session $currentSessionId")
                    
                    val matchingSession = exerciseSessions.find { it.exerciseId.toInt() == exerciseId }
                    if (matchingSession != null) {
                        Log.d("WorkoutDetailsScreen", "Found matching session: exerciseId=${matchingSession.exerciseId}, completedSets=${matchingSession.completedSets}, weights=${matchingSession.weight}, reps=${matchingSession.repsOrTime}")
                        return@withContext matchingSession
                    } else {
                        Log.d("WorkoutDetailsScreen", "No matching session found for exercise $exerciseId in session $currentSessionId")
                    }
                } else {
                    Log.d("WorkoutDetailsScreen", "No current session ID available")
                }
                
                Log.d("WorkoutDetailsScreen", "No matching session found for exercise $exerciseId")
                null
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Error fetching exercise session data: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Function to vibrate on value change
    fun vibrateOnValueChange() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    // Function to vibrate only when value actually changes
    fun vibrateOnValueChangeIfDifferent(oldValue: Float, newValue: Float) {
        if (oldValue != newValue) {
            vibrateOnValueChange()
        }
    }

    // Function to save exercise session with muscle soreness data
    suspend fun saveExerciseSessionWithSoreness(exerciseWithDetails: WorkoutExerciseWithDetails) {
        Log.d("WorkoutDetailsScreen", "saveExerciseSessionWithSoreness called")
        val exercise = exerciseWithDetails.entityExercise
        val workoutExercise = exerciseWithDetails.workoutExercise
        
        try {
            withContext(Dispatchers.IO) {
                // Check if session data already exists for this exercise
                val existingSessionData = getCompletedExerciseSessionData(exercise.id)
                
                if (existingSessionData != null) {
                    // Update existing session with new soreness factors
                    val updatedSession = existingSessionData.copy(
                        eccentricFactor = eccentricFactor,
                        noveltyFactor = noveltyFactor,
                        adaptationLevel = adaptationLevel,
                        rpe = rpe,
                        subjectiveSoreness = subjectiveSoreness
                    )
                    dao.updateExerciseSession(updatedSession)
                    Log.d("WorkoutDetailsScreen", "Updated existing exercise session with new soreness data")
                } else {
                    // Create new session with soreness factors
                    val exerciseSession = SessionEntityExercise(
                        sessionId = currentWorkout?.sessionId ?: System.currentTimeMillis(),
                        exerciseId = exercise.id.toLong(),
                        sets = workoutExercise.sets,
                        repsOrTime = List(workoutExercise.sets) { workoutExercise.reps },
                        weight = List(workoutExercise.sets) { workoutExercise.weight },
                        muscleGroup = exercise.muscle,
                        muscleParts = exercise.parts,
                        completedSets = workoutExercise.sets,
                        notes = "",
                        eccentricFactor = eccentricFactor, // Individual soreness factor for this exercise
                        noveltyFactor = noveltyFactor, // Individual soreness factor for this exercise
                        adaptationLevel = adaptationLevel, // Individual soreness factor for this exercise
                        rpe = rpe, // Individual soreness factor for this exercise
                        subjectiveSoreness = subjectiveSoreness // Individual soreness factor for this exercise
                    )
                    dao.insertExerciseSession(exerciseSession)
                    Log.d("WorkoutDetailsScreen", "Created new exercise session with soreness data")
                }
            }
            
            // Mark exercise as completed in GeneralViewModel
            Log.d("WorkoutDetailsScreen", "Marking exercise ${exercise.id} (${exercise.name}) as completed")
            generalViewModel.markExerciseAsCompleted(exercise.id)
            
            // Show success notification
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exercise saved with soreness data", Toast.LENGTH_SHORT).show()
                showCompletedExerciseDialog = false
                completedExerciseInfo = null
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Error saving exercise session: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving exercise", Toast.LENGTH_SHORT).show()
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
                            dao.updateWorkoutExerciseOrder(
                                exerciseWithDetails.workoutExercise.id,
                                i
                            )
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

                    Log.d(
                        "WorkoutDetailsScreen",
                        "Saving workout session - Start: ${currentWorkoutState.startTime}, End: $endTime, Duration: $duration"
                    )
                    Log.d(
                        "WorkoutDetailsScreen",
                        "Session details - ID: ${currentWorkoutState.sessionId}, Workout ID: ${currentWorkoutState.workoutId}, Name: ${currentWorkoutState.workoutName}"
                    )

                    val sessionWorkout = SessionWorkoutEntity(
                        sessionId = currentWorkoutState.sessionId,
                        workoutId = currentWorkoutState.workoutId,
                        workoutName = currentWorkoutState.workoutName,
                        startTime = currentWorkoutState.startTime,
                        endTime = endTime
                    )

                    Log.d(
                        "WorkoutDetailsScreen",
                        "Saving session: ID=${sessionWorkout.sessionId}, Name='${sessionWorkout.workoutName}', Start=${sessionWorkout.startTime}, End=${sessionWorkout.endTime}, Duration=${(sessionWorkout.endTime - sessionWorkout.startTime) / (60 * 1000)} min"
                    )

                    // Check if session already exists
                    val existingSession = dao.getWorkoutSession(sessionWorkout.sessionId)
                    if (existingSession != null) {
                        Log.d(
                            "WorkoutDetailsScreen",
                            "Updating existing session: ${sessionWorkout.sessionId}"
                        )
                        dao.updateWorkoutSession(sessionWorkout)
                    } else {
                        Log.d(
                            "WorkoutDetailsScreen",
                            "Inserting new session: ${sessionWorkout.sessionId}"
                        )
                        dao.insertWorkoutSession(sessionWorkout)
                    }

                    // Verify the session was saved
                    val savedSession = dao.getWorkoutSession(sessionWorkout.sessionId)
                    Log.d(
                        "WorkoutDetailsScreen",
                        "Verification - Saved session: ${savedSession?.workoutName}, Duration: ${savedSession?.let { (it.endTime - it.startTime) / (60 * 1000) }} min"
                    )

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
                    val newlyUnlockedAchievementsForFeed =
                        achievementManager.getNewlyUnlockedAchievementsForFeed()

                    // Convert achievement IDs to WorkoutAchievementData objects
                    val achievementDataList =
                        newlyUnlockedAchievementsForFeed.map { achievementId ->
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
                        val completedExercises =
                            dao.getExerciseSessionsForSession(sessionWorkout.sessionId)
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

                        // Award XP for workout completion
                        val xpSystem = XPSystem(db.userXPDao())
                        val durationMinutes = (duration / (1000 * 60)).toInt()
                        val workoutXP = xpSystem.calculateWorkoutXP(durationMinutes, totalSets)
                        
                        // Use a default user ID for now (you can integrate with auth system later)
                        val userId = "current_user"
                        
                        // Get current user XP to calculate level changes
                        val currentUserXP = xpSystem.getUserXP(userId)
                        val previousTotalXP = currentUserXP?.totalXP ?: 0
                        val previousLevel = currentUserXP?.currentLevel ?: 1
                        
                        // Award XP to database
                        val xpAwarded = xpSystem.awardXP(
                            userId = userId,
                            xpAmount = workoutXP,
                            source = "workout_completion",
                            sourceId = sessionWorkout.sessionId.toString(),
                            description = "Completed workout: ${sessionWorkout.workoutName} (${durationMinutes}min, ${totalSets} sets)"
                        )
                        
                        if (xpAwarded) {
                            Log.d("WorkoutDetailsScreen", "Awarded $workoutXP XP for workout completion")
                            
                            // Get updated user XP to calculate new level
                            val updatedUserXP = xpSystem.getUserXP(userId)
                            val newTotalXP = updatedUserXP?.totalXP ?: previousTotalXP
                            val newLevel = updatedUserXP?.currentLevel ?: previousLevel
                            
                            // Always show XP gain dialog, regardless of level increase
                            Log.d("WorkoutDetailsScreen", "XP gained: $workoutXP (Level: $previousLevel -> $newLevel)")
                            
                            // Store in XP buffer for level-up dialog
                            val xpBuffer = XPBuffer(
                                xpGained = workoutXP,
                                previousLevel = previousLevel,
                                newLevel = newLevel,
                                previousTotalXP = previousTotalXP,
                                newTotalXP = newTotalXP,
                                timestamp = System.currentTimeMillis()
                            )
                            generalViewModel.setXPBuffer(xpBuffer)
                        } else {
                            Log.e("WorkoutDetailsScreen", "Failed to award XP for workout completion")
                        }

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
                                Log.d(
                                    "WorkoutDetailsScreen",
                                    "Workout shared to feed: ${completionResponse.shared}"
                                )
                                // Mark achievements as posted to feed to prevent duplicates in future posts
                                if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                                    achievementManager.markAchievementsAsPostedToFeed(
                                        newlyUnlockedAchievementsForFeed
                                    )
                                    Log.d(
                                        "WorkoutDetailsScreen",
                                        "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed"
                                    )
                                }
                            },
                            onFailure = { exception ->
                                Log.w(
                                    "WorkoutDetailsScreen",
                                    "Failed to share workout to feed: ${exception.message}"
                                )
                                // Even if sharing fails, mark achievements as posted to prevent duplicates
                                // The achievements are still properly unlocked in the database
                                if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                                    achievementManager.markAchievementsAsPostedToFeed(
                                        newlyUnlockedAchievementsForFeed
                                    )
                                    Log.d(
                                        "WorkoutDetailsScreen",
                                        "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed despite failure"
                                    )
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("WorkoutDetailsScreen", "Error sharing workout to feed: ${e.message}")
                        // Even if there's an error, mark achievements as posted to prevent duplicates
                        if (newlyUnlockedAchievementsForFeed.isNotEmpty()) {
                            achievementManager.markAchievementsAsPostedToFeed(
                                newlyUnlockedAchievementsForFeed
                            )
                            Log.d(
                                "WorkoutDetailsScreen",
                                "Marked ${newlyUnlockedAchievementsForFeed.size} achievements as posted to feed despite error"
                            )
                        }
                        // Don't fail the workout completion if sharing fails
                    }
                    
                    // Handle soreness assessment scheduling (TESTING MODE: 1-2 minute delays)
                    try {
                        val sorenessService = SorenessAssessmentService(context)
                        sorenessService.handleWorkoutCompletion(sessionWorkout.sessionId, exercisesList)
                        Log.d("WorkoutDetailsScreen", "Scheduled soreness assessment for session ${sessionWorkout.sessionId} (TESTING: 1-2 min delays)")
                    } catch (e: Exception) {
                        Log.e("WorkoutDetailsScreen", "Error scheduling soreness assessment: ${e.message}")
                        // Don't fail workout completion if soreness scheduling fails
                    }
                }

                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    // Stop the break timer when workout ends
                    viewModel.stopBreakTimer()
                    
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
        // Stop break timer when navigating back
        viewModel.stopBreakTimer()
        navController.popBackStack()
    }

    // Update the Recovery Factor Dialog
    if (showRecoveryDialog) {
        val context = LocalContext.current

        fun vibrateOnValueChange() {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        20,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
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
                            onValueChange = { newValue ->
                                val oldValue = recoveryFactors.proteinIntake.toFloat()
                                vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                viewModel.updateRecoveryFactors(proteinIntake = newValue.toInt())
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
                            onValueChange = { newValue ->
                                val oldValue = when {
                                    currentRecoveryFactor.contains("Sleep") -> recoveryFactors.sleepQuality
                                    currentRecoveryFactor.contains("Hydration") -> recoveryFactors.hydration
                                    currentRecoveryFactor.contains("Stress") -> recoveryFactors.stressLevel
                                    else -> 5
                                }.toFloat()
                                vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                when {
                                    currentRecoveryFactor.contains("Sleep") ->
                                        viewModel.updateRecoveryFactors(sleepQuality = newValue.toInt())

                                    currentRecoveryFactor.contains("Hydration") ->
                                        viewModel.updateRecoveryFactors(hydration = newValue.toInt())

                                    currentRecoveryFactor.contains("Stress") ->
                                        viewModel.updateRecoveryFactors(stressLevel = newValue.toInt())
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

    @Composable
    fun BreakTimerDisplay() {
        var breakText by remember { mutableStateOf("00:00") }

        LaunchedEffect(isBreakActive, breakStartTime) {
            Log.d(
                "BreakTimerDisplay",
                "LaunchedEffect triggered - isBreakActive: $isBreakActive, breakStartTime: $breakStartTime"
            )
            while (isBreakActive && breakStartTime > 0) {
                breakText = viewModel.calculateBreakDuration()
                Log.d("BreakTimerDisplay", "Break timer running - breakText: $breakText")
                delay(1000) // Update every second
            }
        }

        Log.d(
            "BreakTimerDisplay",
            "Rendering - isBreakActive: $isBreakActive, breakText: $breakText"
        )
        if (isBreakActive) {
            Text(
                text = "Break: $breakText",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

    // Function to update movement time
    fun recordMovement() {
        lastMovementTime = System.currentTimeMillis()
        if (isWaitingForContinuation) {
            Log.d("DRAG_DEBUG", "Movement detected during countdown - cancelling countdown (auto-scroll will resume)")
            cancelCountdown() // This already increments autoScrollRestartTrigger
        }
    }


    // Function to display exercise card content
    @Composable
    fun ExerciseCardContent(
        exercise: EntityExercise,
        workoutExercise: WorkoutExercise,
        index: Int,
        context: Context,
        currentWorkout: CurrentWorkoutState?,
        swapThreshold: Float,
        cardHeightPx: Float,
        exerciseListState: LazyListState,
        isSwipeInProgress: Boolean = false,
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
            targetValue = if (isBeingDragged) 12f else 2f,
            animationSpec = tween(200),
            label = "elevation"
        )

        // Shining effect animation
        val infiniteTransition = rememberInfiniteTransition(label = "shining")
        val shineOffset by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 400f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shine"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .scale(scale)
                .zIndex(if (isBeingDragged) 1000f else 0f)
                .background(
                    color = if (isBeingDragged) Color(0xFF2D1856).copy(alpha = 0.05f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    var isLongPressDetected = false
                    var dragStartTime = 0L

                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            isDragging = true
                            isFingerDown = true
                            isLongPressDetected = true
                            dragStartTime = System.currentTimeMillis()
                            draggedItem = WorkoutExerciseWithDetails(workoutExercise, exercise)
                            draggedItemIndex = index
                            dragOffset = offset
                            hasStartedDragging = false
                            autoScrollDirection = 0
                            autoScrollAmount = 0f
                            dragDirection = 0
                            isWaitingForContinuation = false // Cancel any ongoing countdown
                            deselectionTimeLeft = 0f
                            lastMovementTime = System.currentTimeMillis()
                            onDragStart(
                                WorkoutExerciseWithDetails(workoutExercise, exercise),
                                index
                            )
                            // Vibrate when card gets selected (changes color)
                            vibrateOnValueChange()
                            Log.d("DRAG_DEBUG", "Long press detected - starting drag")
                        },
                        onDragEnd = {
                            isDragging = false
                            // Reset only drag-related state
                            isDragging = false
                            draggedItem = null
                            dragOffset = Offset.Zero
                            hasStartedDragging = false
                            dragDirection = 0

                            Log.d(
                                "DRAG_DEBUG",
                                "Drag gesture ended - movement monitoring will handle countdown"
                            )
                        },
                        onDragCancel = {
                            isDragging = false
                            draggedItem = null
                            dragOffset = Offset.Zero
                            hasStartedDragging = false
                            dragDirection = 0

                            onDragEnd()
                            Log.d(
                                "DRAG_DEBUG",
                                "Drag gesture cancelled - movement monitoring will handle countdown"
                            )
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            // Record movement activity
                            recordMovement()

                            handleDragMove(
                                dragAmount,
                                swapThreshold,
                                cardHeightPx,
                                exerciseListState
                            )
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Separate pointer input to detect finger lift
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                    // Reset countdown when finger presses down again
                                    if (isWaitingForContinuation) {
                                        isWaitingForContinuation = false
                                        deselectionTimeLeft = 0f
                                        Log.d("DRAG_DEBUG", "Finger pressed - cancelling countdown")
                                    }
                                }

                                androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                    // Detect movement during countdown regardless of isFingerDown state
                                    if (isWaitingForContinuation) {
                                        Log.d("DRAG_DEBUG", "Movement detected via pointer input during countdown - cancelling countdown")
                                        lastMovementTime = System.currentTimeMillis()
                                        recordMovement()
                                    }
                                }

                                androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                    if (isFingerDown) {
                                        isFingerDown = false
                                        autoScrollDirection = 0
                                        autoScrollAmount = 0f
                                        
                                        // Cancel countdown if it's running
                                        if (isWaitingForContinuation) {
                                            countdownJob?.cancel()
                                            countdownJob = null
                                            Log.d("DRAG_DEBUG", "Finger lifted - cancelling countdown")
                                        }
                                        
                                        isWaitingForContinuation = false
                                        deselectionTimeLeft = 0f
                                        draggedItem = null
                                        draggedItemIndex = -1
                                        dragOffset = Offset.Zero
                                        hasStartedDragging = false
                                        dragDirection = 0
                                        Log.d(
                                            "DRAG_DEBUG",
                                            "Finger actually lifted - stopping auto-scroll and resetting all"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                .clickable(enabled = !isDragging) {
                    // Check if there's an active workout from a different workout
                    val hasActiveWorkoutFromDifferentWorkout =
                        currentWorkout?.isActive == true && currentWorkout?.workoutId != workoutId

                    if (hasActiveWorkoutFromDifferentWorkout) {
                        // Show toast message and block navigation
                        Toast.makeText(
                            context,
                            "Please finish your active workout first",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Check if exercise is completed and show dialog if needed
                        checkCompletedExerciseAndShowDialog(WorkoutExerciseWithDetails(workoutExercise, exercise))
                    }
                },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBeingDragged) Color(0xFF2D1856) else MaterialTheme.colorScheme.surface
            )
        ) {
            Box {
                // Shining effect overlay
                if (isBeingDragged) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = shineOffset
                            }
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.32f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(200f, 0f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }

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
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sets: ${workoutExercise.sets}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (exercise.useTime) {
                                val timeInSeconds = workoutExercise.reps
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

                // Delete button (top right) - hide during swipe
                if (!isSwipeInProgress) {
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
                }

                // Drag handle (middle right) - hide during swipe
                if (!isSwipeInProgress) {
                    Icon(
                        painter = painterResource(id = R.drawable.drag_handle_icon),
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(24.dp)
                    )
                }

                // Completion indicator moved to bottom right
                // Only show completed exercises when we're viewing the active workout
                val isActiveWorkout =
                    currentWorkout?.workoutId == workoutId && currentWorkout.isActive == true
                val isCompleted = if (isActiveWorkout) {
                    generalViewModel.isExerciseCompleted(exercise.id)
                } else {
                    false
                }
                Log.d(
                    "WorkoutDetailsScreen",
                    "Exercise ${exercise.id} (${exercise.name}) completed: $isCompleted, isActiveWorkout: $isActiveWorkout"
                )
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

    val exerciseListState = rememberLazyListState()

    // Card-switching based auto-scroll logic:
    val cardHeightPx = with(LocalDensity.current) { 136.dp.toPx() }
    val swapThreshold =
        cardHeightPx * 0.3f // Reduced from cardHeightPx - 15 to 30% of card height (~40px)

    // Auto-scroll execution based on card switching
    LaunchedEffect(autoScrollDirection, isWaitingForContinuation, autoScrollRestartTrigger, isFingerDown) {
        Log.d("DRAG_DEBUG", "LaunchedEffect triggered - autoScrollDirection: $autoScrollDirection, isFingerDown: $isFingerDown, isWaitingForContinuation: $isWaitingForContinuation, restartTrigger: $autoScrollRestartTrigger")
        
        if (autoScrollDirection != 0 && isFingerDown && !isWaitingForContinuation) {
            val scrollSpeed = 8f // pixels per frame
            val scrollThreshold = cardHeightPx * 1.0f // Switch when scrolled 100% of card height

            Log.d("DRAG_DEBUG", "Auto-scroll started/resumed - direction: $autoScrollDirection, countdown active: $isWaitingForContinuation")

            while (autoScrollDirection != 0 && isFingerDown && !isWaitingForContinuation) {
                // Stop if finger is lifted or countdown starts
                if (!isFingerDown) {
                    Log.d("DRAG_DEBUG", "Finger lifted - stopping auto-scroll")
                    break
                }
                if (isWaitingForContinuation) {
                    Log.d("DRAG_DEBUG", "Countdown started - pausing auto-scroll")
                    break
                }

                when (autoScrollDirection) {
                    -1 -> {
                        // Scroll up
                        exerciseListState.scrollBy(-scrollSpeed)
                        autoScrollAmount -= scrollSpeed
                        Log.d(
                            "DRAG_DEBUG",
                            "Auto-scrolling UP by $scrollSpeed, total: $autoScrollAmount"
                        )

                        // Check if we should switch cards
                        if (autoScrollAmount <= -scrollThreshold && draggedItemIndex >= 0 && draggedItemIndex > 0) {
                            val fromIndex = draggedItemIndex
                            val newList = reorderedExercises.toMutableList()
                            val itemToMove = newList.removeAt(fromIndex)
                            newList.add(fromIndex - 1, itemToMove)
                            reorderedExercises = newList
                            draggedItemIndex = fromIndex - 1
                            updateDatabaseOrder(fromIndex, fromIndex - 1)
                            autoScrollAmount = 0f // Reset scroll amount
                            Log.d(
                                "DRAG_DEBUG",
                                "Auto-scroll: Moved card UP from $fromIndex to ${fromIndex - 1}"
                            )
                        }
                    }

                    1 -> {
                        // Scroll down
                        exerciseListState.scrollBy(scrollSpeed)
                        autoScrollAmount += scrollSpeed
                        Log.d(
                            "DRAG_DEBUG",
                            "Auto-scrolling DOWN by $scrollSpeed, total: $autoScrollAmount"
                        )

                        // Check if we should switch cards
                        if (autoScrollAmount >= scrollThreshold && draggedItemIndex >= 0 && draggedItemIndex < reorderedExercises.size - 1) {
                            val fromIndex = draggedItemIndex
                            val newList = reorderedExercises.toMutableList()
                            val itemToMove = newList.removeAt(fromIndex)
                            newList.add(fromIndex + 1, itemToMove)
                            reorderedExercises = newList
                            draggedItemIndex = fromIndex + 1
                            updateDatabaseOrder(fromIndex, fromIndex + 1)
                            autoScrollAmount = 0f // Reset scroll amount
                            Log.d(
                                "DRAG_DEBUG",
                                "Auto-scroll: Moved card DOWN from $fromIndex to ${fromIndex + 1}"
                            )
                        }
                    }
                }
                kotlinx.coroutines.delay(16) // 60fps
            }
        } else {
            Log.d("DRAG_DEBUG", "Auto-scroll not starting - autoScrollDirection: $autoScrollDirection, isFingerDown: $isFingerDown, isWaitingForContinuation: $isWaitingForContinuation")
        }
    }

    // Countdown handled by startCountdown() and cancelCountdown() functions

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
                    val isActiveWorkout =
                        currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true
                    if (workoutStarted && isActiveWorkout) {
                        Button(
                            onClick = { endWorkoutSession() },
                            modifier = Modifier.padding(
                                start = 4.dp,
                                end = 0.dp,
                                top = 2.dp,
                                bottom = 2.dp
                            ),
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
                            modifier = Modifier.padding(
                                start = 4.dp,
                                end = 4.dp,
                                top = 2.dp,
                                bottom = 2.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Exit",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .pointerInput(Unit) {
                        // Global pointer input to detect movement and finger lift during countdown
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                        // Update movement time on any pointer movement
                                        // This ensures countdown cancellation works even after drag gesture ends
                                        if (isWaitingForContinuation || autoScrollDirection != 0) {
                                            recordMovement()
                                        }
                                    }
                                    androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                        // Global finger lift detection - cancel countdown if running
                                        if (isFingerDown && isWaitingForContinuation) {
                                            Log.d("DRAG_DEBUG", "Global finger lift detected - cancelling countdown")
                                            countdownJob?.cancel()
                                            countdownJob = null
                                            isWaitingForContinuation = false
                                            deselectionTimeLeft = 0f
                                        }
                                        if (isFingerDown) {
                                            isFingerDown = false
                                            autoScrollDirection = 0
                                            autoScrollAmount = 0f
                                            draggedItem = null
                                            draggedItemIndex = -1
                                            dragOffset = Offset.Zero
                                            hasStartedDragging = false
                                            dragDirection = 0
                                            Log.d("DRAG_DEBUG", "Global finger lift - resetting all drag state")
                                        }
                                    }
                                }
                            }
                        }
                    },
                state = exerciseListState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout duration and break timer display
                item {
                    if (workoutStarted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            WorkoutDurationDisplay(startTimeWorkout, workoutStarted)
                            Spacer(modifier = Modifier.width(16.dp))
                            BreakTimerDisplay()
                        }
                    }
                }
                
                // Warm-up Section - Always positioned at the top
                if (selectedWarmUp != null) {
                    // Warm-up exercises display
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Warm-up: ${selectedWarmUp!!.template.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { removeWarmUp() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.minus_icon),
                                            contentDescription = "Remove Warm-up",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = selectedWarmUp!!.template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                // Warm-up exercises list
                                Log.d("WorkoutDetailsScreen", "Rendering warm-up exercises: ${warmUpExercises.size} exercises")
                                warmUpExercises.forEachIndexed { index, warmUpExercise ->
                                    var exercise by remember { mutableStateOf<EntityExercise?>(null) }
                                    
                                    // Debug: Log the warm-up exercise data
                                    LaunchedEffect(warmUpExercise) {
                                        Log.d("WorkoutDetailsScreen", "Warm-up exercise: ID=${warmUpExercise.exerciseId}, sets=${warmUpExercise.sets}, reps=${warmUpExercise.reps}")
                                    }
                                    
                                    // Load exercise data in a coroutine
                                    LaunchedEffect(warmUpExercise.exerciseId) {
                                        try {
                                            Log.d("WorkoutDetailsScreen", "Loading exercise data for ID: ${warmUpExercise.exerciseId}")
                                            exercise = dao.getExerciseById(warmUpExercise.exerciseId)
                                            if (exercise != null) {
                                                Log.d("WorkoutDetailsScreen", "Exercise loaded successfully: ${exercise?.name} (ID: ${exercise?.id})")
                                            } else {
                                                Log.e("WorkoutDetailsScreen", "Exercise not found in database for ID: ${warmUpExercise.exerciseId}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("WorkoutDetailsScreen", "Error loading exercise: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                    
                                    exercise?.let { exerciseData ->

                                        // Check if this warm-up exercise is completed
                                        val currentWorkoutState = currentWorkout
                                        val isWarmUpCompleted = if (currentWorkoutState?.workoutId == workoutId && currentWorkoutState.isActive == true) {
                                            generalViewModel.isExerciseCompleted(exerciseData.id)
                                        } else {
                                            false
                                        }
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                                .clickable {
                                                    // If warm-up exercise is completed, show completion dialog instead of navigating
                                                    if (isWarmUpCompleted) {
                                                        // Show completion info dialog
                                                        completedExerciseInfo = WorkoutExerciseWithDetails(
                                                            workoutExercise = WorkoutExercise(
                                                                id = -1,
                                                                workoutId = workoutId,
                                                                exerciseId = exerciseData.id,
                                                                sets = warmUpExercise.sets,
                                                                reps = if (warmUpExercise.isTimeBased) warmUpExercise.duration else warmUpExercise.reps,
                                                                weight = 0,
                                                                order = -1
                                                            ),
                                                            entityExercise = exerciseData
                                                        )
                                                        showCompletedExerciseDialog = true
                                                    } else {
                                                        // Navigate to exercise details for incomplete warm-up
                                                        val currentWorkoutState = currentWorkout
                                                        val sessionId = currentWorkoutState?.sessionId ?: System.currentTimeMillis()
                                                        Log.d("WorkoutDetailsScreen", "Navigating to warm-up exercise: ${exerciseData.id}, session: $sessionId, workout: $workoutId")
                                                        
                                                        try {
                                                            navController.navigate(
                                                                Screen.Exercise.createRoute(
                                                                    exerciseId = exerciseData.id,
                                                                    sessionId = sessionId,
                                                                    workoutId = workoutId
                                                                )
                                                            )
                                                            Log.d("WorkoutDetailsScreen", "Navigation successful")
                                                        } catch (e: Exception) {
                                                            Log.e("WorkoutDetailsScreen", "Navigation failed: ${e.message}")
                                                            e.printStackTrace()
                                                            // Show error to user
                                                            Toast.makeText(context, "Failed to open exercise: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF1A1A1A)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Box {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Exercise GIF
                                                    ExerciseGif(
                                                        gifPath = exerciseData.gifUrl,
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                    )
                                                    
                                                    // Exercise details
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = exerciseData.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "Sets: ${warmUpExercise.sets}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                            )
                                                            
                                                            if (warmUpExercise.isTimeBased) {
                                                                Text(
                                                                    text = "Duration: ${warmUpExercise.duration}s",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "Reps: ${warmUpExercise.reps}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                            
                                                            if (warmUpExercise.restBetweenSets > 0) {
                                                                Text(
                                                                    text = "Rest: ${warmUpExercise.restBetweenSets}s",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // Completion indicator (green circle) for warm-up exercises
                                                if (isWarmUpCompleted) {
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
                                }
                            }
                        }
                    }
                } else {
                    // Warm-up button
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { showWarmUpDialog = true }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Warm-up  ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.fire_icon),
                                    contentDescription = "Add Warm-up",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
                
                // Exercise items
                itemsIndexed(reorderedExercises) { index, exerciseWithDetails ->
                    SwipeableExerciseCard(
                        exercise = exerciseWithDetails.entityExercise,
                        workoutExercise = exerciseWithDetails.workoutExercise,
                        hasAlternatives = exerciseWithDetails.workoutExercise.hasAlternatives,
                        alternatives = run {
                            val alts = exerciseAlternatives[exerciseWithDetails.workoutExercise.id] ?: emptyList()
                            // DON'T filter out active exercise - carousel needs full list to maintain correct indices
                            println("CAROUSEL_VOID: SwipeableExerciseCard for ${exerciseWithDetails.entityExercise.name} received ${alts.size} alternatives")
                            alts
                        },
                        onSwipeLeft = {
                            // Left swipe action - could be used for other features
                        },
                        onSwipeRight = {
                            // Right swipe action - could be used for other features
                        },
                        onAddAlternativeClick = {
                            handleExerciseSwipe(exerciseWithDetails)
                        },
                        onSelectAlternative = { selectedExercise ->
                            // Handle alternative selection
                            println("CAROUSEL: onSelectAlternative called for exercise: ${selectedExercise.name} (id: ${selectedExercise.id})")
                            coroutineScope.launch {
                                // Find the ExerciseAlternative record for this exercise
                                val alternativesList = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
                                
                                // Check if this is an alternative exercise
                                val alternative = alternativesList.find { it.alternativeExerciseId == selectedExercise.id }
                                
                                // Check if this is the original exercise
                                val isOriginal = alternativesList.firstOrNull()?.originalExerciseId == selectedExercise.id
                                
                                if (alternative != null) {
                                    println("CAROUSEL: Found alternative record, activating...")
                                    // Deactivate all alternatives for this workout exercise
                                    dao.deactivateAllAlternatives(exerciseWithDetails.workoutExercise.id)
                                    
                                    // Activate the selected alternative
                                    dao.activateAlternative(alternative.id)
                                    
                                    // Update the workout exercise to use the alternative exercise
                                    dao.updateWorkoutExerciseId(exerciseWithDetails.workoutExercise.id, alternative.alternativeExerciseId)
                                    
                                    // Refresh the exercise list
                                    val updatedWorkoutExercises = dao.getWorkoutExercisesForWorkout(workoutId)
                                    val updatedExercises = updatedWorkoutExercises.map { workoutExercise ->
                                        val exercise = dao.getExerciseById(workoutExercise.exerciseId)
                                        WorkoutExerciseWithDetails(workoutExercise, exercise!!)
                                    }
                                    reorderedExercises = updatedExercises
                                    viewModel.updateExercisesOrder(updatedExercises)
                                } else if (isOriginal) {
                                    println("CAROUSEL: Switching back to original exercise...")
                                    // Deactivate all alternatives
                                    dao.deactivateAllAlternatives(exerciseWithDetails.workoutExercise.id)
                                    
                                    // Update the workout exercise to use the original exercise
                                    dao.updateWorkoutExerciseId(exerciseWithDetails.workoutExercise.id, selectedExercise.id)
                                    
                                    // Refresh the exercise list
                                    val updatedWorkoutExercises = dao.getWorkoutExercisesForWorkout(workoutId)
                                    val updatedExercises = updatedWorkoutExercises.map { workoutExercise ->
                                        val exercise = dao.getExerciseById(workoutExercise.exerciseId)
                                        WorkoutExerciseWithDetails(workoutExercise, exercise!!)
                                    }
                                    reorderedExercises = updatedExercises
                                    viewModel.updateExercisesOrder(updatedExercises)
                                    
                                    // Refresh exercise alternatives map
                                    val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
                                    reorderedExercises.forEach { exWithDetails ->
                                        val alts = dao.getExerciseAlternatives(exWithDetails.workoutExercise.id)
                                        
                                        // Build the full list: original exercise + all alternatives
                                        val allExercises = mutableListOf<EntityExercise>()
                                        
                                        // Add the original exercise if there are any alternatives
                                        if (alts.isNotEmpty()) {
                                            val originalExerciseId = alts.firstOrNull()?.originalExerciseId
                                            if (originalExerciseId != null) {
                                                try {
                                                    val originalExercise = dao.getExerciseById(originalExerciseId)
                                                    if (originalExercise != null && originalExercise.id != exWithDetails.entityExercise.id) {
                                                        allExercises.add(originalExercise)
                                                    }
                                                } catch (e: Exception) {
                                                    println("CAROUSEL: Failed to load original exercise $originalExerciseId: ${e.message}")
                                                }
                                            }
                                        }
                                        
                                        // Add all alternative exercises
                                        val alternativeExercises = alts.mapNotNull { alt ->
                                            try {
                                                dao.getExerciseById(alt.alternativeExerciseId)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        allExercises.addAll(alternativeExercises)
                                        
                                        alternativesMap[exWithDetails.workoutExercise.id] = allExercises
                                    }
                                    exerciseAlternatives = alternativesMap
                                    
                                    println("CAROUSEL: Original exercise activated successfully")
                                } else {
                                    println("CAROUSEL: ERROR - Exercise ${selectedExercise.id} is neither an alternative nor the original")
                                }
                            }
                        },
                        onRemoveAlternative = { exerciseToRemove ->
                            // Handle alternative removal with special logic for original exercise
                            println("CAROUSEL: onRemoveAlternative called for exercise: ${exerciseToRemove.name} (id: ${exerciseToRemove.id})")
                            coroutineScope.launch {
                                removeAlternativeExercise(exerciseWithDetails.workoutExercise.id, exerciseToRemove.id)
                            }
                        }
                    ) {
                        // Content - this is where the exercise card content goes
                        ExerciseCardContent(
                            exercise = exerciseWithDetails.entityExercise,
                            workoutExercise = exerciseWithDetails.workoutExercise,
                            index = index,
                            context = context,
                            currentWorkout = currentWorkout,
                            swapThreshold = swapThreshold,
                            cardHeightPx = cardHeightPx,
                            exerciseListState = exerciseListState,
                            isSwipeInProgress = it,
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
                                isWaitingForContinuation = false // Cancel any ongoing countdown
                                deselectionTimeLeft = 0f
                            },
                            onDragEnd = {
                                // Reset only drag-related state
                                isDragging = false
                                draggedItem = null
                                dragOffset = Offset.Zero
                                hasStartedDragging = false
                                dragDirection = 0

                                Log.d(
                                    "DRAG_DEBUG",
                                    "Drag gesture ended - movement monitoring will handle countdown"
                                )
                            },
                            onDragMove = { offset ->
                                // Record movement activity
                                recordMovement()

                                handleDragMove(offset, swapThreshold, cardHeightPx, exerciseListState)
                            }
                        )
                    }
                }
                
                // Add Exercise Button
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                navController.navigate(
                                    Screen.AddExerciseToWorkout.createRoute(
                                        workoutId
                                    )
                                )
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
                }
                

                // Recovery Factors Section - Show when:
                // 1. No active workout at all, OR
                // 2. We're viewing the same workout that's currently active
                val shouldShowRecoveryFactors = when {
                    currentWorkout?.isActive != true -> true // No active workout
                    currentWorkout?.workoutId == workoutId && currentWorkout?.isActive == true -> true // Inside active workout
                    else -> false // Active workout exists but we're viewing a different workout
                }
                
                if (shouldShowRecoveryFactors) {
                    item {
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
                                        Box(
                                            modifier = Modifier
                                                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.7f).dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
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
                                                            "• Stress Level (1-10): Your current stress level")
                                            }
                                        }
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
                                    Triple(
                                        "Sleep",
                                        recoveryFactors.sleepQuality,
                                        "Sleep Quality (1-10)"
                                    ),
                                    Triple(
                                        "Protein",
                                        recoveryFactors.proteinIntake,
                                        "Protein Intake (g)"
                                    ),
                                    Triple(
                                        "Hydration",
                                        recoveryFactors.hydration,
                                        "Hydration Level (1-10)"
                                    ),
                                    Triple(
                                        "Stress Lvl",
                                        recoveryFactors.stressLevel,
                                        "Stress Level (1-10)"
                                    )
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
        }
    }

    // Completed Exercise Info Dialog
    if (showCompletedExerciseDialog && completedExerciseInfo != null) {
        var exerciseSessionData by remember { mutableStateOf<SessionEntityExercise?>(null) }
        var isLoadingSessionData by remember { mutableStateOf(true) }
        
        // Fetch exercise session data when dialog opens
        LaunchedEffect(completedExerciseInfo) {
            completedExerciseInfo?.let { exerciseInfo ->
                Log.d("WorkoutDetailsScreen", "Dialog opened for exercise: ${exerciseInfo.entityExercise.id} (${exerciseInfo.entityExercise.name})")
                Log.d("WorkoutDetailsScreen", "Current workout state: $currentWorkout")
                exerciseSessionData = getCompletedExerciseSessionData(exerciseInfo.entityExercise.id)
                Log.d("WorkoutDetailsScreen", "Session data result: $exerciseSessionData")
                
                // Load saved soreness factors for this specific exercise with fallback logic
                if (exerciseSessionData != null) {
                    // Check if current session has non-default soreness factors
                    val currentSession = exerciseSessionData!!
                    val hasCurrentSorenessFactors = currentSession.eccentricFactor != 1.0f || 
                                                   currentSession.noveltyFactor != 5 || 
                                                   currentSession.adaptationLevel != 5 || 
                                                   currentSession.rpe != 5 || 
                                                   currentSession.subjectiveSoreness != 5
                    
                    if (hasCurrentSorenessFactors) {
                        // Use current session's soreness factors
                        eccentricFactor = currentSession.eccentricFactor
                        noveltyFactor = currentSession.noveltyFactor
                        adaptationLevel = currentSession.adaptationLevel
                        rpe = currentSession.rpe
                        subjectiveSoreness = currentSession.subjectiveSoreness
                        defaultSorenessValues = false
                        Log.d("WorkoutDetailsScreen", "Loaded soreness factors from current session for exercise ${exerciseInfo.entityExercise.id}: eccentric=$eccentricFactor, novelty=$noveltyFactor, adaptation=$adaptationLevel, rpe=$rpe, soreness=$subjectiveSoreness")
                    } else {
                        // Current session has default values, try to find previous session with soreness factors
                        val previousSessionWithSoreness = withContext(Dispatchers.IO) {
                            dao.getLatestExerciseSessionWithSorenessFactors(exerciseInfo.entityExercise.id.toLong())
                        }
                        
                        if (previousSessionWithSoreness != null) {
                            // Use previous session's soreness factors
                            eccentricFactor = previousSessionWithSoreness.eccentricFactor
                            noveltyFactor = previousSessionWithSoreness.noveltyFactor
                            adaptationLevel = previousSessionWithSoreness.adaptationLevel
                            rpe = previousSessionWithSoreness.rpe
                            subjectiveSoreness = previousSessionWithSoreness.subjectiveSoreness
                            defaultSorenessValues = false
                            Log.d("WorkoutDetailsScreen", "Loaded soreness factors from previous session for exercise ${exerciseInfo.entityExercise.id}: eccentric=$eccentricFactor, novelty=$noveltyFactor, adaptation=$adaptationLevel, rpe=$rpe, soreness=$subjectiveSoreness")
                        } else {
                            // No previous soreness factors found, use defaults
                            eccentricFactor = 1.0f
                            noveltyFactor = 5
                            adaptationLevel = 5
                            rpe = 5
                            subjectiveSoreness = 5
                            defaultSorenessValues = true
                            Log.d("WorkoutDetailsScreen", "No previous soreness factors found for exercise ${exerciseInfo.entityExercise.id}, using defaults")
                        }
                    }
                } else {
                    // No current session data, try to find previous session with soreness factors
                    val previousSessionWithSoreness = withContext(Dispatchers.IO) {
                        dao.getLatestExerciseSessionWithSorenessFactors(exerciseInfo.entityExercise.id.toLong())
                    }
                    
                    if (previousSessionWithSoreness != null) {
                        // Use previous session's soreness factors
                        eccentricFactor = previousSessionWithSoreness.eccentricFactor
                        noveltyFactor = previousSessionWithSoreness.noveltyFactor
                        adaptationLevel = previousSessionWithSoreness.adaptationLevel
                        rpe = previousSessionWithSoreness.rpe
                        subjectiveSoreness = previousSessionWithSoreness.subjectiveSoreness
                        defaultSorenessValues = false
                        Log.d("WorkoutDetailsScreen", "Loaded soreness factors from previous session for exercise ${exerciseInfo.entityExercise.id}: eccentric=$eccentricFactor, novelty=$noveltyFactor, adaptation=$adaptationLevel, rpe=$rpe, soreness=$subjectiveSoreness")
                    } else {
                        // No previous soreness factors found, use defaults
                        eccentricFactor = 1.0f
                        noveltyFactor = 5
                        adaptationLevel = 5
                        rpe = 5
                        subjectiveSoreness = 5
                        defaultSorenessValues = true
                        Log.d("WorkoutDetailsScreen", "No previous soreness factors found for exercise ${exerciseInfo.entityExercise.id}, using defaults")
                    }
                }
                
                isLoadingSessionData = false
            }
        }
        
        Dialog(
            onDismissRequest = { 
                showCompletedExerciseDialog = false 
                completedExerciseInfo = null
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .then(
                        if (!defaultSorenessValues) {
                            Modifier.fillMaxHeight(0.9f)
                        } else {
                            Modifier.wrapContentHeight()
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                                        // Title
                Text(
                    "Exercise Completed",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                ) 
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content (scrollable when muscle soreness is expanded)
                Column(
                        modifier = Modifier
                            .then(
                                if (!defaultSorenessValues) {
                                    Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                } else {
                                    Modifier
                                }
                            ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Exercise name
                    Text(
                        text = completedExerciseInfo!!.entityExercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Exercise details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Exercise Details",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "Muscle Group: ${completedExerciseInfo!!.entityExercise.muscle}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "Difficulty: ${completedExerciseInfo!!.entityExercise.difficulty}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                        }
                    }
                    
                    // Individual Set Performance
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Set Performance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (isLoadingSessionData) {
                                // Loading indicator
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (exerciseSessionData != null) {
                                // Display each set
                                exerciseSessionData!!.let { sessionData ->
                                    val completedSets = sessionData.completedSets
                                    val weights = sessionData.weight
                                    val repsOrTimes = sessionData.repsOrTime
                                    
                                    if (completedSets > 0) {
                                        for (setIndex in 0 until completedSets) {
                                            val setNumber = setIndex + 1
                                            val weight = weights.getOrNull(setIndex) ?: 0
                                            val repsOrTime = repsOrTimes.getOrNull(setIndex) ?: 0
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Set $setNumber",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                if (completedExerciseInfo!!.entityExercise.useTime) {
                                                    // Time-based exercise
                                                    val minutes = repsOrTime / 60
                                                    val seconds = repsOrTime % 60
                                                    Text(
                                                        text = "Time: ${minutes}:${String.format("%02d", seconds)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                } else {
                                                    // Rep-based exercise
                                                    Text(
                                                        text = "Weight: ${weight}kg × Reps: $repsOrTime",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No set data available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Failed to load set data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Track Muscle Soreness section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Track Muscle Soreness",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Checkbox(
                                    checked = !defaultSorenessValues,
                                    onCheckedChange = { checked ->
                                        defaultSorenessValues = !checked
                                        if (checked) {
                                            // Reset to default values
                                            eccentricFactor = 1.0f
                                            noveltyFactor = 5
                                            adaptationLevel = 5
                                            rpe = 5
                                            subjectiveSoreness = 5
                                        }
                                    }
                                )
                            }

                            if (!defaultSorenessValues) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Exercise Factors section
                                Text(
                                    "Exercise Factors",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Eccentric Factor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Eccentric Factor (1.0-2.0)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { showSorenessInfoDialog = "eccentric" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Eccentric Factor Info",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = eccentricFactor,
                                        onValueChange = { newValue -> 
                                            val oldValue = eccentricFactor
                                            eccentricFactor = newValue
                                            vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                        },
                                        valueRange = 1f..2f,
                                        steps = 9,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format("%.1f", eccentricFactor),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Novelty Factor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Novelty Factor (0-10)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { showSorenessInfoDialog = "novelty" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Novelty Factor Info",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = noveltyFactor.toFloat(),
                                        onValueChange = { newValue -> 
                                            val oldValue = noveltyFactor.toFloat()
                                            noveltyFactor = newValue.toInt()
                                            vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                        },
                                        valueRange = 0f..10f,
                                        steps = 10,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = noveltyFactor.toString(),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Adaptation Level
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Adaptation Level (0-10)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { showSorenessInfoDialog = "adaptation" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Adaptation Level Info",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = adaptationLevel.toFloat(),
                                        onValueChange = { newValue -> 
                                            val oldValue = adaptationLevel.toFloat()
                                            adaptationLevel = newValue.toInt()
                                            vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                        },
                                        valueRange = 0f..10f,
                                        steps = 10,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = adaptationLevel.toString(),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Perceived Exertion & Soreness section
                                Text(
                                    "Perceived Exertion & Soreness",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // RPE
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Rate of Perceived Exertion (1-10)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { showSorenessInfoDialog = "rpe" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "RPE Info",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = rpe.toFloat(),
                                        onValueChange = { newValue -> 
                                            val oldValue = rpe.toFloat()
                                            rpe = newValue.toInt()
                                            vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                        },
                                        valueRange = 1f..10f,
                                        steps = 9,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = rpe.toString(),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Subjective Soreness
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Subjective Soreness (1-10)",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { showSorenessInfoDialog = "soreness" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Soreness Info",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = subjectiveSoreness.toFloat(),
                                        onValueChange = { newValue -> 
                                            val oldValue = subjectiveSoreness.toFloat()
                                            subjectiveSoreness = newValue.toInt()
                                            vibrateOnValueChangeIfDifferent(oldValue, newValue)
                                        },
                                        valueRange = 1f..10f,
                                        steps = 9,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = subjectiveSoreness.toString(),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                TextButton(
                    onClick = { 
                        showCompletedExerciseDialog = false 
                        completedExerciseInfo = null
                    }
                ) {
                    Text("Cancel")
                }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { 
                                completedExerciseInfo?.let { exerciseInfo ->
                                    coroutineScope.launch {
                                        saveExerciseSessionWithSoreness(exerciseInfo)
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Muscle Soreness Info Dialog
    if (showSorenessInfoDialog != null) {
        AlertDialog(
            onDismissRequest = { showSorenessInfoDialog = null },
            title = {
                Text(
                    text = when (showSorenessInfoDialog) {
                        "eccentric" -> "Eccentric Factor"
                        "novelty" -> "Novelty Factor"
                        "adaptation" -> "Adaptation Level"
                        "rpe" -> "Rate of Perceived Exertion"
                        "soreness" -> "Subjective Soreness"
                        else -> ""
                    }
                )
            },
            text = {
                Text(
                    text = when (showSorenessInfoDialog) {
                        "eccentric" -> "The degree of emphasis on the lowering (eccentric) phase of the movement. Higher values indicate slower, more controlled negatives."
                        "novelty" -> "How new or different this exercise is from your usual routine. Higher values indicate more novel movements that may cause more soreness."
                        "adaptation" -> "How well adapted your body is to this exercise. Higher values indicate better adaptation and potentially less soreness."
                        "rpe" -> "How hard the exercise felt. 1 = Very easy, 5 = Moderate effort, 10 = Maximum effort possible."
                        "soreness" -> "Current muscle soreness level. 1 = No soreness, 5 = Moderate discomfort, 10 = Severe pain/inability to move."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSorenessInfoDialog = null }) {
                    Text("OK")
                }
            }
        )
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
                TextButton(onClick = {
                    showRenameDialog = false
                    newWorkoutName = ""
                }) {
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
            text = { Text("Are you sure you want to exit and delete this workout session? All completed exercises will also be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    coroutineScope.launch {
                        // Remove session and all exercise sessions from DB
                        val sessionId = currentWorkout?.sessionId
                        if (sessionId != null && sessionId > 0) {
                            withContext(Dispatchers.IO) {
                                // Delete exercise sessions first (due to foreign key constraints)
                                dao.deleteExerciseSessionsBySessionId(sessionId)
                                // Then delete the workout session
                                dao.deleteWorkoutSessionById(sessionId)
                                Log.d("WorkoutDetailsScreen", "Deleted workout session $sessionId and all associated exercise sessions")
                            }
                        }
                        // Stop the break timer when exiting workout
                        viewModel.stopBreakTimer()
                        
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
    
    // Warm-up Selection Dialog
    if (showWarmUpDialog) {
        var warmUpTemplates by remember { mutableStateOf<List<WarmUpTemplateWithExercises>>(emptyList()) }
        var isLoadingTemplates by remember { mutableStateOf(true) }
        
        // Load warm-up templates when dialog opens
        LaunchedEffect(showWarmUpDialog) {
            if (showWarmUpDialog) {
                try {
                    val templates = warmUpDao.getAllWarmUpTemplatesWithExercises().first()
                    warmUpTemplates = templates
                } catch (e: Exception) {
                    Log.e("WorkoutDetailsScreen", "Error loading warm-up templates: ${e.message}")
                } finally {
                    isLoadingTemplates = false
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { showWarmUpDialog = false },
            title = { 
                Text(
                    text = "Select Warm-up Template",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                if (isLoadingTemplates) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (warmUpTemplates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No warm-up templates available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(warmUpTemplates) { template ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectWarmUpTemplate(template) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = template.template.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    if (template.template.description.isNotEmpty()) {
                                        Text(
                                            text = template.template.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Category: ${template.template.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        
                                        Text(
                                            text = "Difficulty: ${template.template.difficulty}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        
                                        Text(
                                            text = "${template.template.estimatedDuration} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    Text(
                                        text = "${template.exercises.size} exercises",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWarmUpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load alternatives for all exercises when reorderedExercises is available
    LaunchedEffect(reorderedExercises.isNotEmpty()) {
        if (reorderedExercises.isNotEmpty()) {
            println("CAROUSEL: Loading alternatives for ${reorderedExercises.size} exercises")
            val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
            reorderedExercises.forEach { exerciseWithDetails ->
                val alternatives = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
                println("CAROUSEL: Exercise ${exerciseWithDetails.entityExercise.name} has ${alternatives.size} alternatives")
                
                // Build the full list: original exercise + all alternatives
                val allExercises = mutableListOf<EntityExercise>()
                
                // Add the original exercise if there are any alternatives
                if (alternatives.isNotEmpty()) {
                    val originalExerciseId = alternatives.firstOrNull()?.originalExerciseId
                    if (originalExerciseId != null) {
                        try {
                            val originalExercise = dao.getExerciseById(originalExerciseId)
                            if (originalExercise != null && originalExercise.id != exerciseWithDetails.entityExercise.id) {
                                println("CAROUSEL: Adding original exercise to alternatives: ${originalExercise.name}")
                                allExercises.add(originalExercise)
                            }
                        } catch (e: Exception) {
                            println("CAROUSEL: Failed to load original exercise $originalExerciseId: ${e.message}")
                        }
                    }
                }
                
                // Add all alternative exercises
                val alternativeExercises = alternatives.mapNotNull { alt ->
                    try {
                        val exercise = dao.getExerciseById(alt.alternativeExerciseId)
                        if (exercise != null) {
                            println("CAROUSEL: Found alternative exercise: ${exercise.name}")
                            exercise
                        } else {
                            println("CAROUSEL: Exercise ${alt.alternativeExerciseId} not found")
                            null
                        }
                    } catch (e: Exception) {
                        println("CAROUSEL: Failed to load exercise ${alt.alternativeExerciseId}: ${e.message}")
                        null
                    }
                }
                allExercises.addAll(alternativeExercises)
                
                alternativesMap[exerciseWithDetails.workoutExercise.id] = allExercises
            }
            exerciseAlternatives = alternativesMap
            println("CAROUSEL: Loaded alternatives map with ${alternativesMap.size} entries")
        }
    }
    
    // Also reload when reorderedExercises changes
    LaunchedEffect(reorderedExercises) {
        val alternativesMap = mutableMapOf<Int, List<EntityExercise>>()
        reorderedExercises.forEach { exerciseWithDetails ->
            val alternatives = dao.getExerciseAlternatives(exerciseWithDetails.workoutExercise.id)
            
            // Build the full list: original exercise + all alternatives
            val allExercises = mutableListOf<EntityExercise>()
            
            // Add the original exercise if there are any alternatives
            if (alternatives.isNotEmpty()) {
                val originalExerciseId = alternatives.firstOrNull()?.originalExerciseId
                if (originalExerciseId != null) {
                    try {
                        val originalExercise = dao.getExerciseById(originalExerciseId)
                        if (originalExercise != null && originalExercise.id != exerciseWithDetails.entityExercise.id) {
                            allExercises.add(originalExercise)
                        }
                    } catch (e: Exception) {
                        println("CAROUSEL: Failed to load original exercise $originalExerciseId: ${e.message}")
                    }
                }
            }
            
            // Add all alternative exercises
            val alternativeExercises = alternatives.mapNotNull { alt ->
                try {
                    dao.getExerciseById(alt.alternativeExerciseId)
                } catch (e: Exception) {
                    null
                }
            }
            allExercises.addAll(alternativeExercises)
            
            alternativesMap[exerciseWithDetails.workoutExercise.id] = allExercises
        }
        exerciseAlternatives = alternativesMap
    }

    // Exercise Alternatives Dialog
    if (showAlternativesDialog && selectedWorkoutExercise != null) {
        var alternativesWithDetails by remember { mutableStateOf<List<ExerciseAlternativeWithDetails>>(emptyList()) }
        
        // Load alternatives with details
        LaunchedEffect(selectedWorkoutExercise) {
            alternativesWithDetails = dao.getExerciseAlternativesWithDetails(selectedWorkoutExercise!!.workoutExercise.id)
        }
        
        ExerciseAlternativeDialog(
            currentExercise = selectedWorkoutExercise!!.entityExercise,
            onDismiss = {
                showAlternativesDialog = false
                selectedWorkoutExercise = null
                alternatives = emptyList()
                similarExercises = emptyList()
            },
            onSelectAlternative = { alternativeExercise ->
                // This is called when user selects an alternative
                addAlternativeExercise(alternativeExercise)
                // Close the dialog after adding
                showAlternativesDialog = false
                selectedWorkoutExercise = null
                similarExercises = emptyList()
            },
            dao = dao
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