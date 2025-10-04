package com.example.gymtracker.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gymtracker.R
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.WorkoutExercise
import kotlin.math.abs

@Composable
fun SwipeableExerciseCard(
    exercise: EntityExercise,
    workoutExercise: WorkoutExercise,
    hasAlternatives: Boolean,
    alternatives: List<EntityExercise> = emptyList(),
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onAddAlternativeClick: () -> Unit,
    onSelectAlternative: (EntityExercise) -> Unit = {},
    onRemoveAlternative: (EntityExercise) -> Unit = {},
    swipeActivationZone: @Composable BoxScope.() -> Unit = {},
    content: @Composable (isSwipeInProgress: Boolean) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipeInProgress by remember { mutableStateOf(false) }
    var isCarouselMode by remember { mutableStateOf(false) }
    var isExitingCarousel by remember { mutableStateOf(false) }  // New: Track exit transition
    var savedOffsetX by remember { mutableFloatStateOf(0f) } // Saved state when carousel mode was entered
    var currentAlternativeIndex by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }

    // Filter out the currently active exercise from alternatives at the top
    // This ensures all logic (navigation, rendering, boundaries) uses the same filtered list
    val nonActiveAlternatives = remember(alternatives, exercise.id) {
        alternatives.filter { it.id != exercise.id }
    }

    // Reset carousel mode when the exercise changes (after selection)
    LaunchedEffect(exercise.id) {
        println("CAROUSEL: Exercise changed to ${exercise.id}, resetting carousel mode")
        isCarouselMode = false
        isExitingCarousel = false
        currentAlternativeIndex = 0
        offsetX = 0f
        isSwipeInProgress = false
    }

    // Force reset swipe states when exiting carousel mode
    LaunchedEffect(isCarouselMode) {
        if (!isCarouselMode) {
            println("CAROUSEL: Exited carousel mode, forcing state reset")
            offsetX = 0f
            isSwipeInProgress = false
            isDragging = false
        }
    }

    val density = LocalDensity.current

    val swipeThreshold = with(density) { 100.dp.toPx() }
    val maxSwipeDistance = with(density) { 200.dp.toPx() }

    val alpha by animateFloatAsState(
        targetValue = if (isSwipeInProgress) 0.7f else 1f,
        animationSpec = tween(200),
        label = "swipe_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSwipeInProgress) 0.95f else 1f,
        animationSpec = tween(200),
        label = "swipe_scale"
    )

    // Calculate transition progress (0.0 to 1.0) based on offsetX
    // threshold: minimum swipe to trigger navigation (80dp)
    // transitionThreshold: distance for smooth visual transition (250dp - much larger for smooth animation)
    val threshold = with(density) { 80.dp.toPx() }
    val transitionThreshold = with(density) { 250.dp.toPx() }
    
    // Show carousel if in carousel mode OR during smooth transition (only after some drag)
    val isTransitioning = !isCarouselMode && isSwipeInProgress && offsetX < 0 && isDragging && abs(offsetX) > 20f
    val showCarousel = isCarouselMode || isTransitioning || isExitingCarousel
    
    // Entry transition progress (when entering carousel mode)
    val entryProgress = if (!isCarouselMode && !isExitingCarousel && offsetX < 0) {
        (abs(offsetX) / transitionThreshold).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Exit transition progress (when exiting carousel mode)
    // Goes from 1.0 (fully in carousel) → 0.0 (back to normal) as user swipes right
    val exitProgress = if (isExitingCarousel && offsetX > 0) {
        1f - (offsetX / transitionThreshold).coerceIn(0f, 1f)
    } else if (isExitingCarousel) {
        1f  // Still in carousel, no swipe yet
    } else {
        0f  // Not exiting
    }
    
    // Transition progress within carousel (ONLY when already in carousel mode)
    // During entry transition, we DON'T want card-to-card transitions - only main card shrinking
    val transitionProgress = if (isCarouselMode && offsetX != 0f) {
        (abs(offsetX) / transitionThreshold).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Debug logging
    if (isSwipeInProgress) {
        println("CAROUSEL_DEBUG: offsetX=$offsetX, isTransitioning=$isTransitioning, showCarousel=$showCarousel, isDragging=$isDragging, isCarouselMode=$isCarouselMode")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
    ) {
        // Normal mode: Main card (always rendered, but may be hidden during carousel)
        if (!isCarouselMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .alpha(
                        when {
                            isExitingCarousel -> 1f - exitProgress  // Fade in during exit (1.0 → 0.0 becomes 0.0 → 1.0)
                            showCarousel -> 1f - entryProgress  // Fade out during entry
                            else -> 1f
                        }
                    )
                    .zIndex(if (isSwipeInProgress) 1000f else 0f)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    // Card width calculation - shrinks when swiping left
                    val cardWidth = if (isSwipeInProgress && offsetX < 0) {
                        // Shrink proportionally to swipe distance
                        val shrinkAmount = with(density) { abs(offsetX).toDp() }
                        (maxWidth - shrinkAmount).coerceAtLeast(50.dp)
                    } else {
                        // Normal width
                        maxWidth
                    }

                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .graphicsLayer {
                                // Apply general scale
                                scaleY = scale
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSwipeInProgress)
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            // Show card content
                            content(false)
                        }
                    }
                }

                // Swipe activation zone - positioned ONLY on the 8-dots drag handle icon
                // ONLY show when NOT in carousel mode to avoid gesture conflicts
                if (!isCarouselMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .height(60.dp) // Height to match icon area
                            .width(60.dp) // Width to match icon clickable area (narrower to avoid remove button)
                            .padding(end = 4.dp) // Small padding from edge
                            .pointerInput(Unit) {
                                var totalDragX = 0f
                                var totalDragY = 0f
                                var isHorizontalDrag = false

                                detectDragGestures(
                                    onDragStart = { _ ->
                                        totalDragX = 0f
                                        totalDragY = 0f
                                        isHorizontalDrag = false
                                        println("CAROUSEL_VOID: [ACTIVATION ZONE] onDragStart")
                                    },
                                onDragEnd = {
                                    println("CAROUSEL_VOID: [ACTIVATION ZONE] onDragEnd - isHorizontalDrag=$isHorizontalDrag, offsetX=$offsetX, totalDragX=$totalDragX, totalDragY=$totalDragY")
                                    if (isHorizontalDrag) {
                                        println("CAROUSEL_VOID: [ACTIVATION ZONE] PROCESSING - offsetX=$offsetX, threshold=$threshold")

                                        // Check if we should enter carousel mode (threshold-based)
                                        if (offsetX < -threshold) {
                                            println("CAROUSEL_VOID: [ACTIVATION ZONE] ENTERING CAROUSEL MODE - nonActiveAlternatives.size=${nonActiveAlternatives.size}")
                                            isCarouselMode = true
                                            currentAlternativeIndex = if (nonActiveAlternatives.isEmpty()) nonActiveAlternatives.size else 0
                                            println("CAROUSEL_VOID: [ACTIVATION ZONE] Set currentAlternativeIndex=$currentAlternativeIndex")
                                            // Reset states and return early
                                            offsetX = 0f
                                            isSwipeInProgress = false
                                            isDragging = false
                                            isHorizontalDrag = false
                                            println("CAROUSEL_VOID: [ACTIVATION ZONE] Reset states - offsetX=$offsetX, isSwipeInProgress=$isSwipeInProgress")
                                            return@detectDragGestures
                                        }
                                    }

                                    // Always reset states
                                    offsetX = 0f
                                    isSwipeInProgress = false
                                    isDragging = false
                                    isHorizontalDrag = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                    isSwipeInProgress = false
                                    offsetX = 0f
                                    isHorizontalDrag = false
                                },
                                onDrag = { change, dragAmount ->
                                    totalDragX += abs(dragAmount.x)
                                    totalDragY += abs(dragAmount.y)

                                    // Determine if this is a horizontal drag after some movement
                                    if (!isHorizontalDrag && (totalDragX > 20f || totalDragY > 20f)) {
                                        isHorizontalDrag = totalDragX > totalDragY * 1.5f
                                        println("CAROUSEL_VOID: [ACTIVATION ZONE] onDrag - Checking direction: totalDragX=$totalDragX, totalDragY=$totalDragY, isHorizontalDrag=$isHorizontalDrag")
                                        if (isHorizontalDrag) {
                                            isSwipeInProgress = true
                                            isDragging = true
                                            println("CAROUSEL_VOID: [ACTIVATION ZONE] onDrag - HORIZONTAL DRAG CONFIRMED")
                                        }
                                    }

                                    // Only update offsetX if horizontal drag is confirmed
                                    if (isHorizontalDrag) {
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetX = offsetX.coerceIn(-525f, 525f)
                                        println("CAROUSEL_VOID: [ACTIVATION ZONE] onDrag - Updated offsetX=$offsetX")
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }

        // Carousel mode: Show alternatives (fade in during entry transition, fade out during exit)
        if (showCarousel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .alpha(
                        when {
                            isExitingCarousel -> exitProgress  // Fade out during exit (1.0 → 0.0)
                            isCarouselMode -> 1f  // Fully visible in carousel mode
                            else -> entryProgress  // Fade in during entry
                        }
                    )
                    .zIndex(if (isSwipeInProgress) 1001f else 1f) // Above main card
                    .pointerInput(Unit) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isHorizontalDrag = false

                    detectDragGestures(
                        onDragStart = { _ ->
                            totalDragX = 0f
                            totalDragY = 0f
                            isHorizontalDrag = false
                            println("CAROUSEL_VOID: [CAROUSEL] onDragStart - isCarouselMode=$isCarouselMode, currentIndex=$currentAlternativeIndex, offsetX=$offsetX")
                        },
                        onDragEnd = {
                            println("CAROUSEL_VOID: [CAROUSEL] onDragEnd - isHorizontalDrag=$isHorizontalDrag, offsetX=$offsetX, totalDragX=$totalDragX, totalDragY=$totalDragY, threshold=$threshold")
                            if (isHorizontalDrag) {
                                println("CAROUSEL_VOID: [CAROUSEL] onDragEnd - PROCESSING (isHorizontalDrag is TRUE)")

                                if (isCarouselMode || isExitingCarousel) {
                                    println("CAROUSEL_VOID: [CAROUSEL] In carousel/exit mode - checking offsetX=$offsetX vs threshold=$threshold")
                                    // In carousel mode - navigate between alternatives and add button
                                    if (offsetX > threshold) {
                                        println("CAROUSEL_VOID: [CAROUSEL] Swipe RIGHT detected - currentIndex=$currentAlternativeIndex, isExitingCarousel=$isExitingCarousel")
                                        
                                        if (isExitingCarousel) {
                                            // Complete the exit transition
                                            println("CAROUSEL_VOID: [CAROUSEL] COMPLETING EXIT transition")
                                            isCarouselMode = false
                                            isExitingCarousel = false
                                            currentAlternativeIndex = 0
                                            offsetX = 0f
                                            isSwipeInProgress = false
                                            isDragging = false
                                            return@detectDragGestures
                                        } else if (currentAlternativeIndex > 0) {
                                            // Navigate to previous alternative
                                            currentAlternativeIndex--
                                            println("CAROUSEL_VOID: [CAROUSEL] Moving to PREVIOUS - index now: $currentAlternativeIndex")
                                        } else {
                                            // At first alternative - START exit transition
                                            println("CAROUSEL_VOID: [CAROUSEL] STARTING EXIT transition")
                                            isExitingCarousel = true
                                            isCarouselMode = false
                                            // DON'T reset offsetX - let it continue for transition
                                        }
                                    } else if (offsetX < -threshold) {
                                        println("CAROUSEL_VOID: [CAROUSEL] Swipe LEFT detected - currentIndex=$currentAlternativeIndex, nonActiveAlternatives.size=${nonActiveAlternatives.size}")
                                        // Swipe left - go to next alternative or add button
                                        if (currentAlternativeIndex < nonActiveAlternatives.size) {
                                            currentAlternativeIndex++
                                            println("CAROUSEL_VOID: [CAROUSEL] Moving to NEXT - index now: $currentAlternativeIndex")
                                        } else {
                                            println("CAROUSEL_VOID: [CAROUSEL] Already at add button")
                                        }
                                    } else {
                                        println("CAROUSEL_VOID: [CAROUSEL] Small swipe - offsetX=$offsetX not enough to navigate")
                                    }
                                } else {
                                    println("CAROUSEL_VOID: [CAROUSEL] NOT in carousel mode yet - checking if should enter")
                                    // Not in carousel mode - check if we should enter it
                                    // This handles the case where the carousel detector processes the entry swipe
                                    // before the activation zone sets isCarouselMode=true
                                    if (offsetX < -threshold) {
                                        println("CAROUSEL_VOID: [CAROUSEL] Entering carousel mode from carousel detector - offsetX=$offsetX")
                                        isCarouselMode = true
                                        // Determine starting index based on how far the user swiped
                                        // If they swiped far enough (> 2x threshold), they want to see the NEXT card, not the first
                                        currentAlternativeIndex = if (nonActiveAlternatives.isEmpty()) {
                                            nonActiveAlternatives.size  // Go to add button if no alternatives
                                        } else if (abs(offsetX) > threshold * 2) {
                                            // User swiped far - they saw the transition to the next card, so show it
                                            1.coerceAtMost(nonActiveAlternatives.size)
                                        } else {
                                            // Normal entry - show first alternative
                                            0
                                        }
                                        println("CAROUSEL_VOID: [CAROUSEL] Carousel mode activated, currentIndex=$currentAlternativeIndex (offsetX=$offsetX, threshold*2=${threshold * 2})")
                                        // Reset states and return early
                                        offsetX = 0f
                                        isSwipeInProgress = false
                                        isDragging = false
                                        isHorizontalDrag = false
                                        return@detectDragGestures
                                    } else {
                                        println("CAROUSEL_VOID: [CAROUSEL] Not enough swipe - offsetX=$offsetX, threshold=$threshold")
                                    }
                                }
                            }

                            // Always reset states after handling gesture (moved to end to ensure clean state)
                            offsetX = 0f
                            isSwipeInProgress = false
                            isDragging = false
                            isHorizontalDrag = false
                        },
                        onDragCancel = {
                            isDragging = false
                            isSwipeInProgress = false
                            offsetX = 0f
                            isHorizontalDrag = false
                        },
                        onDrag = { change, dragAmount ->
                            totalDragX += abs(dragAmount.x)
                            totalDragY += abs(dragAmount.y)

                            // Determine if this is a horizontal drag after some movement
                            if (!isHorizontalDrag && (totalDragX > 20f || totalDragY > 20f)) {
                                isHorizontalDrag = totalDragX > totalDragY * 1.5f
                                println("CAROUSEL_VOID: [CAROUSEL] onDrag - Checking direction: totalDragX=$totalDragX, totalDragY=$totalDragY, isHorizontalDrag=$isHorizontalDrag")
                                if (isHorizontalDrag) {
                                    isSwipeInProgress = true
                                    isDragging = true
                                    println("CAROUSEL_VOID: [CAROUSEL] onDrag - HORIZONTAL DRAG CONFIRMED, isDragging=$isDragging")
                                }
                            }

                            // Only consume and handle horizontal drags
                            if (isHorizontalDrag) {
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(
                                    -maxSwipeDistance,
                                    maxSwipeDistance
                                )
                                println("CAROUSEL_VOID: [CAROUSEL] onDrag - Updated offsetX=$offsetX (dragAmount.x=${dragAmount.x})")
                                
                                // Check for exit transition during drag
                                if (isCarouselMode && currentAlternativeIndex == 0 && offsetX > 0) {
                                    println("CAROUSEL_VOID: [CAROUSEL] onDrag - RIGHT swipe at first alternative - STARTING EXIT transition")
                                    isExitingCarousel = true
                                    isCarouselMode = false
                                }
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Determine which card is transitioning based on swipe direction
            val totalItems = nonActiveAlternatives.size + 1 // +1 for Add button
            val nextIndex = when {
                offsetX > 0 && currentAlternativeIndex > 0 -> currentAlternativeIndex - 1 // Swiping right
                offsetX < 0 && currentAlternativeIndex < totalItems -> currentAlternativeIndex + 1 // Swiping left
                else -> currentAlternativeIndex // No transition
            }

            // Original exercise (always shown as slim bar in carousel mode)
            println("CAROUSEL_VOID: [RENDER] >>> RENDERING ORIGINAL EXERCISE SLIM BAR <<<")
            SlimExerciseBar(
                exercise = exercise,
                isOriginal = true,
                onClick = { }
            )

            // Alternative exercises with smooth transitions
            println("CAROUSEL_VOID: [RENDER] Rendering alternatives - currentIndex=$currentAlternativeIndex, nextIndex=$nextIndex, nonActiveAlternatives.size=${nonActiveAlternatives.size}, offsetX=$offsetX, transitionProgress=$transitionProgress")
            
            nonActiveAlternatives.forEachIndexed { index, alternative ->
                println("CAROUSEL_VOID: [RENDER] Alternative $index: ${alternative.name} (ID: ${alternative.id})")
                key(alternative.id) {  // Use unique key to prevent recomposition issues
                    TransitionAlternativeCard(
                        exercise = alternative,
                        itemIndex = index,
                        currentIndex = currentAlternativeIndex,
                        nextIndex = nextIndex,
                        transitionProgress = transitionProgress,
                        entryProgress = entryProgress,
                        exitProgress = exitProgress,
                        isCarouselMode = isCarouselMode,
                        isExitingCarousel = isExitingCarousel,
                        offsetX = offsetX,
                        onSelectAlternative = {
                            println("CAROUSEL: Alternative selected - exiting carousel mode")
                            onSelectAlternative(alternative)
                            // Exit carousel mode after selection
                            isCarouselMode = false
                            isExitingCarousel = false
                            currentAlternativeIndex = 0
                        },
                        onRemoveAlternative = {
                            println("CAROUSEL: Alternative removal requested")
                            onRemoveAlternative(alternative)
                        }
                    )
                }
            }

            // Add button with smooth transitions
            val addButtonIndex = nonActiveAlternatives.size
            TransitionAddButton(
                itemIndex = addButtonIndex,
                currentIndex = currentAlternativeIndex,
                nextIndex = nextIndex,
                transitionProgress = transitionProgress,
                offsetX = offsetX,
                onAddClick = onAddAlternativeClick
            )
        }
        }
    }
}


@Composable
fun AlternativeItem(
    exercise: EntityExercise,
    isOriginal: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOriginal)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOriginal) "Original: ${exercise.name}" else exercise.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isOriginal) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun AddAlternativeItem(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alternative",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RowScope.AlternativeCard(
    exercise: EntityExercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show GIF image if available
            if (exercise.gifUrl.isNotEmpty()) {
                ExerciseGif(
                    gifPath = exercise.gifUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    cornerRadius = 8f
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = exercise.muscle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.SlimExerciseBar(
    exercise: EntityExercise,
    isOriginal: Boolean,
    onClick: () -> Unit
) {
    println("CAROUSEL_VOID: [SLIM BAR] Rendering slim bar for: ${exercise.name} (ID: ${exercise.id}), isOriginal=$isOriginal")
    Card(
        modifier = Modifier
            .weight(0.005f)  // Use even smaller weight to ensure it's truly minimal
            .fillMaxHeight()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        // No content - just an empty slim bar
    }
}

@Composable
private fun RowScope.TransitionAlternativeCard(
    exercise: EntityExercise,
    itemIndex: Int,
    currentIndex: Int,
    nextIndex: Int,
    transitionProgress: Float,
    entryProgress: Float,
    exitProgress: Float,
    isCarouselMode: Boolean,
    isExitingCarousel: Boolean,
    offsetX: Float,
    onSelectAlternative: () -> Unit,
    onRemoveAlternative: () -> Unit
) {
    // Determine if this card is currently active, becoming active, or becoming inactive
    val isCurrentlyActive = itemIndex == currentIndex
    val isBecomingActive = itemIndex == nextIndex && itemIndex != currentIndex
    val isBecomingInactive = itemIndex == currentIndex && nextIndex != currentIndex

    println("CAROUSEL_VOID: [CARD $itemIndex] isCurrentlyActive=$isCurrentlyActive, isBecomingActive=$isBecomingActive, isBecomingInactive=$isBecomingInactive (currentIndex=$currentIndex, nextIndex=$nextIndex)")

    // Calculate the width fraction (0.0 = slim bar, 1.0 = full width)
    val widthFraction = when {
        // During EXIT transition: first card (index 0) shrinks gradually
        isExitingCarousel && itemIndex == 0 -> {
            // exitProgress goes 1.0 → 0.0 as user swipes right
            // So multiply by same slow factor for smooth transition
            val fraction = (exitProgress * 0.15f).coerceIn(0f, 1f)
            println("CAROUSEL_VOID: [CARD $itemIndex] EXIT SHRINKING - widthFraction=$fraction (exitProgress=$exitProgress)")
            fraction
        }
        
        // During EXIT transition: other cards stay as slim bars
        isExitingCarousel && itemIndex != 0 -> {
            println("CAROUSEL_VOID: [CARD $itemIndex] EXIT SLIM BAR - widthFraction=0.0")
            0f
        }
        
        // During ENTRY transition: first card (index 0) expands gradually based on offsetX
        // It should expand at the SAME RATE as the main card shrinks (pixel-perfect sync)
        !isCarouselMode && itemIndex == 0 -> {
            // Use entryProgress but make it expand MUCH slower to match main card shrinking
            // entryProgress goes 0→1 over transitionThreshold (250dp)
            // We use a small factor (0.15) so it takes much longer to fully expand
            val fraction = (entryProgress * 0.15f).coerceIn(0f, 1f)  // Only 15% of entryProgress - very slow expansion
            println("CAROUSEL_VOID: [CARD $itemIndex] ENTRY EXPANDING - widthFraction=$fraction (entryProgress=$entryProgress)")
            fraction
        }
        
        // During ENTRY transition: other cards stay as slim bars
        !isCarouselMode && itemIndex != 0 -> {
            println("CAROUSEL_VOID: [CARD $itemIndex] ENTRY SLIM BAR - widthFraction=0.0")
            0f
        }
        
        isCurrentlyActive && isBecomingInactive -> {
            // Shrinking from expanded to slim
            val fraction = 1f - transitionProgress
            println("CAROUSEL_VOID: [CARD $itemIndex] SHRINKING - widthFraction=$fraction")
            fraction
        }

        !isCurrentlyActive && isBecomingActive -> {
            // Expanding from slim to full
            val fraction = transitionProgress
            println("CAROUSEL_VOID: [CARD $itemIndex] EXPANDING - widthFraction=$fraction")
            fraction
        }

        isCurrentlyActive && !isBecomingInactive -> {
            // Fully expanded, no transition
            println("CAROUSEL_VOID: [CARD $itemIndex] FULLY EXPANDED - widthFraction=1.0")
            1f
        }

        else -> {
            // Slim bar, no transition
            println("CAROUSEL_VOID: [CARD $itemIndex] SLIM BAR - widthFraction=0.0")
            0f
        }
    }

    val slimWidth = 8.dp

    println("CAROUSEL_VOID: [CARD $itemIndex] FINAL widthFraction=$widthFraction, will render: ${if (widthFraction > 0.01f) "CARD" else "SLIM BAR"}")

    if (widthFraction > 0.01f) {
        // Show as expanding/shrinking card
        println("CAROUSEL_VOID: [CARD $itemIndex] Rendering EXPANDED card with weight=$widthFraction for: ${exercise.name}")
        Card(
            modifier = Modifier
                .weight(widthFraction)
                .fillMaxHeight()
                .clickable { onSelectAlternative() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GIF - same size as original card (100dp)
                    if (exercise.gifUrl.isNotEmpty()) {
                        ExerciseGif(
                            gifPath = exercise.gifUrl,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    // Text details on the right
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${exercise.muscle} - ${exercise.parts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Difficulty: ${exercise.difficulty}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Remove button (top right) - only show when card is expanded enough
                if (widthFraction > 0.5f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable(onClick = onRemoveAlternative),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.minus_icon),
                            contentDescription = "Remove Alternative",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Show as slim bar with minimal weight (widthFraction is effectively 0)
        println("CAROUSEL_VOID: [SLIM BAR] Rendering ALTERNATIVE slim bar for: ${exercise.name} (ID: ${exercise.id})")
        Card(
            modifier = Modifier
                .weight(0.005f)  // Use minimal weight instead of fixed width
                .fillMaxHeight()
                .clickable { onSelectAlternative() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            // No content - just an empty slim bar
        }
    }
}

@Composable
private fun RowScope.TransitionAddButton(
    itemIndex: Int,
    currentIndex: Int,
    nextIndex: Int,
    transitionProgress: Float,
    offsetX: Float,
    onAddClick: () -> Unit
) {
    val isCurrentlyActive = itemIndex == currentIndex
    val isBecomingActive = itemIndex == nextIndex && itemIndex != currentIndex
    val isBecomingInactive = itemIndex == currentIndex && nextIndex != currentIndex

    val widthFraction = when {
        isCurrentlyActive && isBecomingInactive -> 1f - transitionProgress
        !isCurrentlyActive && isBecomingActive -> transitionProgress
        isCurrentlyActive && !isBecomingInactive -> 1f
        else -> 0f
    }

    val slimWidth = 8.dp

    if (widthFraction > 0.05f) {
        Card(
            modifier = Modifier
                .weight(widthFraction)
                .fillMaxHeight()
                .clickable { onAddClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (widthFraction > 0.3f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .width(slimWidth)
                .fillMaxHeight()
                .clickable { onAddClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            // Empty - just a slim bar
        }
    }
}
