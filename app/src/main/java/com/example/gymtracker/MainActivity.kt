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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.gymtracker.viewmodels.PhysicalParametersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.gymtracker.services.TimerService
import com.example.gymtracker.components.TopPermissionBanner
import com.example.gymtracker.data.UserSettingsPreferences

data class NotificationNavigation(
    val exerciseId: Int,
    val sessionId: Long,
    val workoutId: Int
)

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

    // Navigation state for notification
    private var navigationFromNotification: NotificationNavigation? = null

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

    private fun hideDeleteZone() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = "HIDE_DELETE_ZONE"
        }
        startService(intent)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPause() {
        Log.d("MainActivity", "App paused - hiding delete zone")
        hideDeleteZone()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppStop() {
        Log.d("MainActivity", "App stopped - hiding delete zone")
        hideDeleteZone()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResume() {
        Log.d("MainActivity", "App resumed - checking token refresh")
        // Try to refresh token when app comes back to foreground
        // Note: This will be handled in the Compose content where we have access to the ViewModel
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

            // Handle timer notification intent
            if (intentData.getBooleanExtra("from_notification", false)) {
                Log.d("MainActivity", "App opened from timer notification")
                navigationFromNotification = NotificationNavigation(
                    exerciseId = intentData.getIntExtra("exercise_id", 0),
                    sessionId = intentData.getLongExtra("session_id", 0L),
                    workoutId = intentData.getIntExtra("workout_id", 0)
                )
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
                    val authViewModel: AuthViewModel =
                        viewModel(factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                    @Suppress("UNCHECKED_CAST")
                                    return AuthViewModel(applicationContext) as T
                                }
                                throw IllegalArgumentException("Unknown ViewModel class")
                            }
                        })

                    // Permission request logic
                    val userSettings = remember { UserSettingsPreferences(applicationContext) }
                    val settings by userSettings.settingsFlow.collectAsState(initial = null)
                    var showPermissionBanner by remember { mutableStateOf(false) }

                    // Check if we should show permission banner for first-time users
                    LaunchedEffect(settings) {
                        settings?.let { userSettingsData ->
                            if (!userSettingsData.notificationPermissionRequested) {
                                showPermissionBanner = true
                            }
                        }
                    }

                    // Handle navigation from floating timer and achievement notifications
                    LaunchedEffect(Unit) {
                        // Check if we should open achievements from notification
                        if (intent?.getBooleanExtra("open_achievements", false) == true) {
                            navController.navigate(Screen.Achievements.route)
                        }

                        // Check if we should navigate to exercise from timer notification
                        navigationFromNotification?.let { navData ->
                            Log.d(
                                "MainActivity",
                                "Navigating to exercise from notification: exerciseId=${navData.exerciseId}, sessionId=${navData.sessionId}, workoutId=${navData.workoutId}"
                            )
                            navController.navigate("exercise/${navData.exerciseId}/${navData.sessionId}/${navData.workoutId}") {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                            navigationFromNotification = null // Clear after navigation
                        }
                        
                        // Try to refresh token when MainActivity starts
                        authViewModel.refreshTokenIfNeeded()
                    }

                                        // MainActivity only shows content for authenticated users
                    // Authentication is handled by SplashActivity

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Permission banner at the top
                            if (showPermissionBanner) {
                                TopPermissionBanner(
                                    onDismiss = {
                                        showPermissionBanner = false
                                        userSettings.updateNotificationPermissionRequested(true)
                                    },
                                    onPermissionGranted = {
                                        showPermissionBanner = false
                                        userSettings.updateNotificationPermissionRequested(true)
                                    }
                                )
                            }

                                                         NavHost(
                                 navController = navController,
                                 startDestination = Screen.Home.route // MainActivity is only for authenticated users
                                                          ) {
                                                                 composable(Screen.Home.route) {
                                     HomeScreen(navController, generalViewModel, authViewModel)
                                 }
                                                                 composable(Screen.LoadWorkout.route) {
                                     LoadWorkoutScreen(navController, generalViewModel)
                                 }
                                                                 composable(Screen.LoadHistory.route) {
                                     LoadHistoryScreen(
                                         navController,
                                         viewModel,
                                         generalViewModel
                                     )
                                 }
                                                                 composable(Screen.Achievements.route) {
                                     AchievementsScreen(navController, generalViewModel)
                                 }
                                                                 composable(Screen.CreateExercise.route) {
                                     CreateExerciseScreen(navController)
                                 }
                                                                 composable(Screen.CreateWarmUp.route) {
                                     CreateWarmUpScreen(navController)
                                 }
                                                                 composable(Screen.Settings.route) {
                                     SettingsScreen(
                                         navController,
                                         generalViewModel,
                                         authViewModel
                                     )
                                 }
                                                                 composable(Screen.ChangePassword.route) {
                                     ChangePasswordScreen(navController, authViewModel)
                                 }
                                                                 composable(Screen.Feed.route) {
                                     FeedScreen(navController, authViewModel)
                                 }
                                                                 composable(Screen.AddMeasurement.route) {
                                     val database = AppDatabase.getDatabase(applicationContext)
                                     val physicalParametersDao = database.physicalParametersDao()
                                     val physicalParametersViewModel =
                                         PhysicalParametersViewModel(physicalParametersDao)
                                     AddMeasurementScreen(
                                         navController,
                                         physicalParametersViewModel
                                     )
                                 }
                                                                 composable(
                                     Screen.Routes.WORKOUT_DETAILS,
                                     arguments = listOf(navArgument("workoutId") {
                                         type = NavType.IntType
                                     })
                                 ) { backStackEntry ->
                                     val workoutId =
                                         backStackEntry.arguments?.getInt("workoutId") ?: 0
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
                                     val exerciseId =
                                         backStackEntry.arguments?.getInt("exerciseId") ?: 0
                                     val workoutSessionId =
                                         backStackEntry.arguments?.getLong("sessionId") ?: 0L
                                     val workoutId =
                                         backStackEntry.arguments?.getInt("workoutId") ?: 0
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
                                     arguments = listOf(navArgument("workoutId") {
                                         type = NavType.IntType
                                     })
                                 ) { backStackEntry ->
                                     val workoutId =
                                         backStackEntry.arguments?.getInt("workoutId") ?: 0
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
    }


