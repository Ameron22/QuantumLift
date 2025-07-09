package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.gymtracker.data.UserSettingsPreferences
import com.example.gymtracker.classes.NumberPicker
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.res.painterResource
import com.example.gymtracker.R
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.data.AchievementManager
import com.example.gymtracker.viewmodels.AuthViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel
) {
    val context = LocalContext.current
    val prefs = remember { UserSettingsPreferences(context) }
    val settings by prefs.settingsFlow.collectAsState(initial = null)

    var workTime by remember { mutableStateOf(settings?.defaultWorkTime ?: 30) }
    var breakTime by remember { mutableStateOf(settings?.defaultBreakTime ?: 60) }
    var soundEnabled by remember { mutableStateOf(settings?.soundEnabled ?: true) }
    var vibrationEnabled by remember { mutableStateOf(settings?.vibrationEnabled ?: true) }
    var soundVolume by remember { mutableStateOf(settings?.soundVolume ?: 0.5f) }
    var showSavedMessage by remember { mutableStateOf(false) }
    
    // Track original values to detect changes
    var originalWorkTime by remember { mutableStateOf(settings?.defaultWorkTime ?: 30) }
    var originalBreakTime by remember { mutableStateOf(settings?.defaultBreakTime ?: 60) }
    var originalSoundEnabled by remember { mutableStateOf(settings?.soundEnabled ?: true) }
    var originalVibrationEnabled by remember { mutableStateOf(settings?.vibrationEnabled ?: true) }
    var originalSoundVolume by remember { mutableStateOf(settings?.soundVolume ?: 0.5f) }
    
    // State for number picker dialogs
    var showWorkTimePicker by remember { mutableStateOf(false) }
    var showBreakTimePicker by remember { mutableStateOf(false) }
    
    // State for back confirmation dialog
    var showBackConfirmationDialog by remember { mutableStateOf(false) }
    var pendingNavigationRoute by remember { mutableStateOf<String?>(null) }
    
    // State for collapsible timer settings
    var showTimerSettings by remember { mutableStateOf(true) }

    LaunchedEffect(settings) {
        settings?.let {
            workTime = it.defaultWorkTime
            breakTime = it.defaultBreakTime
            soundEnabled = it.soundEnabled
            vibrationEnabled = it.vibrationEnabled
            soundVolume = it.soundVolume
            originalWorkTime = it.defaultWorkTime
            originalBreakTime = it.defaultBreakTime
            originalSoundEnabled = it.soundEnabled
            originalVibrationEnabled = it.vibrationEnabled
            originalSoundVolume = it.soundVolume
        }
    }

    // Function to check if there are any changes
    fun hasChanges(): Boolean {
        return workTime != originalWorkTime || 
               breakTime != originalBreakTime || 
               soundEnabled != originalSoundEnabled || 
               vibrationEnabled != originalVibrationEnabled ||
               soundVolume != originalSoundVolume
    }

    // Function to save settings
    fun saveSettings() {
        prefs.updateWorkTime(workTime)
        prefs.updateBreakTime(breakTime)
        prefs.updateSoundEnabled(soundEnabled)
        prefs.updateVibrationEnabled(vibrationEnabled)
        prefs.updateSoundVolume(soundVolume)
        
        // Update original values after saving
        originalWorkTime = workTime
        originalBreakTime = breakTime
        originalSoundEnabled = soundEnabled
        originalVibrationEnabled = vibrationEnabled
        originalSoundVolume = soundVolume
        
        showSavedMessage = true
        
        // Verify the save by reading back the values
        val currentSettings = prefs.getCurrentSettings()
        Log.d("SettingsScreen", "Settings after save - Work: ${currentSettings.defaultWorkTime}, Break: ${currentSettings.defaultBreakTime}, Sound: ${currentSettings.soundEnabled}, Vibration: ${currentSettings.vibrationEnabled}, Volume: ${currentSettings.soundVolume}")
    }

    // Function to handle navigation with change detection
    fun handleNavigation(route: String) {
        if (hasChanges()) {
            pendingNavigationRoute = route
            showBackConfirmationDialog = true
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    WorkoutIndicator(generalViewModel = generalViewModel, navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = { 
            // Custom bottom navigation that checks for changes
            val items = listOf(
                Screen.Home,
                Screen.LoadWorkout,
                Screen.LoadHistory,
                Screen.Achievements,
                Screen.Settings
            )
            
            NavigationBar(
                modifier = Modifier.height(72.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            when(screen) {
                                Screen.Home -> Icon(
                                    painter = painterResource(id = R.drawable.home_icon),
                                    contentDescription = "Home",
                                    modifier = Modifier.size(36.dp)
                                )
                                Screen.LoadWorkout -> Icon(
                                    painter = painterResource(id = R.drawable.dumbell_icon2),
                                    contentDescription = "Workouts",
                                    modifier = Modifier.size(44.dp)
                                )
                                Screen.LoadHistory -> Icon(
                                    painter = painterResource(id = R.drawable.history_icon),
                                    contentDescription = "History",
                                    modifier = Modifier.size(36.dp)
                                )
                                Screen.Achievements -> Icon(
                                    painter = painterResource(id = R.drawable.trophy),
                                    contentDescription = "Achievements",
                                    modifier = Modifier.size(40.dp)
                                )
                                Screen.Settings -> Icon(
                                    painter = painterResource(id = R.drawable.settings_svgrepo_com),
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(26.dp)
                                )
                                else -> Icon(
                                    painter = painterResource(id = R.drawable.food_icon),
                                    contentDescription = screen.route,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        label = { },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                handleNavigation(screen.route)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .padding(top = 32.dp)) {
        Text(
            "Settings", 
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Timer Settings Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with title and toggle button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Timer Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showTimerSettings = !showTimerSettings }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (showTimerSettings) R.drawable.minus_icon else R.drawable.plus_icon
                            ),
                            contentDescription = if (showTimerSettings) "Show Less" else "Show More",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Timer settings content (collapsible)
                if (showTimerSettings) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Default Work Time",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { showWorkTimePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Work Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    workTime / 60,
                                    workTime % 60
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Default Break Time",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { showBreakTimePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Break Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    breakTime / 60,
                                    breakTime % 60
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sound and Vibration Settings
                    Text(
                        "Ring effects:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sound Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Checkbox(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Vibration Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Checkbox(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Sound Volume Setting
                    if (soundEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sound Volume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = soundVolume,
                                onValueChange = { soundVolume = it },
                                valueRange = 0f..1f,
                                steps = 9,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(soundVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Check if changes have been made
        val hasChanges = hasChanges()
        
        Button(
            onClick = { saveSettings() },
            enabled = hasChanges,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (hasChanges) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Save")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test Achievement Notification Button
        Button(
            onClick = { 
                val achievementManager = AchievementManager.getInstance()
                achievementManager.showMultipleAchievementsNotification()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Test Achievement Notification")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test Single Achievement Notification Button
        Button(
            onClick = { 
                val achievementManager = AchievementManager.getInstance()
                achievementManager.notificationService.showAchievementNotification("first_workout")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text("Test Single Achievement")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test Achievement Unlock Button
        Button(
            onClick = { 
                val achievementManager = AchievementManager.getInstance()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    achievementManager.testAchievementUnlock("workout_warrior")
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text("Test Achievement Unlock")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logout Button
        Button(
            onClick = { 
                // Create AuthViewModel and logout
                val authViewModel = AuthViewModel(context)
                authViewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
        
        // Work Time Picker Dialog
        if (showWorkTimePicker) {
            AlertDialog(
                onDismissRequest = { showWorkTimePicker = false },
                title = {
                    Text(
                        "Select Work Time",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .width(260.dp)
                    ) {
                        NumberPicker(
                            value = workTime / 60,
                            range = 0..5,
                            onValueChange = { newMinutes ->
                                workTime = (newMinutes * 60) + (workTime % 60)
                            },
                            unit = "m"
                        )
                        NumberPicker(
                            value = workTime % 60,
                            range = 0..59,
                            onValueChange = { newSeconds ->
                                workTime = (workTime / 60 * 60) + newSeconds
                            },
                            unit = "s"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWorkTimePicker = false }) {
                        Text("OK")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.background(Color.Transparent)
            )
        }
        
        // Break Time Picker Dialog
        if (showBreakTimePicker) {
            AlertDialog(
                onDismissRequest = { showBreakTimePicker = false },
                title = {
                    Text(
                        "Select Break Time",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .width(260.dp)
                    ) {
                        NumberPicker(
                            value = breakTime / 60,
                            range = 0..5,
                            onValueChange = { newMinutes ->
                                breakTime = (newMinutes * 60) + (breakTime % 60)
                            },
                            unit = "m"
                        )
                        NumberPicker(
                            value = breakTime % 60,
                            range = 0..59,
                            onValueChange = { newSeconds ->
                                breakTime = (breakTime / 60 * 60) + newSeconds
                            },
                            unit = "s"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBreakTimePicker = false }) {
                        Text("OK")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.background(Color.Transparent)
            )
        }
    }
    
    // Handle back navigation
    BackHandler {
        if (hasChanges()) {
            showBackConfirmationDialog = true
        } else {
            navController.popBackStack()
        }
    }
    
    // Back confirmation dialog
    if (showBackConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showBackConfirmationDialog = false
                pendingNavigationRoute = null
            },
            title = { Text("Save Changes?") },
            text = { Text("Do you want to save your changes before leaving?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveSettings()
                        showBackConfirmationDialog = false
                        
                        // Navigate based on pending route or go back
                        if (pendingNavigationRoute != null) {
                            navController.navigate(pendingNavigationRoute!!) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            pendingNavigationRoute = null
                        } else {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackConfirmationDialog = false
                        
                        // Navigate based on pending route or go back
                        if (pendingNavigationRoute != null) {
                            navController.navigate(pendingNavigationRoute!!) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            pendingNavigationRoute = null
                        } else {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Don't Save")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    }
} 