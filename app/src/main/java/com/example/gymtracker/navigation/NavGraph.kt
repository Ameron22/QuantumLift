package com.example.gymtracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gymtracker.screens.AddExerciseToWorkoutScreen
import com.example.gymtracker.screens.HomeScreen
import com.example.gymtracker.screens.WorkoutDetailsScreen
import com.example.gymtracker.screens.ExerciseScreen
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(
            route = Screen.WorkoutDetails.route,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
            WorkoutDetailsScreen(
                workoutId = workoutId,
                navController = navController
            )
        }
        
        composable(
            route = Screen.Exercise.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.IntType },
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("workoutId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
            ExerciseScreen(
                exerciseId = exerciseId,
                workoutSessionId = sessionId,
                workoutId = workoutId,
                navController = navController
            )
        }
        
        composable(
            route = Screen.AddExerciseToWorkout.route,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
            AddExerciseToWorkoutScreen(
                workoutId = workoutId,
                navController = navController,
                detailsViewModel = viewModel()
            )
        }
    }
} 