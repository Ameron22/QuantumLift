package com.example.gymtracker.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.data.AppDatabase
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.viewmodels.AuthViewModel
import com.example.gymtracker.viewmodels.PhysicalParametersViewModel
import com.example.gymtracker.data.XPSystem
import com.example.gymtracker.data.UserXP
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import android.util.Log


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.exerciseDao() }
    val physicalParametersDao = remember { db.physicalParametersDao() }
    val physicalParametersViewModel = remember { PhysicalParametersViewModel(physicalParametersDao) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Welcome", "Body")

    // Load physical parameters when Body tab is selected
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            Log.d("HomeScreen", "Body tab selected, loading physical parameters")
            physicalParametersViewModel.debugCheckTable() // Debug table existence
            physicalParametersViewModel.loadPhysicalParameters("current_user")
            physicalParametersViewModel.loadAllBodyMeasurements("current_user")
        }
    }

    Scaffold(
        topBar = {
            Column {
            TopAppBar(
                title = { 
                    Text(
                        text = "Quantum Lift",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    WorkoutIndicator(generalViewModel = generalViewModel, navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
                
                // Tab bar under TopAppBar
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { 
                                Log.d("HomeScreen", "Tab clicked: $title (index: $index)")
                                selectedTabIndex = index 
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        // Tab Content
        when (selectedTabIndex) {
            0 -> WelcomeTab(paddingValues = paddingValues)
            1 -> {
                Log.d("HomeScreen", "Rendering BodyScreen")
                BodyScreen(navController = navController, viewModel = physicalParametersViewModel, paddingValues = paddingValues)
            }
        }
    }
}

@Composable
fun WelcomeTab(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val xpSystem = remember { XPSystem(db.userXPDao()) }
    val userId = "current_user" // Default user ID
    
    var userXP by remember { mutableStateOf<UserXP?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load user XP data
    LaunchedEffect(Unit) {
        try {
            userXP = xpSystem.getUserXP(userId)
            isLoading = false
        } catch (e: Exception) {
            Log.e("WelcomeTab", "Error loading user XP: ${e.message}")
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // XP Display Card
        if (!isLoading && userXP != null) {
            XPDisplayCard(userXP = userXP!!, xpSystem = xpSystem)
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Show default welcome for new users
            Text(
                text = "Welcome to Quantum Lift",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your journey to fitness starts here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick action cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add quick action cards here in the future
        }
    }
}

@Composable
fun XPDisplayCard(userXP: UserXP, xpSystem: XPSystem) {
    val levelTitle = xpSystem.getLevelTitle(userXP.currentLevel)
    val progressPercentage = if (userXP.xpToNextLevel > 0) {
        val currentLevelXP = when {
            userXP.currentLevel <= 10 -> (userXP.currentLevel - 1) * XPSystem.XP_LEVEL_1_10
            userXP.currentLevel <= 25 -> 1000 + (userXP.currentLevel - 11) * XPSystem.XP_LEVEL_11_25
            userXP.currentLevel <= 50 -> 5000 + (userXP.currentLevel - 26) * XPSystem.XP_LEVEL_26_50
            userXP.currentLevel <= 75 -> 15000 + (userXP.currentLevel - 51) * XPSystem.XP_LEVEL_51_75
            else -> 30000 + (userXP.currentLevel - 76) * XPSystem.XP_LEVEL_76_100
        }
        val levelTotalXP = when {
            userXP.currentLevel < 10 -> userXP.currentLevel * XPSystem.XP_LEVEL_1_10
            userXP.currentLevel < 25 -> 1000 + (userXP.currentLevel - 10) * XPSystem.XP_LEVEL_11_25
            userXP.currentLevel < 50 -> 5000 + (userXP.currentLevel - 25) * XPSystem.XP_LEVEL_26_50
            userXP.currentLevel < 75 -> 15000 + (userXP.currentLevel - 50) * XPSystem.XP_LEVEL_51_75
            else -> 30000 + (userXP.currentLevel - 75) * XPSystem.XP_LEVEL_76_100
        }
        val levelProgress = userXP.totalXP - currentLevelXP
        val levelTotal = levelTotalXP - currentLevelXP
        if (levelTotal > 0) (levelProgress.toFloat() / levelTotal) else 1f
    } else 1f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level and Title
            Text(
                text = "Level ${userXP.currentLevel}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = levelTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // XP Progress
            Text(
                text = "${userXP.totalXP} XP",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            
            if (userXP.xpToNextLevel > 0) {
                Text(
                    text = "${userXP.xpToNextLevel} XP to next level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}







