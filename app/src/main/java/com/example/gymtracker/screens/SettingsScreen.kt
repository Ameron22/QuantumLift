package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import com.example.gymtracker.components.LoadingSpinner
import com.example.gymtracker.data.WorkoutPrivacySettings
import com.example.gymtracker.data.UpdateWorkoutPrivacySettingsRequest
import com.example.gymtracker.services.AuthRepository


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val prefs = remember { UserSettingsPreferences(context) }
    val settings by prefs.settingsFlow.collectAsState(initial = null)
    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var workTime by remember { mutableStateOf(settings?.defaultWorkTime ?: 30) }
    var breakTime by remember { mutableStateOf(settings?.defaultBreakTime ?: 60) }
    var preSetBreakTime by remember { mutableStateOf(settings?.defaultPreSetBreakTime ?: 10) }
    var soundEnabled by remember { mutableStateOf(settings?.soundEnabled ?: true) }
    var vibrationEnabled by remember { mutableStateOf(settings?.vibrationEnabled ?: true) }
    var soundVolume by remember { mutableStateOf(settings?.soundVolume ?: 0.5f) }
    var showSavedMessage by remember { mutableStateOf(false) }
    
    // Track original values to detect changes
    var originalWorkTime by remember { mutableStateOf(settings?.defaultWorkTime ?: 30) }
    var originalBreakTime by remember { mutableStateOf(settings?.defaultBreakTime ?: 60) }
    var originalPreSetBreakTime by remember { mutableStateOf(settings?.defaultPreSetBreakTime ?: 10) }
    var originalSoundEnabled by remember { mutableStateOf(settings?.soundEnabled ?: true) }
    var originalVibrationEnabled by remember { mutableStateOf(settings?.vibrationEnabled ?: true) }
    var originalSoundVolume by remember { mutableStateOf(settings?.soundVolume ?: 0.5f) }
    
    // State for number picker dialogs
    var showWorkTimePicker by remember { mutableStateOf(false) }
    var showBreakTimePicker by remember { mutableStateOf(false) }
    var showPreSetBreakTimePicker by remember { mutableStateOf(false) }
    
    // State for back confirmation dialog
    var showBackConfirmationDialog by remember { mutableStateOf(false) }
    var pendingNavigationRoute by remember { mutableStateOf<String?>(null) }
    
    // State for collapsible timer settings
    var showTimerSettings by remember { mutableStateOf(true) }
    
    // State for collapsible user settings
    var showUserSettings by remember { mutableStateOf(true) }
    
    // Privacy settings state
    var autoShareWorkouts by remember { mutableStateOf(true) }
    var defaultPrivacy by remember { mutableStateOf("FRIENDS") }
    var isLoadingPrivacySettings by remember { mutableStateOf(true) }
    var isSavingPrivacySettings by remember { mutableStateOf(false) }
    var showPrivacySuccessMessage by remember { mutableStateOf(false) }
    var privacyErrorMessage by remember { mutableStateOf<String?>(null) }
    
    val privacyOptions = listOf("PUBLIC", "FRIENDS", "PRIVATE")

    LaunchedEffect(settings) {
        settings?.let {
            workTime = it.defaultWorkTime
            breakTime = it.defaultBreakTime
            preSetBreakTime = it.defaultPreSetBreakTime ?: 10
            soundEnabled = it.soundEnabled
            vibrationEnabled = it.vibrationEnabled
            soundVolume = it.soundVolume
            originalWorkTime = it.defaultWorkTime
            originalBreakTime = it.defaultBreakTime
            originalPreSetBreakTime = it.defaultPreSetBreakTime ?: 10
            originalSoundEnabled = it.soundEnabled
            originalVibrationEnabled = it.vibrationEnabled
            originalSoundVolume = it.soundVolume
        }
    }
    
    // Load privacy settings
    LaunchedEffect(Unit) {
        try {
            val result = authRepository.getWorkoutPrivacySettings()
            result.fold(
                onSuccess = { settings ->
                    autoShareWorkouts = settings.autoShareWorkouts
                    defaultPrivacy = settings.defaultPostPrivacy
                },
                onFailure = { exception ->
                    privacyErrorMessage = "Error loading settings: ${exception.message}"
                }
            )
        } catch (e: Exception) {
            privacyErrorMessage = "Error loading settings: ${e.message}"
        } finally {
            isLoadingPrivacySettings = false
        }
    }
    
    // Auto-clear privacy success message
    LaunchedEffect(showPrivacySuccessMessage) {
        if (showPrivacySuccessMessage) {
            kotlinx.coroutines.delay(3000)
            showPrivacySuccessMessage = false
        }
    }

    // Function to check if there are any changes
    fun hasChanges(): Boolean {
        return workTime != originalWorkTime || 
               breakTime != originalBreakTime || 
               preSetBreakTime != originalPreSetBreakTime ||
               soundEnabled != originalSoundEnabled || 
               vibrationEnabled != originalVibrationEnabled ||
               soundVolume != originalSoundVolume
    }

    // Function to save settings
    fun saveSettings() {
        prefs.updateWorkTime(workTime)
        prefs.updateBreakTime(breakTime)
        prefs.updatePreSetBreakTime(preSetBreakTime)
        prefs.updateSoundEnabled(soundEnabled)
        prefs.updateVibrationEnabled(vibrationEnabled)
        prefs.updateSoundVolume(soundVolume)
        
        // Update original values after saving
        originalWorkTime = workTime
        originalBreakTime = breakTime
        originalPreSetBreakTime = preSetBreakTime
        originalSoundEnabled = soundEnabled
        originalVibrationEnabled = vibrationEnabled
        originalSoundVolume = soundVolume
        
        showSavedMessage = true
        
        // Verify the save by reading back the values
        val currentSettings = prefs.getCurrentSettings()
        Log.d("SettingsScreen", "Settings after save - Work: ${currentSettings.defaultWorkTime}, Break: ${currentSettings.defaultBreakTime}, Sound: ${currentSettings.soundEnabled}, Vibration: ${currentSettings.vibrationEnabled}, Volume: ${currentSettings.soundVolume}")
    }
    
    // Function to save privacy settings
    fun savePrivacySettings() {
        scope.launch {
            isSavingPrivacySettings = true
            privacyErrorMessage = null
            
            try {
                val request = UpdateWorkoutPrivacySettingsRequest(
                    autoShareWorkouts = autoShareWorkouts,
                    defaultPostPrivacy = defaultPrivacy
                )
                
                val result = authRepository.updateWorkoutPrivacySettings(request)
                result.fold(
                    onSuccess = {
                        showPrivacySuccessMessage = true
                    },
                    onFailure = { exception ->
                        privacyErrorMessage = "Error saving settings: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                privacyErrorMessage = "Error saving settings: ${e.message}"
            } finally {
                isSavingPrivacySettings = false
            }
        }
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
                Screen.Feed,
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
                                Screen.Feed -> Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Feed",
                                    modifier = Modifier.size(36.dp)
                                )
                                Screen.Settings -> Icon(
                                    painter = painterResource(id = R.drawable.settings_svgrepo_com),
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(26.dp)
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.People,
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
        if (authState.isLoading) {
            // Loading indicator in center of page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingSpinner(
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        } else {
            // Make the content scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp)
                    .padding(top = 32.dp)
            ) {
        
        // User Settings Container
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
                        "User Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showUserSettings = !showUserSettings }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (showUserSettings) R.drawable.minus_icon else R.drawable.plus_icon
                            ),
                            contentDescription = if (showUserSettings) "Show Less" else "Show More",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // User settings content (collapsible)
                if (showUserSettings) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Username
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Username",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Username",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = authState.user?.username ?: "Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Email
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = authState.user?.email ?: "Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Change Password
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { 
                                navController.navigate(Screen.ChangePassword.route)
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Change Password",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Change Password",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Update your account password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Privacy Settings Section
                    if (isLoadingPrivacySettings) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Privacy Settings",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Privacy Settings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Loading privacy settings...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } else {
                        // Auto-share toggle
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Auto-Share Workouts",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Automatically share completed workouts to your feed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoShareWorkouts,
                                        onCheckedChange = { autoShareWorkouts = it }
                                    )
                                }
                                
                                // Default privacy level
                                Text(
                                    text = "Default Privacy Level",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose who can see your shared workouts by default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                privacyOptions.forEach { option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = defaultPrivacy == option,
                                            onClick = { defaultPrivacy = option }
                                        )
                                        Column(
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = when (option) {
                                                    "PUBLIC" -> "Public"
                                                    "FRIENDS" -> "Friends"
                                                    "PRIVATE" -> "Private"
                                                    else -> option
                                                },
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = when (option) {
                                                    "PUBLIC" -> "Everyone can see your posts"
                                                    "FRIENDS" -> "Only your friends can see your posts"
                                                    "PRIVATE" -> "Only you can see your posts"
                                                    else -> ""
                                                },
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                // Save privacy settings button
                                Button(
                                    onClick = { savePrivacySettings() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSavingPrivacySettings
                                ) {
                                    if (isSavingPrivacySettings) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Save Privacy Settings")
                                    }
                                }
                                
                                // Privacy success message
                                if (showPrivacySuccessMessage) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                contentDescription = "Success",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Privacy settings saved successfully!",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                
                                // Privacy error message
                                privacyErrorMessage?.let { error ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Error,
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = error,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Logout Button
                    Button(
                        onClick = {
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Default Pre-Set Break Time",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { showPreSetBreakTimePicker = true },
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
                                text = "Pre-Set Break Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    preSetBreakTime / 60,
                                    preSetBreakTime % 60
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
        
        // Pre-Set Break Time Picker Dialog
        if (showPreSetBreakTimePicker) {
            AlertDialog(
                onDismissRequest = { showPreSetBreakTimePicker = false },
                title = {
                    Text(
                        "Select Pre-Set Break Time",
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
                            value = preSetBreakTime / 60,
                            range = 0..5,
                            onValueChange = { newMinutes ->
                                preSetBreakTime = (newMinutes * 60) + (preSetBreakTime % 60)
                            },
                            unit = "m"
                        )
                        NumberPicker(
                            value = preSetBreakTime % 60,
                            range = 0..59,
                            onValueChange = { newSeconds ->
                                preSetBreakTime = (preSetBreakTime / 60 * 60) + newSeconds
                            },
                            unit = "s"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPreSetBreakTimePicker = false }) {
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
}