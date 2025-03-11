package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.ui.theme.GymTrackerTheme
import com.example.gymtracker.screens.HomeScreen // Import HomeScreen
import com.example.gymtracker.screens.LoadHistoryScreen
import com.example.gymtracker.screens.LoadWorkoutScreen
import com.example.gymtracker.screens.WorkoutCreationScreen // Import WorkoutCreationScreen
import com.example.gymtracker.screens.WorkoutDetailsScreen
import com.example.gymtracker.screens.ExerciseScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
    object LoadWorkout : Screen("load_workout")
    object LoadHistory : Screen("load_history")
    object ExerciseDetails : Screen("exercise_details")
}


class MainActivity : ComponentActivity() {
    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as GymTrackerApp).database.exerciseDao())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            GymTrackerTheme {

                //creates a NavController to manage navigation
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }
                    composable(Screen.WorkoutCreation.route) {
                        WorkoutCreationScreen(navController)
                    }
                    composable(Screen.LoadWorkout.route) {
                        LoadWorkoutScreen(navController)
                    }
                    composable(Screen.LoadHistory.route) {
                        LoadHistoryScreen(navController, viewModel)
                    }
                    composable(
                        "workoutDetails/{workoutId}",
                        arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                        WorkoutDetailsScreen(workoutId = workoutId, navController = navController)
                    }
                    composable(
                        "exerciseDetails/{exerciseId}",
                        arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
                    ) {backStackEntry ->
                        val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                        ExerciseScreen(exerciseId = exerciseId, navController)
                    }

                }
            }
        }
    }
}

class HistoryViewModelFactory(private val dao: ExerciseDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
