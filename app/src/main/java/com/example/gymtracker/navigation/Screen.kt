package com.example.gymtracker.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WorkoutCreation : Screen("workout_creation")
    object LoadWorkout : Screen("load_workout")
    object LoadHistory : Screen("load_history")
    object ExerciseDetails : Screen("exercise_details")
    object Achievements : Screen("achievements")
    
    // Helper object for parameterized routes
    object Routes {
        const val WORKOUT_DETAILS = "workoutDetails/{workoutId}"
        const val EXERCISE_DETAILS = "exerciseDetails/{exerciseId}/{workoutSessionId}"
        
        fun workoutDetails(workoutId: Int) = "workoutDetails/$workoutId"
        fun exerciseDetails(exerciseId: Int, workoutSessionId: Long) = "exerciseDetails/$exerciseId/$workoutSessionId"
    }
} 