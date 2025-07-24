package com.example.gymtracker.screens
import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.gymtracker.viewmodels.GeneralViewModel
import com.example.gymtracker.viewmodels.AuthViewModel
import com.example.gymtracker.viewmodels.PhysicalParametersViewModel
import com.example.gymtracker.data.XPSystem
import com.example.gymtracker.data.UserXP
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import android.util.Log
import com.google.android.filament.MaterialInstance
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CylinderNode

import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import io.github.sceneview.utils.projectionTransform


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
    val tabs = listOf("Welcome", "Body", "Muscles")

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
            2 -> {
                Log.d("HomeScreen", "Rendering MusclesTab")
                MusclesTab(paddingValues = paddingValues)
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

@Composable
fun MusclesTab(paddingValues: PaddingValues) {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val scene = rememberScene(engine)
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bicepsNode = remember { mutableStateOf<ModelNode?>(null) }

        LaunchedEffect(Unit) {
            scene.skybox = null
            // Debug: Check if the model file exists
            try {
                val assets = context.assets
                val files = assets.list("")
                Log.d("MusclesTab", "Available assets: ${files?.joinToString(", ")}")
                val modelExists = assets.list("")?.contains("front_muscles.glb") == true
                Log.d("MusclesTab", "Model file exists: $modelExists")
            } catch (e: Exception) {
                Log.e("MusclesTab", "Error checking assets: ${e.message}")
            }
        }
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            renderer = renderer,
            scene = scene,
            isOpaque = false,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            collisionSystem = rememberCollisionSystem(view),
            mainLightNode = rememberMainLightNode(engine) {
                intensity = 100_000.0f
            },
            cameraNode = rememberCameraNode(engine) {
                // Move camera far away
                position = Position(x = 0f, y = 0f, z = 6.5f)
            },
            childNodes = rememberNodes {
                add(
                    ModelNode(
                        modelInstance = modelLoader.createModelInstance(
                            assetFileLocation = "manequin_model.glb"
                        ),
                        //scaleToUnits = 1.0f
                    ).apply {
                        transform(
                            position = Position(x = 0f, y = -2.4f, z = 0f), //change y here, if change camera it will jump
                            rotation = Rotation(y = 15f)
                        )
                    }
                )
                add(
                    ModelNode(
                        modelInstance = modelLoader.createModelInstance(
                            assetFileLocation = "untrainable.glb"
                        ),
                        //scaleToUnits = 1.0f
                    ).apply {
                        transform(
                            position = Position(x = 0f, y = -2.4f, z = 0f),
                            rotation = Rotation(y = 15f)

                        )
                    }
                )

                val node = ModelNode(
                    modelInstance = modelLoader.createModelInstance(
                        assetFileLocation = "biceps.glb"
                    ),
                    //scaleToUnits = 1.0f
                ).apply {
                    transform(
                        position = Position(x = 0f, y = -2.4f, z = 0f),
                        rotation = Rotation(y = 15f)
                        )
                }


                bicepsNode.value = node

                add(node)

                add(
                    CylinderNode(
                        engine = engine,
                        radius = 0.2f,
                        height = 2.0f,
                        // Simple colored material with physics properties
                        materialInstance = materialLoader.createColorInstance(
                            color = Color.Blue,
                            metallic = 0.5f,
                            roughness = 0.2f,
                            reflectance = 0.4f
                        )
                ).apply {
                        // Define the node position and rotation
                        transform(
                            position = Position(y = 1.0f),
                            rotation = Rotation(x = 90.0f)
                        )
                })
            },
            // Handle user interactions
            onGestureListener = rememberOnGestureListener(
                onDoubleTapEvent = { event, tappedNode ->
                    tappedNode?.let {
                        //it.scale *= 1.1f
                        it.isVisible = false
                    }
                }
            ),
        )
    }

}