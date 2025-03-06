package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymtracker.ui.theme.GymTrackerTheme
import com.example.gymtracker.screens.HomeScreen // Import HomeScreen
import com.example.gymtracker.screens.WorkoutCreationScreen // Import WorkoutCreationScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
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
                }
            }
        }
    }
}