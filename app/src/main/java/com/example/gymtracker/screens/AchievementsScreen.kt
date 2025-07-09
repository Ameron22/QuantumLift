package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.components.AchievementCard
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.components.WorkoutIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.data.AchievementCategory
import com.example.gymtracker.data.AchievementManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    generalViewModel: GeneralViewModel
) {
    val achievementManager = remember { AchievementManager.getInstance() }
    val achievements by achievementManager.achievements.collectAsState()
    val newlyUnlocked by achievementManager.newlyUnlockedAchievements.collectAsState()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                actions = {
                    // Reset button for development/testing
                    IconButton(
                        onClick = {
                            // Reset all achievements to locked state
                            scope.launch {
                                achievementManager.resetAllAchievements()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = "Reset Achievements",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    WorkoutIndicator(generalViewModel = generalViewModel, navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group achievements by category
            val groupedAchievements = achievements.groupBy { it.category }
            
            // Display each category
            AchievementCategory.values().forEach { category ->
                val achievementsInCategory = groupedAchievements[category].orEmpty()
                if (achievementsInCategory.isNotEmpty()) {
                    item {
                        // Category Header
                        Text(
                            text = when (category) {
                                AchievementCategory.WORKOUT_MILESTONES -> "💪 Workout Milestones"
                                AchievementCategory.STRENGTH_GOALS -> "🏋️‍♂️ Strength Goals"
                                AchievementCategory.CONSISTENCY_AWARDS -> "🎯 Consistency Awards"
                                AchievementCategory.SPECIAL_CHALLENGES -> "🌟 Special Challenges"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(achievementsInCategory) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            isNewlyUnlocked = newlyUnlocked.contains(achievement.id),
                            onAchievementClicked = {
                                // Clear the newly unlocked state for this specific achievement
                                achievementManager.clearSpecificNewlyUnlockedAchievement(achievement.id)
                            }
                        )
                    }
                }
            }
        }
    }
} 