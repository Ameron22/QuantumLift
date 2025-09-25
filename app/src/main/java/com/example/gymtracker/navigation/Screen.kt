package com.example.gymtracker.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object LoadWorkout : Screen("load_workout")
    object LoadHistory : Screen("load_history")
    object Achievements : Screen("achievements")
    object CreateExercise : Screen("create_exercise")
    object WorkoutDetails : Screen("workout_details/{workoutId}") {
        fun createRoute(workoutId: Int) = "workout_details/$workoutId"
    }
    object Exercise : Screen("exercise/{exerciseId}/{sessionId}/{workoutId}") {
        fun createRoute(exerciseId: Int, sessionId: Long, workoutId: Int) = "exercise/$exerciseId/$sessionId/$workoutId"
    }
    object AddExerciseToWorkout : Screen("addExerciseToWorkout/{workoutId}") {
        fun createRoute(workoutId: Int) = "addExerciseToWorkout/$workoutId"
    }
    object CreateWarmUp : Screen("create_warm_up")
    object Settings : Screen("settings")
    object ChangePassword : Screen("change_password")
    object Feed : Screen("feed")
    object AddMeasurement : Screen("add_measurement")
    
    // Helper object for parameterized routes
    object Routes {
        const val WORKOUT_DETAILS = "workoutDetails/{workoutId}"
        const val EXERCISE_DETAILS = "exerciseDetails/{exerciseId}/{workoutSessionId}"
        
        fun workoutDetails(workoutId: Int) = "workoutDetails/$workoutId"
        fun exerciseDetails(exerciseId: Int, workoutSessionId: Long) = "exerciseDetails/$exerciseId/$workoutSessionId"
    }
} 