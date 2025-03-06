package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymtracker.ui.theme.GymTrackerTheme
import com.example.gymtracker.screens.HomeScreen // Import HomeScreen
import com.example.gymtracker.screens.LoadWorkoutScreen
import com.example.gymtracker.screens.WorkoutCreationScreen // Import WorkoutCreationScreen
import com.example.gymtracker.screens.WorkoutDetailsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
    object LoadWorkout : Screen("load_workout")
    object SeeWorkout : Screen("see_workout")
}


class MainActivity : ComponentActivity() {

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
                    composable(
                        "workoutDetails/{workoutId}",
                        arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                        WorkoutDetailsScreen(workoutId = workoutId, navController = navController)
                    }
                }
            }
        }
    }
}
