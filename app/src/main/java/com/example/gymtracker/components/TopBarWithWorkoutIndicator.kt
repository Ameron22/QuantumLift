package com.example.gymtracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.navigation.Screen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.*

// Scrolling text composable for long workout names
@Composable
fun ScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.White,
    maxWidth: androidx.compose.ui.unit.Dp = 120.dp
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

@Composable
fun WorkoutIndicator(
    generalViewModel: GeneralViewModel,
    navController: NavController? = null
) {
    val currentWorkout by generalViewModel.currentWorkout.collectAsState()
    val isWorkoutActive = currentWorkout?.isActive == true
    val workoutName = currentWorkout?.workoutName ?: ""

    // Debug logging
    android.util.Log.d("WorkoutIndicator", "currentWorkout: $currentWorkout")
    android.util.Log.d("WorkoutIndicator", "isWorkoutActive: $isWorkoutActive")
    android.util.Log.d("WorkoutIndicator", "workoutName: '$workoutName'")
    android.util.Log.d("WorkoutIndicator", "workoutId: ${currentWorkout?.workoutId}")
    android.util.Log.d("WorkoutIndicator", "Should show indicator: ${isWorkoutActive && workoutName.isNotEmpty()}")
    android.util.Log.d("WorkoutIndicator", "navController provided: ${navController != null}")

    // Workout indicator on the right
    if (isWorkoutActive && workoutName.isNotEmpty()) {
        val isClickable = navController != null
        val scale by animateFloatAsState(
            targetValue = if (isClickable) 1.0f else 1.0f,
            animationSpec = tween(200),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .background(
                    color = Color(0xFF2E7D32), // Darker green color
                    shape = RoundedCornerShape(12.dp) // Bigger corner radius
                )
                .scale(scale)
                .clickable(enabled = isClickable) {
                    android.util.Log.d("WorkoutIndicator", "Indicator clicked!")
                    navController?.let { nav ->
                        // Navigate to the active workout details screen
                        val workoutId = currentWorkout?.workoutId
                        android.util.Log.d("WorkoutIndicator", "Attempting navigation to workout ID: $workoutId")
                        if (workoutId != null && workoutId > 0) {
                            try {
                                val route = Screen.Routes.workoutDetails(workoutId)
                                android.util.Log.d("WorkoutIndicator", "Navigating to route: $route")
                                nav.navigate(route)
                            } catch (e: Exception) {
                                android.util.Log.e("WorkoutIndicator", "Navigation error: ${e.message}")
                                e.printStackTrace()
                            }
                        } else {
                            android.util.Log.e("WorkoutIndicator", "Invalid workout ID: $workoutId")
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp) // Bigger padding
        ) {
            ScrollingText(
                text = if (isClickable) "$workoutName →" else workoutName,
                style = MaterialTheme.typography.titleMedium, // Bigger text style
                color = Color.White,
                maxWidth = 150.dp // Adjust max width for the indicator
            )
        }
    }
} 