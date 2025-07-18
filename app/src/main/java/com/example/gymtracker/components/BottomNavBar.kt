package com.example.gymtracker.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymtracker.R
import com.example.gymtracker.navigation.Screen
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.LoadWorkout,
        Screen.LoadHistory,
        Screen.Feed,
        Screen.Achievements,
        Screen.Settings
    )

    NavigationBar(
        modifier = Modifier.height(72.dp) // Increased height to prevent selection box cutoff
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
                            modifier = Modifier.size(44.dp) // Slightly bigger dumbbell icon
                        )
                        Screen.LoadHistory -> Icon(
                            painter = painterResource(id = R.drawable.history_icon),
                            contentDescription = "History",
                            modifier = Modifier.size(36.dp) // Smaller history icon
                        )
                        Screen.Achievements -> Icon(
                            painter = painterResource(id = R.drawable.trophy),
                            contentDescription = "Achievements",
                            modifier = Modifier.size(40.dp) // Standard icon size
                        )
                        Screen.Settings -> Icon(
                            painter = painterResource(id = R.drawable.settings_svgrepo_com),
                            contentDescription = "Settings",
                            modifier = Modifier.size(26.dp)
                        )
                        Screen.Feed -> Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Feed",
                            modifier = Modifier.size(36.dp)
                        )
                        else -> Icon(
                            painter = painterResource(id = R.drawable.food_icon),
                            contentDescription = screen.route,
                            modifier = Modifier.size(40.dp) // Standard icon size
                        )
                    }
                },
                label = { }, // Empty label to remove text
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
} 