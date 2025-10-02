package com.example.gymtracker.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.WorkoutExercise
import kotlin.math.abs

@Composable
fun SwipeableExerciseCard(
    exercise: EntityExercise,
    workoutExercise: WorkoutExercise,
    hasAlternatives: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isSwipeInProgress by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .zIndex(if (isSwipeInProgress) 1000f else 0f)
    ) {
        // Background actions (only show if has alternatives)
        if (hasAlternatives) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left swipe action (Alternatives)
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Alternatives",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                        )
                    }
                }
                
                // Right swipe action (could be used for other actions)
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Options",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                        )
                    }
                }
            }
        }
        
        // Main card content
        Card(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { _ ->
                            isSwipeInProgress = true
                        },
                        onDragEnd = {
                            when {
                                offsetX > swipeThreshold -> {
                                    // Swiped right
                                    onSwipeRight()
                                }
                                offsetX < -swipeThreshold -> {
                                    // Swiped left
                                    onSwipeLeft()
                                }
                            }
                            
                            // Reset position
                            offsetX = 0f
                            isSwipeInProgress = false
                        },
                        onDrag = { _, dragAmount ->
                            // Only allow horizontal swiping
                            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                offsetX = (offsetX + dragAmount.x).coerceIn(-maxSwipeDistance, maxSwipeDistance)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSwipeInProgress) 
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

