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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick action cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


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







