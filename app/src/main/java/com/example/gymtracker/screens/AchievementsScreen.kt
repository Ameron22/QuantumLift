package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymtracker.components.AchievementCard
import com.example.gymtracker.components.BottomNavBar
import com.example.gymtracker.data.AchievementCategory
import com.example.gymtracker.data.AchievementManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController) {
    val achievementManager = remember { AchievementManager.getInstance() }
    val achievements by achievementManager.achievements.collectAsState()
    val newlyUnlocked by achievementManager.newlyUnlockedAchievements.collectAsState()
    
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
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
                            isNewlyUnlocked = newlyUnlocked.contains(achievement.id)
                        )
                    }
                }
            }
        }
    }
} 