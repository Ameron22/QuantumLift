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
            Text(
                text = if (isClickable) "$workoutName →" else workoutName,
                style = MaterialTheme.typography.titleMedium, // Bigger text style
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
} 