package com.example.gymtracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.ui.theme.QuantumLiftTheme
import com.example.gymtracker.ui.theme.GradientBackground
import com.example.gymtracker.screens.*
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.utils.ExerciseDataImporter
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return HistoryViewModel((application as QuantumLiftApp).database.exerciseDao()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    private fun checkAchievements(dao: ExerciseDao) {
        val achievementManager = AchievementManager.getInstance()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check total workout count
                val totalWorkouts = dao.getTotalWorkoutCount()
                achievementManager.updateWorkoutCount(totalWorkouts)

                // Check workout streak
                val streak = calculateWorkoutStreak(dao)
                achievementManager.updateConsistencyStreak(streak)

                // Check strength milestones - only if there are exercises
                val maxBenchPress = dao.getMaxWeightForExercise("Bench Press")
                if (maxBenchPress != null && maxBenchPress > 0) {
                    achievementManager.updateStrengthProgress("Bench Press", maxBenchPress)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking achievements", e)
            }
        }
    }

    private suspend fun calculateWorkoutStreak(dao: ExerciseDao): Int {
        val workoutDates = dao.getWorkoutDates()
        if (workoutDates.isEmpty()) return 0

        var streak = 1
        var currentDate = workoutDates.first()

        for (i in 1 until workoutDates.size) {
            val nextDate = workoutDates[i]
            val daysBetween = TimeUnit.MILLISECONDS.toDays(currentDate - nextDate)
            
            if (daysBetween == 1L) {
                streak++
                currentDate = nextDate
            } else {
                break
            }
        }

        return streak
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AchievementManager
        AchievementManager.initialize(applicationContext)

        // Initialize database and DAO
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.exerciseDao()

        // Import exercises and check achievements
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "Starting exercise import")
                val importer = ExerciseDataImporter(applicationContext, dao)
                importer.importExercises()
                Log.d("MainActivity", "Exercise import completed")
                checkAchievements(dao)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during initialization", e)
            }
        }

        setContent {
            QuantumLiftTheme {
                GradientBackground {
                    val navController = rememberNavController()
                    // Create a shared ViewModel instance
                    val workoutDetailsViewModel = viewModel<WorkoutDetailsViewModel>()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(navController)
                        }
                        composable(Screen.LoadWorkout.route) {
                            LoadWorkoutScreen(navController)
                        }
                        composable(Screen.LoadHistory.route) {
                            LoadHistoryScreen(navController, viewModel)
                        }
                        composable(Screen.Achievements.route) {
                            AchievementsScreen(navController)
                        }
                        composable(Screen.WorkoutCreation.route) {
                            WorkoutCreationScreen(navController)
                        }
                        composable(Screen.CreateExercise.route) {
                            CreateExerciseScreen(navController)
                        }
                        composable(
                            Screen.Routes.WORKOUT_DETAILS,
                            arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                            WorkoutDetailsScreen(
                                workoutId = workoutId, 
                                navController = navController,
                                viewModel = workoutDetailsViewModel
                            )
                        }
                        composable(
                            Screen.Exercise.route,
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                            val workoutSessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            ExerciseScreen(
                                exerciseId = exerciseId,
                                workoutSessionId = workoutSessionId,
                                navController = navController,
                                viewModel = workoutDetailsViewModel
                            )
                        }
                        composable(
                            Screen.AddExerciseToWorkout.route,
                            arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                            AddExerciseToWorkoutScreen(workoutId = workoutId, navController = navController)
                        }
                    }
                }
            }
        }
    }
}
