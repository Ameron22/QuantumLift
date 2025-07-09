package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.example.gymtracker.screens.LoginScreen
import com.example.gymtracker.screens.RegisterScreen
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.utils.ExerciseDataImporter
import com.example.gymtracker.viewmodels.WorkoutDetailsViewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.viewmodels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.gymtracker.utils.FloatingTimerManager

class MainActivity : ComponentActivity(), LifecycleObserver {
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
    
    // Navigation state for floating timer
    private var navigationExerciseId = 0
    private var navigationSessionId = 0L
    private var navigationWorkoutId = 0
    private var shouldNavigateToExercise = false

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
    
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPause() {
        Log.d("MainActivity", "App paused - hiding delete zone")
        FloatingTimerManager.hideDeleteZone(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppStop() {
        Log.d("MainActivity", "App stopped - hiding delete zone")
        FloatingTimerManager.hideDeleteZone(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }
    
    private fun handleNavigationIntent(intent: Intent?) {
        intent?.let { intentData ->
            if (intentData.getBooleanExtra("from_floating_timer", false)) {
                Log.d("MainActivity", "App brought to foreground from floating timer")
                // Just bring the app to foreground, no specific navigation needed
            }
            
            // Handle achievement notification intent
            if (intentData.getBooleanExtra("open_achievements", false)) {
                Log.d("MainActivity", "Opening achievements screen from notification")
                // This will be handled in the Compose UI to navigate to achievements
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Add lifecycle observer to hide delete zone when app goes to background
        lifecycle.addObserver(this)
        
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
        
        // Handle navigation intent from floating timer
        handleNavigationIntent(intent)

        setContent {
            QuantumLiftTheme {
                GradientBackground {
                    val navController = rememberNavController()
                    // Create shared ViewModel instances
                    val workoutDetailsViewModel = viewModel<WorkoutDetailsViewModel>()
                    val generalViewModel = viewModel<GeneralViewModel>()
                    val authViewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return AuthViewModel(applicationContext) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    })

                    // Handle navigation from floating timer and achievement notifications
                    LaunchedEffect(Unit) {
                        // Check if we should open achievements from notification
                        if (intent?.getBooleanExtra("open_achievements", false) == true) {
                            navController.navigate(Screen.Achievements.route)
                        }
                    }

                    // Check authentication state and navigate accordingly
                    val authState by authViewModel.authState.collectAsState()
                    LaunchedEffect(authState.isLoggedIn) {
                        if (!authState.isLoggedIn) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController, authViewModel)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController, authViewModel)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController, generalViewModel)
                        }
                        composable(Screen.LoadWorkout.route) {
                            LoadWorkoutScreen(navController, generalViewModel)
                        }
                        composable(Screen.LoadHistory.route) {
                            LoadHistoryScreen(navController, viewModel, generalViewModel)
                        }
                        composable(Screen.Achievements.route) {
                            AchievementsScreen(navController, generalViewModel)
                        }
                        composable(Screen.CreateExercise.route) {
                            CreateExerciseScreen(navController)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController, generalViewModel)
                        }
                        composable(
                            Screen.Routes.WORKOUT_DETAILS,
                            arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                            WorkoutDetailsScreen(
                                workoutId = workoutId, 
                                navController = navController,
                                viewModel = workoutDetailsViewModel,
                                generalViewModel = generalViewModel
                            )
                        }
                        composable(
                            Screen.Exercise.route,
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("workoutId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                            val workoutSessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                            ExerciseScreen(
                                exerciseId = exerciseId,
                                workoutSessionId = workoutSessionId,
                                workoutId = workoutId,
                                navController = navController,
                                viewModel = workoutDetailsViewModel,
                                generalViewModel = generalViewModel
                            )
                        }
                        composable(
                            Screen.AddExerciseToWorkout.route,
                            arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                            AddExerciseToWorkoutScreen(
                                workoutId = workoutId, 
                                navController = navController,
                                detailsViewModel = workoutDetailsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
