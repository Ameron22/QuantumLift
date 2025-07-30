package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

import android.graphics.PixelFormat
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.View
import com.google.android.filament.MaterialInstance
import java.nio.ByteBuffer
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.example.gymtracker.classes.HistoryViewModel
import com.example.gymtracker.classes.MuscleSorenessData
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.clickable


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
            0 -> {
                            Log.d("HomeScreen", "Rendering WelcomeTab with Muscles")
            WelcomeTabWithMuscles(navController = navController, selectedTabIndex = selectedTabIndex, paddingValues = paddingValues)
            }
            1 -> {
                Log.d("HomeScreen", "Rendering BodyScreen")
                BodyScreen(navController = navController, viewModel = physicalParametersViewModel, paddingValues = paddingValues)
            }
        }
    }
}

@Composable
fun WelcomeTabWithMuscles(navController: NavController, selectedTabIndex: Int, paddingValues: PaddingValues) {
    val context = LocalContext.current
    
    // XP System setup
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
            Log.e("WelcomeTabWithMuscles", "Error loading user XP: ${e.message}")
            isLoading = false
        }
    }
    
    // Create HistoryViewModel instance for muscle soreness data
    val dao = remember { db.exerciseDao() }
    val historyViewModel = remember { HistoryViewModel(dao) }
    
    // Panel state - global scope
    var isPanelExpanded by remember { mutableStateOf(false) }
    
    // ModelViewer instance - global scope
    var modelViewer by remember { mutableStateOf<ModelViewer?>(null) }
    
    // Animation parameters - moved to higher scope for restart capability
    var theta by remember { mutableStateOf(0.0) }  // Tracks rotation angle
    var originalMatrix by remember { mutableStateOf<FloatArray?>(null) }  // Store original transform matrix
            var targetRotation by remember { mutableStateOf(0.0) }  // Target rotation angle
        var isRotatingToTarget by remember { mutableStateOf(false) }  // Whether we're rotating to a specific target
        var shouldStopRotation by remember { mutableStateOf(false) }  // Whether to stop all rotation after reaching target
        var isZoomedIn by remember { mutableStateOf(false) }  // Whether the model is zoomed in for body view
        var zoomType by remember { mutableStateOf("") }  // Type of zoom: "upper" or "lower"
        var zoomProgress by remember { mutableStateOf(0f) }  // Animation progress for zoom (0f = normal, 1f = zoomed)
        var previousZoomType by remember { mutableStateOf("") }  // Previous zoom type for smooth transitions
        var currentYOffset by remember { mutableStateOf(0f) }  // Current Y offset for smooth transitions
        var panelXOffset by remember { mutableStateOf(0f) }  // Current X offset for panel-based movement
    
    // Check for devices that might need reduced rendering frequency
    val isLowerEndDevice = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O || 
                         android.os.Build.MANUFACTURER.lowercase().contains("samsung") && 
                         android.os.Build.MODEL.lowercase().contains("a")
    val rotationSpeed = if (isLowerEndDevice) 0.0005 else 0.001  // Even slower for lower-end devices
    val fastRotationSpeed = if (isLowerEndDevice) 0.02 else 0.04  // Faster rotation for card clicks
    
    // Animation state variables - moved to higher scope for cleanup
    var isRenderingActive by remember { mutableStateOf(true) }
    var isAnimationActive by remember { mutableStateOf(true) }
    var showLoadingCover by remember { mutableStateOf(true) }
    var shouldShowModel by remember { mutableStateOf(true) }  // Control model visibility
    
    // Function to adjust model position based on panel state
    fun adjustModelPosition(viewer: ModelViewer, isExpanded: Boolean) {
        // The position adjustment is now handled in the rotation animation
        // This function is kept for compatibility but the actual adjustment
        // happens in the rotation frame callback
        Log.d("MUSCLES_DEBUG", "=== Panel state changed: ${if (isExpanded) "expanded" else "collapsed"} ===")
    }
    
    Log.d("MUSCLES_DEBUG", "=== WelcomeTabWithMuscles Composable started ===")
    
    // Collect and log muscle soreness data
    val muscleSoreness by historyViewModel.muscleSoreness.collectAsState()
    LaunchedEffect(muscleSoreness) {
        if (muscleSoreness.isNotEmpty()) {
            Log.d("MUSCLES_DEBUG", "=== Muscle soreness data loaded: ${muscleSoreness.size} muscles ===")
            muscleSoreness.forEach { (muscleName, sorenessData) ->
                Log.d("MUSCLES_DEBUG", "=== Muscle: $muscleName, Soreness Level: ${sorenessData.sorenessLevel} ===")
            }
        }
    }
    
    // Cleanup when composable is disposed (when navigating away) - REMOVED DUPLICATE
    
    // Immediate cleanup when navigation starts (before disposal)
    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .collect { route ->
                Log.d("MODEL_LIFECYCLE", "=== Current route: $route ===")
                // If we're not on the home screen anymore, stop all processing immediately
                if (route != null && route != "home" && route != "HomeScreen") {
                    Log.d("MODEL_LIFECYCLE", "=== Navigation detected to $route - stopping all processing immediately ===")
                    isRenderingActive = false
                    isAnimationActive = false
                    showLoadingCover = false
                    // Reset animation parameters
                    theta = 0.0
                    originalMatrix = null
                    Log.d("MODEL_LIFECYCLE", "=== All processing stopped immediately due to navigation ===")
                }
            }
    }
    
    // Immediate cleanup when this composable is no longer active
    LaunchedEffect(Unit) {
        // Monitor if we're still the active tab
        snapshotFlow { selectedTabIndex }
            .collect { tabIndex ->
                // If we're not on the Welcome tab (index 0) anymore, stop processing
                if (tabIndex != 0) {
                    Log.d("MODEL_LIFECYCLE", "=== Tab changed from Welcome (0) to $tabIndex - stopping all processing ===")
                    isRenderingActive = false
                    isAnimationActive = false
                    showLoadingCover = false
                    shouldShowModel = false  // Hide the model immediately
                    theta = 0.0
                    originalMatrix = null
                }
            }
    }
    
    // More aggressive cleanup - stop processing immediately when navigation starts
    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .collect { route ->
                Log.d("MODEL_LIFECYCLE", "=== Current route: $route ===")
                // If we're navigating away from home, stop all processing immediately
                if (route != null && route != "home") {
                    Log.d("MODEL_LIFECYCLE", "=== Navigation away from home detected - stopping all processing immediately ===")
                    isRenderingActive = false
                    isAnimationActive = false
                    showLoadingCover = false
                    shouldShowModel = false  // Hide the model immediately
                    theta = 0.0
                    originalMatrix = null
                }
            }
    }
    
    // Immediate cleanup when this composable starts being disposed
    DisposableEffect(Unit) {
        onDispose {
            Log.d("MODEL_LIFECYCLE", "=== WelcomeTabWithMuscles disposal starting - immediate cleanup ===")
            isRenderingActive = false
            isAnimationActive = false
            showLoadingCover = false
            shouldShowModel = false  // Hide the model immediately
            theta = 0.0
            originalMatrix = null
            Log.d("MODEL_LIFECYCLE", "=== Immediate cleanup completed ===")
        }
    }
    
    // Animate zoom effect with proper state management
    LaunchedEffect(isZoomedIn, zoomType) {
        val targetZoom = if (isZoomedIn) 1f else 0f
        val startZoom = zoomProgress
        val startYOffset = currentYOffset
        val duration = 500L // 500ms animation
        
        // Calculate target Y offset based on zoom type
        val targetYOffset = when (zoomType) {
            "upper" -> 3.5f
            "lower" -> 0.1f
            else -> 0f
        }
        
        Log.d("MODEL_LIFECYCLE", "=== Starting zoom animation: $startZoom -> $targetZoom, Y offset: $startYOffset -> $targetYOffset ===")
        
        repeat(50) { // 50 steps for smooth animation
            val progress = it / 49f
            zoomProgress = startZoom + (targetZoom - startZoom) * progress
            currentYOffset = startYOffset + (targetYOffset - startYOffset) * progress
            if (it % 10 == 0) { // Log every 10 steps to avoid spam
                Log.d("MODEL_LIFECYCLE", "=== Zoom progress: $zoomProgress, Y offset: $currentYOffset (step $it/49) ===")
            }
            delay(duration / 50)
        }
        zoomProgress = targetZoom
        currentYOffset = targetYOffset
        Log.d("MODEL_LIFECYCLE", "=== Zoom animation completed: $targetZoom, Y offset: $currentYOffset ===")
    }
    
    // Handle zoom reset when panel is collapsed
    LaunchedEffect(isPanelExpanded) {
        if (!isPanelExpanded && (isZoomedIn || zoomType.isNotEmpty())) {
            // Panel was collapsed while zoomed in - smoothly reset zoom
            Log.d("MODEL_LIFECYCLE", "=== Panel collapsed while zoomed - resetting zoom smoothly ===")
            isZoomedIn = false
            zoomType = ""
            previousZoomType = ""
            // The LaunchedEffect above will handle the smooth animation back to normal
        }
    }
    
    // Animate panel X offset for smooth panel movement
    LaunchedEffect(isPanelExpanded) {
        val targetXOffset = if (isPanelExpanded) 0.55f else 0f
        val startXOffset = panelXOffset
        val duration = 300L // 300ms animation for panel movement
        
        Log.d("MODEL_LIFECYCLE", "=== Starting panel X animation: $startXOffset -> $targetXOffset ===")
        
        repeat(30) { // 30 steps for smooth animation
            val progress = it / 29f
            panelXOffset = startXOffset + (targetXOffset - startXOffset) * progress
            if (it % 10 == 0) { // Log every 10 steps to avoid spam
                Log.d("MODEL_LIFECYCLE", "=== Panel X offset: $panelXOffset (step $it/29) ===")
            }
            delay(duration / 30)
        }
        panelXOffset = targetXOffset
        Log.d("MODEL_LIFECYCLE", "=== Panel X animation completed: $targetXOffset ===")
    }
    
    // Force immediate visual cleanup when navigation is detected
    LaunchedEffect(shouldShowModel) {
        if (!shouldShowModel) {
            Log.d("MODEL_LIFECYCLE", "=== shouldShowModel is false - forcing immediate visual cleanup ===")
            // Force recomposition to hide the model immediately
            delay(1) // Minimal delay to ensure state change is processed
        }
    }
    
    // Immediate cleanup when tab changes
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != 0) {
            Log.d("MODEL_LIFECYCLE", "=== Tab changed to index $selectedTabIndex - immediate cleanup ===")
            shouldShowModel = false
            isRenderingActive = false
            isAnimationActive = false
            showLoadingCover = false
        }
    }
    
    // Panel state changes are now handled in the rotation animation
    // No need for separate LaunchedEffect since the rotation animation
    // reads isPanelExpanded state directly
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // 3D Muscles Model (takes up the full space) - only show when shouldShowModel is true
        if (shouldShowModel) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
                    .graphicsLayer(alpha = if (shouldShowModel) 1f else 0f), // Force alpha to 0 when shouldShowModel is false
        factory = { ctx ->
            Log.d("MUSCLES_DEBUG", "=== AndroidView factory called ===")
            
            // Check if we should still show the model (navigation might have started)
            if (!shouldShowModel) {
                Log.d("MODEL_LIFECYCLE", "=== shouldShowModel is false - creating empty view ===")
                return@AndroidView SurfaceView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    // Don't set up any surface callbacks or 3D rendering
                    // Just return a transparent view with no processing
                }
            }
            
            // Additional check - if we're not on the Welcome tab, don't create 3D model
            if (selectedTabIndex != 0) {
                Log.d("MODEL_LIFECYCLE", "=== Not on Welcome tab (index $selectedTabIndex) - creating empty view ===")
                return@AndroidView SurfaceView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }
            
            Log.d("MODEL_LIFECYCLE", "=== shouldShowModel is true and on Welcome tab - proceeding with 3D model creation ===")
            
            // Create a simple SurfaceView for the 3D model
            SurfaceView(ctx).apply {
                // Set transparent background
                setZOrderOnTop(true)
                holder.setFormat(PixelFormat.TRANSLUCENT)
                Log.d("MUSCLES_DEBUG", "=== SurfaceView created ===")
                
                // Use shared lifecycle flags from higher scope
                // isRenderingActive and isAnimationActive are now declared at composable level
                
                // List available assets for debugging
                try {
                    Log.d("MUSCLES_DEBUG", "=== Listing available assets ===")
                    val assets = ctx.assets.list("")
                    assets?.forEach { asset ->
                        Log.d("MUSCLES_DEBUG", "=== Asset: $asset ===")
                    }
                    
                    val models = ctx.assets.list("models")
                    models?.forEach { model ->
                        Log.d("MUSCLES_DEBUG", "=== Model: models/$model ===")
                    }
        } catch (e: Exception) {
                    Log.e("MUSCLES_DEBUG", "=== FAILED to list assets ===", e)
    }

                // Initialize Filament Utils
        try {
                    Log.d("MUSCLES_DEBUG", "=== Initializing Filament Utils ===")
                    com.google.android.filament.utils.Utils.init()
                    Log.d("MUSCLES_DEBUG", "=== Filament Utils initialized successfully ===")
        } catch (e: Exception) {
                    Log.e("MUSCLES_DEBUG", "=== FAILED to initialize Filament Utils ===", e)
                }
                
                // Set up the ModelViewer
                try {
                    Log.d("MUSCLES_DEBUG", "=== Creating ModelViewer ===")
                    
                    // Check if device supports OpenGL ES 3.0 (required for Filament)
                    val activityManager = ctx.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val configurationInfo = activityManager.deviceConfigurationInfo
                    val glEsVersion = configurationInfo.glEsVersion
                    
                    Log.d("MUSCLES_DEBUG", "=== OpenGL ES version: $glEsVersion ===")
                    
                    if (glEsVersion < 3.0.toString()) {
                        Log.w("MUSCLES_DEBUG", "=== Device does not support OpenGL ES 3.0 (version: $glEsVersion), skipping 3D model ===")
                        // Create a simple placeholder instead of 3D model
                        this.setBackgroundColor(android.graphics.Color.GRAY)
                        return@apply
                    }
                    
                    // Check for devices that might need reduced rendering frequency
                    val isLowerEndDevice = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O || 
                                         android.os.Build.MANUFACTURER.lowercase().contains("samsung") && 
                                         android.os.Build.MODEL.lowercase().contains("a")
                    
                    Log.d("MUSCLES_DEBUG", "=== Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} ===")
                    Log.d("MUSCLES_DEBUG", "=== API Level: ${android.os.Build.VERSION.SDK_INT} ===")
                    Log.d("MUSCLES_DEBUG", "=== Is lower end device: $isLowerEndDevice ===")
                    
                    val viewer = ModelViewer(this)
                    modelViewer = viewer  // Store in global variable
                    Log.d("MUSCLES_DEBUG", "=== ModelViewer created successfully ===")
                    
                    // Wrap the entire 3D setup in a try-catch to handle any crashes
                    try {
                        // Configure transparent background
                        //viewer.view.blendMode = View.BlendMode.TRANSLUCENT    //makes the background light up slowly
                        viewer.scene.skybox = null
                                                                // Configure camera position to make model more visible
                        try {
                            Log.d("MUSCLES_DEBUG", "=== Configuring camera position ===")
                            val camera = viewer.camera
                            // Move camera closer (half the distance) and adjust position
                            camera.setExposure(16.0f, 1.0f / 125.0f, 1.0f)
                            camera.setLensProjection(45.0, 1.0, 0.1, 1.0)
                            
                            // Camera will be configured after model loads
                        
                        // Configure view settings for proper rendering
                        val view = viewer.view
                        
                        // Use lower quality settings for lower-end devices
                        if (isLowerEndDevice) {
                            Log.d("MUSCLES_DEBUG", "=== Using lower quality settings for lower-end device ===")
                            view.renderQuality = view.renderQuality.apply {
                                hdrColorBuffer = View.QualityLevel.LOW
                            }
                            view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
                                enabled = false  // Disable dynamic resolution for lower-end devices
                            }
                            view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
                                enabled = false  // Disable MSAA for lower-end devices
                            }
                            view.antiAliasing = View.AntiAliasing.NONE  // Disable anti-aliasing for lower-end devices
                            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                                enabled = false  // Disable AO for lower-end devices
                            }
                            view.bloomOptions = view.bloomOptions.apply {
                                enabled = false  // Disable bloom for lower-end devices
                            }
                        } else {
                            view.renderQuality = view.renderQuality.apply {
                                hdrColorBuffer = View.QualityLevel.MEDIUM
                            }
                            view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
                                enabled = true
                                quality = View.QualityLevel.MEDIUM
                            }
                            view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
                                enabled = true
                            }
                            view.antiAliasing = View.AntiAliasing.FXAA
                            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                                enabled = true
                            }
                            view.bloomOptions = view.bloomOptions.apply {
                                enabled = true
                            }
                        }
                        
                        // Ensure proper clearing of the frame buffer
                        try {
                            Log.d("MUSCLES_DEBUG", "=== Configuring frame buffer clearing ===")
                            viewer.renderer.clearOptions = viewer.renderer.clearOptions.apply {
                                clear = true
                                discard = true
                            }
                            Log.d("MUSCLES_DEBUG", "=== Frame buffer clearing configured ===")
                        } catch (e: Exception) {
                            Log.e("MUSCLES_DEBUG", "=== FAILED to configure frame buffer clearing ===", e)
                        }
                        
                        Log.d("MUSCLES_DEBUG", "=== Camera position and view settings configured ===")
                        } catch (e: Exception) {
                            Log.e("MUSCLES_DEBUG", "=== FAILED to configure camera ===", e)
                        }
                    
                        // Load the muscle model
                        try {
                            Log.d("MUSCLES_DEBUG", "=== Loading muscle model from assets ===")
                        
                        // Check if the model file exists
                        val modelFile = "models/model_muscles.glb"
                        try {
                            ctx.assets.open(modelFile).use { input ->
                                Log.d("MUSCLES_DEBUG", "=== Model file exists and is readable ===")
                            }
                        } catch (e: Exception) {
                            Log.e("MUSCLES_DEBUG", "=== Model file not found or not readable: $modelFile ===", e)
                            return@apply
                        }
                        
                        val buffer = ctx.assets.open("models/model_muscles.glb").use { input ->
                            val bytes = ByteArray(input.available())
                            input.read(bytes)
                                            Log.d("MUSCLES_DEBUG", "=== Read ${bytes.size} bytes from model file ===")
                            ByteBuffer.wrap(bytes)
                        }

                                                Log.d("MUSCLES_DEBUG", "=== Calling loadModelGltfAsync ===")
                        viewer.loadModelGltfAsync(buffer) { uri -> 
                            Log.d("MUSCLES_DEBUG", "=== Loading resource: $uri ===")
                            ctx.assets.open("models/$uri").use { input ->
                                val bytes = ByteArray(input.available())
                                input.read(bytes)
                                Log.d("MUSCLES_DEBUG", "=== Loaded resource: $uri (${bytes.size} bytes) ===")
                                ByteBuffer.wrap(bytes)
                            }
                        }
                        Log.d("MUSCLES_DEBUG", "=== Model loading initiated successfully ===")
                        
                        // Set up proper lighting like FilamentApp
                        try {
                            Log.d("MUSCLES_DEBUG", "=== Setting up lighting ===")
                            val engine = viewer.engine
                            val scene = viewer.scene
                            
                            // Create indirect light for proper lighting
                            val ibl = "venetian_crossroads_2k"
                            val iblBuffer = ctx.assets.open("envs/$ibl/${ibl}_ibl.ktx").use { input ->
                                val bytes = ByteArray(input.available())
                                input.read(bytes)
                                    ByteBuffer.wrap(bytes)
                            }
                            val iblBundle = KTX1Loader.createIndirectLight(engine, iblBuffer)
                            scene.indirectLight = iblBundle.indirectLight
                            viewer.indirectLightCubemap = iblBundle.cubemap
                            scene.indirectLight!!.intensity = 30_000.0f
                            
                            Log.d("MUSCLES_DEBUG", "=== Lighting setup completed ===")
                        } catch (e: Exception) {
                            Log.e("MUSCLES_DEBUG", "=== FAILED to setup lighting: ${e.message} ===", e)
                        }
                        
                        // Wait a bit for model to load, then transform
                        this.postDelayed({
                            try {
                                Log.d("MUSCLES_DEBUG", "=== Applying transformToUnitCube after model load ===")
                                viewer.transformToUnitCube()
                                Log.d("MUSCLES_DEBUG", "=== Transform applied successfully ===")
                                
                                // Wait a bit more for transform to settle, then apply camera changes
                                this.postDelayed({
                                    try {
                                        Log.d("MUSCLES_DEBUG", "=== Applying camera positioning after transform ===")
                                        
                                        // Scale the model to appear closer
                                        try {
                                            Log.d("MUSCLES_DEBUG", "=== Scaling model to appear closer ===")
                                            // Apply a scale transform to make the model appear larger (closer)
                                            val engine = viewer.engine
                                            val asset = viewer.asset
                                            if (asset != null) {
                                                val transformManager = engine.transformManager
                                                val rootEntity = asset.root
                                                if (rootEntity != 0) {
                                                    val transform = transformManager.getInstance(rootEntity)
                                                    val matrix = FloatArray(16)
                                                    transformManager.getTransform(transform, matrix)
                                                    // Scale the model by 2.0x to make it appear even closer
                                                    // Apply scaling to the matrix (multiply diagonal elements)
                                                    matrix[0] *= 2.0f  // scale X
                                                    matrix[5] *= 2.0f  // scale Y  
                                                    matrix[10] *= 2.0f // scale Z
                                                    transformManager.setTransform(transform, matrix)
                                                    Log.d("MUSCLES_DEBUG", "=== Model scaled successfully ===")
                                                    
                                                    // Adjust camera position by modifying the model transform
                                                    try {
                                                        Log.d("MUSCLES_DEBUG", "=== Adjusting camera view by modifying model transform ===")
                                                        // Instead of moving camera, we can adjust the model transform to change the view
                                                        // Move the model down slightly to make camera appear higher
                                                        matrix[13] -= 1f  // Move model down (negative Y translation)
                                                        transformManager.setTransform(transform, matrix)
                                                        Log.d("MUSCLES_DEBUG", "=== Model transform adjusted for higher camera view ===")
                                                    } catch (e: Exception) {
                                                        Log.e("MUSCLES_DEBUG", "=== FAILED to adjust model transform ===", e)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MUSCLES_DEBUG", "=== FAILED to scale model ===", e)
                                        }
                                        
                                                                                                                                                                                                                                // Set up simple model rotation animation
                                        try {
                                            Log.d("MUSCLES_DEBUG", "=== Setting up simple model rotation animation ===")
                                            val engine = viewer.engine
                                            val asset = viewer.asset
                                            
                                            if (asset != null) {
                                                val transformManager = engine.transformManager
                                                val rootEntity = asset.root
                                                
                                                if (rootEntity != 0) {
                                                    val transform = transformManager.getInstance(rootEntity)
                                                    
                                                    Log.d("MUSCLES_DEBUG", "=== Starting simple model rotation animation ===")
                                                    
                                                    // Store the original transform matrix in higher scope
                                                    originalMatrix = FloatArray(16)
                                                    transformManager.getTransform(transform, originalMatrix)
                                                    
                                                    // Check if we should use a simpler animation for older devices
                                                    val isOlderDevice = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O
                                                    if (isOlderDevice) {
                                                        Log.d("MUSCLES_DEBUG", "=== Detected older device, using static model ===")
                                                        // For older devices, just set a static rotation instead of continuous animation
                                                        val staticRotation = Math.PI / 4.0  // 45 degrees
                                                        val cosTheta = Math.cos(staticRotation).toFloat()
                                                        val sinTheta = Math.sin(staticRotation).toFloat()
                                                        
                                                        val currentOriginalMatrix = originalMatrix
                                                        if (currentOriginalMatrix != null) {
                                                            val newMatrix = currentOriginalMatrix.clone()
                                                        val temp0 = newMatrix[0]
                                                        val temp2 = newMatrix[2]
                                                        val temp8 = newMatrix[8]
                                                        val temp10 = newMatrix[10]
                                                        
                                                        newMatrix[0] = temp0 * cosTheta - temp2 * sinTheta
                                                        newMatrix[2] = temp0 * sinTheta + temp2 * cosTheta
                                                        newMatrix[8] = temp8 * cosTheta - temp10 * sinTheta
                                                        newMatrix[10] = temp8 * sinTheta + temp10 * cosTheta
                                                        
                                                        if (transform != 0) {
                                                            transformManager.setTransform(transform, newMatrix)
                                                            }
                                                        }
                                                        Log.d("MUSCLES_DEBUG", "=== Static rotation applied for older device ===")
                                                        return@postDelayed
                                                    }
                                                    
                                                    // Set up continuous model rotation with proper lifecycle management
                                                    val choreographer = Choreographer.getInstance()
                                                    // Use the shared isAnimationActive flag from the rendering loop
                                                    // Use reduced animation frequency for lower-end devices
                                                    var animationFrameCount = 0
                                                    val rotationFrameCallback = object : Choreographer.FrameCallback {
                                                        override fun doFrame(frameTimeNanos: Long) {
                                                            if (!isAnimationActive) {
                                                                Log.d("MODEL_LIFECYCLE", "=== Animation stopped - isAnimationActive: false ===")
                                                                return
                                                            }
                                                            
                                                            try {
                                                                // Additional safety checks for older devices
                                                                if (transform == 0 || transformManager == null) {
                                                                    Log.w("MODEL_LIFECYCLE", "=== Transform or transformManager is null, stopping animation ===")
                                                                    isAnimationActive = false
                                                                    return
                                                                }
                                                                
                                                                // Check if animation should still be active
                                                                if (!isAnimationActive) return
                                                                
                                                                // For lower-end devices, update animation every 2nd frame
                                                                if (isLowerEndDevice) {
                                                                    animationFrameCount++
                                                                    if (animationFrameCount % 2 != 0) {
                                                                        if (isAnimationActive) {
                                                                            choreographer.postFrameCallback(this)
                                                                        }
                                                                        return
                                                                    }
                                                                }
                                                                
                                                                // Handle rotation based on whether we're rotating to a target
                                                                if (isRotatingToTarget) {
                                                                    // Calculate the shortest angle difference
                                                                    var angleDiff = targetRotation - theta
                                                                    
                                                                    // Normalize angle difference to shortest path
                                                                    while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI
                                                                    while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI
                                                                    
                                                                    if (kotlin.math.abs(angleDiff) > 0.05) {
                                                                        // Rotate towards target
                                                                        if (angleDiff > 0) {
                                                                            theta += fastRotationSpeed
                                                                        } else {
                                                                            theta -= fastRotationSpeed
                                                                        }
                                                                        // Normalize current angle
                                                                        while (theta > 2 * Math.PI) theta -= 2 * Math.PI
                                                                        while (theta < 0) theta += 2 * Math.PI
                                                                    } else {
                                                                        // Reached target, stop rotation
                                                                        theta = targetRotation
                                                                        isRotatingToTarget = false
                                                                        shouldStopRotation = true
                                                                        Log.d("MODEL_LIFECYCLE", "=== Reached target rotation: $targetRotation, final theta: $theta ===")
                                                                    }
                                                                } else if (!shouldStopRotation) {
                                                                    // Normal continuous rotation only if not stopped
                                                                    theta += rotationSpeed
                                                                    if (theta > 2 * Math.PI) theta -= 2 * Math.PI
                                                                }
                                                                
                                                                // Apply rotation to the original matrix (not the current one)
                                                                val cosTheta = Math.cos(theta).toFloat()
                                                                val sinTheta = Math.sin(theta).toFloat()
                                                                
                                                                // Start with the original matrix
                                                                val currentOriginalMatrix = originalMatrix
                                                                if (currentOriginalMatrix != null) {
                                                                    val newMatrix = currentOriginalMatrix.clone()
                                                                    
                                                                    // Apply panel-based position offset using animated value
                                                                    newMatrix[12] -= panelXOffset  // Move model left based on animated panel offset
                                                                    
                                                                                                        // Apply zoom effect based on zoom type
                                    val zoomScale = 1f + (zoomProgress * 0.8f)  // Scale from 1.0 to 1.8
                                    val zoomOffsetX = zoomProgress * 0.3f  // Move right (back to center) from 0 to 0.3
                                    
                                    // Use smoothly animated Y offset
                                    val zoomOffsetY = currentYOffset * zoomProgress
                                    
                                    // Always apply zoom effect (even if zoomProgress is 0, it will be 1.0 scale)
                                    newMatrix[0] *= zoomScale  // scale X
                                    newMatrix[5] *= zoomScale  // scale Y  
                                    newMatrix[10] *= zoomScale // scale Z
                                    // Move model down to focus on upper body
                                    newMatrix[13] -= zoomOffsetY  // Move model down to show upper body
                                    // Move model right (back to center)
                                    newMatrix[12] += zoomOffsetX  // Move model right to center
                                    
                                   // Log.d("MODEL_LIFECYCLE", "=== Applied zoom: scale=$zoomScale, offsetY=$zoomOffsetY, offsetX=$zoomOffsetX, progress=$zoomProgress, type=$zoomType ===")  too much noise
                                                                
                                                                // Apply Y rotation to the original matrix
                                                                // This prevents accumulation of rotation
                                                                val temp0 = newMatrix[0]
                                                                val temp2 = newMatrix[2]
                                                                val temp8 = newMatrix[8]
                                                                val temp10 = newMatrix[10]
                                                                
                                                                newMatrix[0] = temp0 * cosTheta - temp2 * sinTheta
                                                                newMatrix[2] = temp0 * sinTheta + temp2 * cosTheta
                                                                newMatrix[8] = temp8 * cosTheta - temp10 * sinTheta
                                                                newMatrix[10] = temp8 * sinTheta + temp10 * cosTheta
                                                                
                                                                // Apply the new transform (with safety check)
                                                                if (transform != 0) {
                                                                    transformManager.setTransform(transform, newMatrix)
                                                                    }
                                                                }
                                                                
                                                            } catch (e: Exception) {
                                                                Log.e("MUSCLES_DEBUG", "=== FAILED to rotate model ===", e)
                                                                isAnimationActive = false
                                                                return
                                                            }
                                                            
                                                            if (isAnimationActive) {
                                                                choreographer.postFrameCallback(this)
                                                            }
                                                        }
                                                    }
                                                    
                                                    // Store callback reference for cleanup
                                                    var rotationCallbackRef = rotationFrameCallback
                                                    
                                                    choreographer.postFrameCallback(rotationFrameCallback)
                                                    Log.d("MUSCLES_DEBUG", "=== Simple model rotation animation started ===")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MUSCLES_DEBUG", "=== FAILED to set up model rotation ===", e)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MUSCLES_DEBUG", "=== FAILED to apply camera positioning ===", e)
                                    }
                                }, 500) // 0.5 second delay after transform
                            } catch (e: Exception) {
                                Log.e("MUSCLES_DEBUG", "=== FAILED to apply transform ===", e)
                            }
                        }, 1000) // 1 second delay
                        
                        // Set up skybox from assets
                        try {
                            Log.d("MUSCLES_DEBUG", "=== Setting up skybox from assets ===")
                            val engine = viewer.engine
                            val scene = viewer.scene
                            
                            // Load skybox from assets
                            val skyboxName = "venetian_crossroads_2k"
                            val skyboxBuffer = ctx.assets.open("envs/$skyboxName/${skyboxName}_skybox.ktx").use { input ->
                                val bytes = ByteArray(input.available())
                                input.read(bytes)
                                ByteBuffer.wrap(bytes)
                            }
                            val skyboxBundle = KTX1Loader.createSkybox(engine, skyboxBuffer)
                            scene.skybox = skyboxBundle.skybox
                            viewer.skyboxCubemap = skyboxBundle.cubemap
                            
                            Log.d("MUSCLES_DEBUG", "=== Skybox loaded successfully ===")
                        } catch (e: Exception) {
                            Log.e("MUSCLES_DEBUG", "=== FAILED to load skybox: ${e.message} ===", e)
                        }
                            
                            // Set all muscles to grey (undertrained) at the start
                            try {
                                Log.d("MUSCLES_DEBUG", "=== Setting all muscles to grey (undertrained) ===")
                                setAllMusclesToGrey(viewer)
                                    Log.d("MUSCLES_DEBUG", "=== All muscles set to grey (undertrained) successfully ===")
                            } catch (e: Exception) {
                                Log.e("MUSCLES_DEBUG", "=== FAILED to set muscles to grey: ${e.message} ===", e)
                            }

                            // Function to apply soreness-based colors to muscles
fun applySorenessBasedColors(modelViewer: ModelViewer, muscleSoreness: Map<String, MuscleSorenessData>) {
                                val asset = modelViewer.asset ?: run {
                                    Log.w("MUSCLES_DEBUG", "=== Asset not loaded, cannot apply soreness colors ===")
                                    return
                                }
        
                            val engine = modelViewer.engine
                            val renderableManager = engine.renderableManager

                            Log.d("MUSCLES_DEBUG", "=== Applying soreness-based colors ===")

                            // Direct mapping from muscle parts to model object names
                            val musclePartToModelMapping = mapOf(
                                "Abs" to "Abs",
                                "Adductor" to "Adductor",
                                "Biceps" to "Biceps",
                                "Calves" to "Calves",
                                "Calf" to "Calves",
                                "Chest" to "Chest",
                                "Deltoid_Shoulder" to "Deltoid_Shoulder",
                                "Deltoids" to "Deltoid_Shoulder",
                                "Forearm" to "Wrist",
                                "Forearms" to "Wrist",
                                "Glutes" to "Glutes",
                                "Latissimus_dorsi" to "Latissimus_dorsi",
                                "Latissimus Dorsi" to "Latissimus_dorsi",
                                "Lower Back" to "Trapezius",
                                "Neck" to "Neck",
                                "Obliques" to "Obliques",
                                "Pectorals" to "Chest",
                                "Quands" to "Quands",
                                "Quadriceps" to "Quands",
                                "Trapezius" to "Trapezius",
                                "Triceps" to "Triceps",
                                "Wrist" to "Wrist",
                                // Additional mappings for more specific muscle parts
                                "Upper Chest" to "Chest",
                                "Middle Chest" to "Chest",
                                "Lower Chest" to "Chest",
                                "Front Deltoids" to "Deltoid_Shoulder",
                                "Middle Deltoids" to "Deltoid_Shoulder",
                                "Rear Deltoids" to "Deltoid_Shoulder",
                                "Lats" to "Latissimus_dorsi",
                                "Upper Back" to "Trapezius",
                                "Hamstrings" to "Quands"
                            )

                            // Color mapping based on soreness levels
                            val colorMapping = mapOf(
                                "Very Sore" to "Red",
                                "Sore" to "Orange",
                                "Slightly Sore" to "Yellow",
                                "Fresh" to "Green"
                            )

                            // Get color cube materials
                            val colorCubeMaterials = mutableMapOf<String, MaterialInstance>()
                            val colorCubeNames = listOf("Green", "Grey", "Orange", "Red", "Violet", "Yellow")

                            for (colorName in colorCubeNames) {
                                val colorEntity = asset.entities.find { entity ->
                                    val name = asset.getName(entity)
                                    name == colorName
                                }

                                if (colorEntity != null && renderableManager.hasComponent(colorEntity)) {
                                    val colorRenderable = renderableManager.getInstance(colorEntity)
                                    val primitiveCount = renderableManager.getPrimitiveCount(colorRenderable)

                                    if (primitiveCount > 0) {
                                        val colorMaterial = renderableManager.getMaterialInstanceAt(colorRenderable, 0)
                                        colorCubeMaterials[colorName] = colorMaterial
                                        Log.d("MUSCLES_DEBUG", "=== Loaded color cube material: $colorName ===")
                                    }
                                }
                            }
        
                            // Apply colors based on soreness data
                            for ((musclePart, sorenessData) in muscleSoreness) {
                                // Determine color based on soreness level and training recency
                                val daysSinceLastWorkout = (System.currentTimeMillis() - sorenessData.lastWorkoutTime) / (1000 * 60 * 60 * 24)
                                val colorName = when {
                                    daysSinceLastWorkout > 14 -> "Grey" // Not trained for more than 2 weeks
                                    else -> colorMapping[sorenessData.sorenessLevel] ?: "Green"
                                }

                                val colorMaterial = colorCubeMaterials[colorName]
                                if (colorMaterial != null) {
                                    // Find the corresponding model muscle name
                                    val modelMuscleName = musclePartToModelMapping[musclePart]
                                    
                                    if (modelMuscleName != null) {
                                        // Find the muscle entity in the model
                                        val muscleEntity = asset.entities.find { entity ->
                                            val name = asset.getName(entity)
                                            name == modelMuscleName
                                        }

                                        if (muscleEntity != null && renderableManager.hasComponent(muscleEntity)) {
                                            val muscleRenderable = renderableManager.getInstance(muscleEntity)
                                            val musclePrimitiveCount = renderableManager.getPrimitiveCount(muscleRenderable)

                                            // Apply the color material to all primitives of this muscle
                                            for (primitiveIndex in 0 until musclePrimitiveCount) {
                                                renderableManager.setMaterialInstanceAt(muscleRenderable, primitiveIndex, colorMaterial)
                                            }

                                            Log.d("MUSCLES_DEBUG", "=== Applied $colorName color to $musclePart ($modelMuscleName) - Soreness: ${sorenessData.sorenessLevel}, Days since workout: $daysSinceLastWorkout ===")
                                        } else {
                                            Log.w("MUSCLES_DEBUG", "=== Muscle entity '$modelMuscleName' not found in model ===")
                                        }
                                    } else {
                                        Log.w("MUSCLES_DEBUG", "=== No model mapping found for muscle part: $musclePart ===")
                                    }
                                } else {
                                    Log.w("MUSCLES_DEBUG", "=== Color material '$colorName' not found ===")
                                }
                            }
        
                        Log.d("MUSCLES_DEBUG", "=== Soreness-based colors applied successfully ===")
                        }
                            
                            // Apply soreness-based colors after a short delay
                            this.postDelayed({
                                try {
                                    Log.d("MUSCLES_DEBUG", "=== Applying soreness-based colors ===")
                                    // Get muscle soreness data from the HistoryViewModel
                                    val muscleSorenessData = historyViewModel.muscleSoreness.value
                                    applySorenessBasedColors(viewer, muscleSorenessData)
                                    Log.d("MUSCLES_DEBUG", "=== Soreness-based colors applied successfully ===")
                                } catch (e: Exception) {
                                    Log.e("MUSCLES_DEBUG", "=== FAILED to apply soreness-based colors: ${e.message} ===", e)
                                }
                            }, 2000) // 2 second delay after grey is set
                        
                        // Add view lifecycle listener to stop both rendering and rotation when view is detached
                        this.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: android.view.View) {
                                // Rendering and animation are already started, no action needed
                            }
                            
                            override fun onViewDetachedFromWindow(v: android.view.View) {
                                Log.d("MUSCLES_DEBUG", "=== View detached, stopping rendering loop and rotation animation ===")
                                isRenderingActive = false
                                isAnimationActive = false
                            }
                        })
                        
                        // Add surface holder callback to track rendering - only if shouldShowModel is true
                        if (shouldShowModel) {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                Log.d("MODEL_LIFECYCLE", "=== Surface created - resuming processing ===")
                                
                                // Resume rendering and animation
                                isRenderingActive = true
                                isAnimationActive = true
                                
                                // Don't reset zoom state - preserve current state when surface is recreated
                                // Only reset if we're not in a zoomed state to avoid conflicts
                                if (!isZoomedIn) {
                                    zoomType = ""
                                    previousZoomType = ""
                                    zoomProgress = 0f
                                    currentYOffset = 0f
                                    shouldStopRotation = false
                                    isRotatingToTarget = false
                                    targetRotation = 0.0
                                }
                                // Always reset panel offset to ensure proper panel state
                                panelXOffset = if (isPanelExpanded) 0.55f else 0f
                                
                                Log.d("MODEL_LIFECYCLE", "=== Resumed rendering and animation, preserved zoom state: isZoomedIn=$isZoomedIn, zoomType=$zoomType ===")
                                
                                // Restart rotation animation if model is loaded
                                if (modelViewer != null && originalMatrix != null) {
                                    try {
                                        val choreographer = Choreographer.getInstance()
                                        val transformManager = viewer.engine.transformManager
                                        val transform = viewer.asset?.root?.let { root ->
                                            transformManager.getInstance(root)
                                        }
                                        
                                        if (transform != 0 && transformManager != null) {
                                            // Restart rotation animation
                                            val rotationFrameCallback = object : Choreographer.FrameCallback {
                                                override fun doFrame(frameTimeNanos: Long) {
                                                    if (!isAnimationActive) {
                                                        Log.d("MODEL_LIFECYCLE", "=== Animation stopped - isAnimationActive: false ===")
                                                        return
                                                    }
                                                    
                                                    try {
                                                        // Additional safety checks for older devices
                                                        if (transform == 0 || transformManager == null) {
                                                            Log.w("MODEL_LIFECYCLE", "=== Transform or transformManager is null, stopping animation ===")
                                                            isAnimationActive = false
                                                            return
                                                        }
                                                        
                                                        // Check if animation should still be active
                                                        if (!isAnimationActive) return
                                                        
                                                        // For lower-end devices, update animation every 2nd frame
                                                        if (isLowerEndDevice) {
                                                            // animationFrameCount logic would go here
                                                        }
                                                        
                                                        // Handle rotation based on whether we're rotating to a target
                                                        if (isRotatingToTarget) {
                                                            // Calculate the shortest angle difference
                                                            var angleDiff = targetRotation - theta
                                                            
                                                            // Normalize angle difference to shortest path
                                                            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI
                                                            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI
                                                            
                                                            if (kotlin.math.abs(angleDiff) > 0.05) {
                                                                // Rotate towards target
                                                                if (angleDiff > 0) {
                                                                    theta += fastRotationSpeed
                                                                } else {
                                                                    theta -= fastRotationSpeed
                                                                }
                                                                // Normalize current angle
                                                                while (theta > 2 * Math.PI) theta -= 2 * Math.PI
                                                                while (theta < 0) theta += 2 * Math.PI
                                                            } else {
                                                                // Reached target, stop rotation
                                                                theta = targetRotation
                                                                isRotatingToTarget = false
                                                                shouldStopRotation = true
                                                                Log.d("MODEL_LIFECYCLE", "=== Reached target rotation: $targetRotation, final theta: $theta ===")
                                                            }
                                                        } else if (!shouldStopRotation) {
                                                            // Normal continuous rotation only if not stopped
                                                            theta += rotationSpeed
                                                            if (theta > 2 * Math.PI) theta -= 2 * Math.PI
                                                        }
                                                        
                                                        // Apply rotation to the original matrix (not the current one)
                                                        val cosTheta = Math.cos(theta).toFloat()
                                                        val sinTheta = Math.sin(theta).toFloat()
                                                        
                                                        // Start with the original matrix
                                                        val currentOriginalMatrix = originalMatrix
                                                        if (currentOriginalMatrix != null) {
                                                            val newMatrix = currentOriginalMatrix.clone()
                                                        
                                                            // Apply panel-based position offset using animated value
                                                            newMatrix[12] -= panelXOffset  // Move model left based on animated panel offset
                                                            
                                                            // Apply zoom effect based on zoom type
                                                            val zoomScale = 1f + (zoomProgress * 0.8f)  // Scale from 1.0 to 1.8
                                                            val zoomOffsetX = zoomProgress * 0.3f  // Move right (back to center) from 0 to 0.3
                                                            
                                                            // Use smoothly animated Y offset
                                                            val zoomOffsetY = currentYOffset * zoomProgress
                                                            
                                                            // Always apply zoom effect (even if zoomProgress is 0, it will be 1.0 scale)
                                                            newMatrix[0] *= zoomScale  // scale X
                                                            newMatrix[5] *= zoomScale  // scale Y  
                                                            newMatrix[10] *= zoomScale // scale Z
                                                            // Move model down to focus on upper body
                                                            newMatrix[13] -= zoomOffsetY  // Move model down to show upper body
                                                            // Move model right (back to center)
                                                            newMatrix[12] += zoomOffsetX  // Move model right to center
                                                            
                                                            //Log.d("MODEL_LIFECYCLE", "=== Applied zoom in restarted animation: scale=$zoomScale, offsetY=$zoomOffsetY, offsetX=$zoomOffsetX, progress=$zoomProgress, type=$zoomType ===")
                                                            
                                                            // Apply Y rotation to the original matrix
                                                            // This prevents accumulation of rotation
                                                            val temp0 = newMatrix[0]
                                                            val temp2 = newMatrix[2]
                                                            val temp8 = newMatrix[8]
                                                            val temp10 = newMatrix[10]
                                                            
                                                            newMatrix[0] = temp0 * cosTheta - temp2 * sinTheta
                                                            newMatrix[2] = temp0 * sinTheta + temp2 * cosTheta
                                                            newMatrix[8] = temp8 * cosTheta - temp10 * sinTheta
                                                            newMatrix[10] = temp8 * sinTheta + temp10 * cosTheta
                                                            
                                                            // Apply the new transform (with safety check)
                                                            if (transform != 0) {
                                                                transformManager.setTransform(transform!!, newMatrix)
                                                            }
                                                        }
                                                        
                                                    } catch (e: Exception) {
                                                        Log.e("MODEL_LIFECYCLE", "=== FAILED to rotate model ===", e)
                                                        isAnimationActive = false
                                                        return
                                                    }
                                                    
                                                    if (isAnimationActive) {
                                                        choreographer.postFrameCallback(this)
                                                    }
                                                }
                                            }
                                            
                                            choreographer.postFrameCallback(rotationFrameCallback)
                                            Log.d("MODEL_LIFECYCLE", "=== Restarted rotation animation ===")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MODEL_LIFECYCLE", "=== FAILED to restart rotation animation ===", e)
                                    }
                                }
                                
                                // Start rendering loop with proper lifecycle management
                                val choreographer = Choreographer.getInstance()
                                
                                // Use reduced rendering frequency for lower-end devices
                                var frameCount = 0
                                val frameCallback = object : Choreographer.FrameCallback {
                                    override fun doFrame(frameTimeNanos: Long) {
                                        if (!isRenderingActive) {
                                            Log.d("MODEL_LIFECYCLE", "=== Rendering stopped - isRenderingActive: false ===")
                                            return
                                        }
                                        
                                        try {
                                            // Additional safety checks for older devices
                                            if (modelViewer == null) {
                                                Log.w("MODEL_LIFECYCLE", "=== ModelViewer is null, stopping rendering ===")
                                                isRenderingActive = false
                                                isAnimationActive = false
                                                return
                                            }
                                            
                                            // For lower-end devices, render every 3rd frame instead of every frame
                                            if (isLowerEndDevice) {
                                                frameCount++
                                                if (frameCount % 3 != 0) {
                                                    if (isRenderingActive) {
                                                        choreographer.postFrameCallback(this)
                                                    }
                                                    return
                                                }
                                            }
                                            
                                            viewer.render(frameTimeNanos)
                                            //Log.d("MODEL_LIFECYCLE", "=== Rendered frame at ${frameTimeNanos} ===")
                                        } catch (e: Exception) {
                                            Log.e("MODEL_LIFECYCLE", "=== FAILED to render frame ===", e)
                                            isRenderingActive = false
                                            isAnimationActive = false
                                            return
                                        }
                                        
                                        if (isRenderingActive) {
                                            choreographer.postFrameCallback(this)
                                        }
                                    }
                                }
                                
                                choreographer.postFrameCallback(frameCallback)
                                Log.d("MUSCLES_DEBUG", "=== Started rendering loop ===")
                            }
                            
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                Log.d("MODEL_LIFECYCLE", "=== Surface changed: ${width}x${height} ===")
                            }
                            
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                Log.d("MODEL_LIFECYCLE", "=== Surface destroyed - stopping all processing ===")
                                // Stop all animations when surface is destroyed
                                try {
                                    isRenderingActive = false
                                    isAnimationActive = false
                                    Log.d("MODEL_LIFECYCLE", "=== Surface destroyed, stopped rendering and animation ===")
                                } catch (e: Exception) {
                                    Log.e("MODEL_LIFECYCLE", "=== Error stopping animation on surface destroy ===", e)
                                }
                            }
                        })
                        } else {
                            Log.d("MODEL_LIFECYCLE", "=== shouldShowModel is false - skipping surface callbacks ===")
                        }
                        
                        // Re-enable touch controls but add our own camera positioning
                        setOnTouchListener { _, event ->
                            Log.d("MUSCLES_DEBUG", "=== Touch event: ${event.action} at (${event.x}, ${event.y}) ===")
                            // Pass touch events to viewer but also apply our camera positioning
                            viewer.onTouchEvent(event)
                            true
                        }
                        
                        // Add lifecycle listener to track when view is attached/detached
                        addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: android.view.View) {
                                Log.d("MODEL_LIFECYCLE", "=== View attached to window - resuming processing ===")
                                isRenderingActive = true
                                isAnimationActive = true
                            }
                            
                            override fun onViewDetachedFromWindow(v: android.view.View) {
                                Log.d("MODEL_LIFECYCLE", "=== View detached from window - pausing processing ===")
                                isRenderingActive = false
                                isAnimationActive = false
                            }
                        })

                    } catch (e: Exception) {
                        Log.e("MUSCLES_DEBUG", "=== FAILED to load model ===", e)
                    }
                } catch (e: Exception) {
                    Log.e("MUSCLES_DEBUG", "=== FAILED to setup 3D model - using fallback ===", e)
                    // Set a simple background color as fallback
                    this.setBackgroundColor(android.graphics.Color.DKGRAY)
                }
                } catch (e: Exception) {
                    Log.e("MUSCLES_DEBUG", "=== FAILED to create ModelViewer ===", e)
                }
            }
        }
    )
        }
    
        // Hexagonal Loading Cover
        // showLoadingCover is now declared at higher scope
        var currentGroup by remember { mutableStateOf("A") }
        var animationProgress by remember { mutableStateOf(0f) }
        
        // Get vibrator service for haptic feedback
        val vibrator = remember {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
        }
        
        LaunchedEffect(Unit) {
            // Start the hexagonal fade animation after a short delay
            delay(500)
            
            // Group-based fade animation with smooth transitions
            // Group A fades out over 0.3 seconds with bubble vibrations
            repeat(30) { // 30 steps of 10ms each = 0.3 seconds
                if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
                delay(10)
                animationProgress += 0.033f // 1/30 = 0.033
                // Bubble vibration every 3 steps (every 30ms)
                if (it % 3 == 0) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
            currentGroup = "B"
            
            // Group B fades out over 0.3 seconds with bubble vibrations
            repeat(30) {
                if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
                delay(10)
                animationProgress += 0.033f
                // Bubble vibration every 3 steps (every 30ms)
                if (it % 3 == 0) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
            currentGroup = "C"
            
            // Group C fades out over 0.3 seconds with bubble vibrations
            repeat(30) {
                if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
                delay(10)
                animationProgress += 0.033f
                // Bubble vibration every 3 steps (every 30ms)
                if (it % 3 == 0) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
            currentGroup = "D"
            
            // Group D fades out over 0.3 seconds with bubble vibrations
            repeat(30) {
                if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
                delay(10)
                animationProgress += 0.033f
                // Bubble vibration every 3 steps (every 30ms)
                if (it % 3 == 0) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            if (!showLoadingCover) return@LaunchedEffect // Stop if composable is disposed
            currentGroup = "DONE"
            
            // Hide the entire cover after animation completes
            showLoadingCover = false
        }
        
        // Debug log to verify current group is updating
        LaunchedEffect(currentGroup) {
            Log.d("HEXAGON_DEBUG", "Current group: $currentGroup")
        }
        
        // Debug log to verify loading cover state
        LaunchedEffect(showLoadingCover) {
            Log.d("HEXAGON_DEBUG", "Show loading cover: $showLoadingCover")
        }
        
        if (showLoadingCover) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                // Interconnected hexagonal grid pattern with key to force recomposition
                key(currentGroup, animationProgress) {
                    HexagonalGrid(currentGroup = currentGroup, animationProgress = animationProgress)
                }
            }
        }
        
        // Body View Panel on the right side
        if (!showLoadingCover) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = tween(300)
                        )
                        .width(if (isPanelExpanded) 175.dp else 48.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Arrow button in top left corner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        IconButton(
                            onClick = { 
                                isPanelExpanded = !isPanelExpanded
                                // Resume normal rotation when collapsing the panel
                                if (!isPanelExpanded) {
                                    shouldStopRotation = false
                                    isRotatingToTarget = false
                                    // Don't immediately reset zoom - let the animation handle it
                                    // The LaunchedEffect will handle the smooth transition back to normal
                                    Log.d("MODEL_LIFECYCLE", "=== Panel collapsed - resuming normal rotation and triggering zoom reset ===")
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPanelExpanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                                contentDescription = if (isPanelExpanded) "Collapse panel" else "Expand panel",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Only show cards when expanded
                    if (isPanelExpanded) {
                        // Upper Body Front Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clickable {
                                    Log.d("MODEL_LIFECYCLE", "=== Upper Body Front card clicked - rotating to face user ===")
                                    // Calculate the nearest 0-degree position (face user)
                                    val currentAngle = theta
                                    val nearestZero = (currentAngle / (2 * Math.PI)).toInt() * 2 * Math.PI
                                    targetRotation = nearestZero
                                    isRotatingToTarget = true
                                    shouldStopRotation = false  // Reset stop state to allow rotation
                                    previousZoomType = zoomType  // Store previous zoom type
                                    isZoomedIn = true  // Enable zoom effect
                                    zoomType = "upper"  // Set zoom type for upper body
                                    Log.d("MODEL_LIFECYCLE", "=== Current angle: $currentAngle, Target: $targetRotation, Zoom: $isZoomedIn, Type: $zoomType ===")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Upper Body Front",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Lower Body Front Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clickable {
                                    Log.d("MODEL_LIFECYCLE", "=== Lower Body Front card clicked - rotating to face user ===")
                                    // Calculate the nearest 0-degree position (face user)
                                    val currentAngle = theta
                                    val nearestZero = (currentAngle / (2 * Math.PI)).toInt() * 2 * Math.PI
                                    targetRotation = nearestZero
                                    isRotatingToTarget = true
                                    shouldStopRotation = false  // Reset stop state to allow rotation
                                    previousZoomType = zoomType  // Store previous zoom type
                                    isZoomedIn = true  // Enable zoom effect
                                    zoomType = "lower"  // Set zoom type for lower body
                                    Log.d("MODEL_LIFECYCLE", "=== Current angle: $currentAngle, Target: $targetRotation, Zoom: $isZoomedIn, Type: $zoomType ===")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Lower Body Front",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Upper Body Back Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Upper Body Back",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Lower Body Back Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Lower Body Back",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Modern Level Bar at the feet of the model - only show after loading cover fades
        if (!showLoadingCover) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                ) {
                    if (!isLoading && userXP != null) {
                        ModernLevelBar(userXP = userXP!!, xpSystem = xpSystem)
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Show default welcome for new users
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Welcome to Quantum Lift",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your journey starts here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Log.d("MUSCLES_DEBUG", "=== WelcomeTabWithMuscles Composable completed ===")
    }
}

// Function to set all muscles to grey (undertrained) using the same pattern as MainActivity.kt
fun setAllMusclesToGrey(modelViewer: ModelViewer) {
    val asset = modelViewer.asset ?: run {
        Log.w("MUSCLES_DEBUG", "=== Asset not loaded, cannot set muscles to grey ===")
        return
    }
    
    val engine = modelViewer.engine
    val renderableManager = engine.renderableManager
    
    Log.d("MUSCLES_DEBUG", "=== Setting all muscles to grey (undertrained) ===")
    
    // Find the Grey color cube entity
    val greyEntity = asset.entities.find { entity ->
        val name = asset.getName(entity)
        name == "Grey"
    }
    
    if (greyEntity == null) {
        Log.e("MUSCLES_DEBUG", "=== Grey color cube not found in model ===")
        return
    }
    
    if (!renderableManager.hasComponent(greyEntity)) {
        Log.e("MUSCLES_DEBUG", "=== Grey entity has no renderable component ===")
        return
    }
    
    // Get the Grey material instance
    val greyRenderable = renderableManager.getInstance(greyEntity)
    val greyPrimitiveCount = renderableManager.getPrimitiveCount(greyRenderable)
    
    if (greyPrimitiveCount == 0) {
        Log.e("MUSCLES_DEBUG", "=== Grey entity has no primitives ===")
        return
    }
    
    val greyMaterial = renderableManager.getMaterialInstanceAt(greyRenderable, 0)
    Log.d("MUSCLES_DEBUG", "=== Got Grey material instance ===")
    
    // Apply Grey material to all muscle entities (excluding color cubes and protected parts)
    val colorCubeNames = listOf("Green", "Grey", "Orange", "Red", "Violet", "Yellow")
    val protectedParts = listOf("Untrainable", "_FullBody Remeshed.001")
    
    for (entity in asset.entities) {
        val name = asset.getName(entity)
        
        // Skip color cubes and protected parts
        if (colorCubeNames.contains(name) || protectedParts.any { protectedPart ->
            name.contains(protectedPart)
        }) {
            continue
        }
        
        // Apply Grey material to this muscle
        if (renderableManager.hasComponent(entity)) {
            val muscleRenderable = renderableManager.getInstance(entity)
            val musclePrimitiveCount = renderableManager.getPrimitiveCount(muscleRenderable)
            
            // Apply the Grey material to all primitives of this muscle
            for (primitiveIndex in 0 until musclePrimitiveCount) {
                renderableManager.setMaterialInstanceAt(muscleRenderable, primitiveIndex, greyMaterial)
            }
            
            Log.d("MUSCLES_DEBUG", "=== Applied Grey material to muscle: $name ===")
        }
    }
    
    Log.d("MUSCLES_DEBUG", "=== All muscles set to grey (undertrained) successfully ===")
}

@Composable
fun HexagonalGrid(currentGroup: String, animationProgress: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Debug log to verify HexagonalGrid is being called
    LaunchedEffect(Unit) {
        Log.d("HEXAGON_DEBUG", "HexagonalGrid called with currentGroup: $currentGroup")
    }
    
    // Create random group assignments for each hexagon
    val hexagonGroups = remember {
        val totalHexagons = 22 * 4 // rows * cols
        val groups = mutableListOf<String>()
        repeat(totalHexagons) { index ->
            val group = when (index % 4) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                else -> "D"
            }
            groups.add(group)
        }
        groups.shuffled() // Randomize the order
    }
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val hexSize = min(width / 5, height / 3.5f) // Slightly smaller hexagons
        
        // Create interconnected hexagon grid
        val rows = 22
        val cols = 4
        
        var hexagonIndex = 0
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Proper hexagonal tessellation with tight spacing
                val radius = hexSize * 0.5f
                val width = radius * 2f
                val height = radius * 1.732f  // sqrt(3) * radius
                
                // Horizontal spacing: width * 1.5 (full width + half width for proper spacing)
                val centerX = col * width * 1.5f + width * 0.2f
                // Vertical spacing: height/2 (half height of hexagon) - this creates tight packing
                val centerY = row * (height / 2f)
                
                // Offset every other row by half the horizontal spacing
                val offsetX = if (row % 2 == 1) width * 0.75f else 0f
                val finalCenterX = centerX + offsetX
                
                // Get random group for this hexagon
                val group = hexagonGroups[hexagonIndex]
                hexagonIndex++
                
                // Calculate alpha based on current group and hexagon group with smooth fading
                val alpha = when {
                    currentGroup == "DONE" -> 0f // All hexagons transparent when animation is done
                    group == "A" && currentGroup in listOf("B", "C", "D", "DONE") -> {
                        // Group A fades out smoothly
                        val fadeStart = 0f
                        val fadeEnd = 0.25f // 25% of total animation
                        if (animationProgress <= fadeEnd) {
                            1f - (animationProgress / fadeEnd)
                        } else {
                            0f
                        }
                    }
                    group == "B" && currentGroup in listOf("C", "D", "DONE") -> {
                        // Group B fades out smoothly
                        val fadeStart = 0.25f
                        val fadeEnd = 0.5f // 50% of total animation
                        if (animationProgress <= fadeEnd) {
                            1f - ((animationProgress - fadeStart) / (fadeEnd - fadeStart))
                        } else {
                            0f
                        }
                    }
                    group == "C" && currentGroup in listOf("D", "DONE") -> {
                        // Group C fades out smoothly
                        val fadeStart = 0.5f
                        val fadeEnd = 0.75f // 75% of total animation
                        if (animationProgress <= fadeEnd) {
                            1f - ((animationProgress - fadeStart) / (fadeEnd - fadeStart))
                        } else {
                            0f
                        }
                    }
                    group == "D" && currentGroup == "DONE" -> {
                        // Group D fades out smoothly
                        val fadeStart = 0.75f
                        val fadeEnd = 1f // 100% of total animation
                        if (animationProgress <= fadeEnd) {
                            1f - ((animationProgress - fadeStart) / (fadeEnd - fadeStart))
                        } else {
                            0f
                        }
                    }
                    else -> 1f // Fully opaque for current and future groups
                }
                
                // Debug log for first few hexagons
                if (hexagonIndex <= 5) {
                    Log.d("HEXAGON_DEBUG", "Hexagon $hexagonIndex: Group=$group, CurrentGroup=$currentGroup, Alpha=$alpha")
                }
                
                // Draw hexagon
                val path = Path()
                for (i in 0..5) {
                    val angle = i * Math.PI / 3
                    val x = finalCenterX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                
                // Draw hexagon with fill and stroke for better visibility
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = alpha * 0.8f), // Slightly transparent fill
                    style = Fill
                )
                
                // Draw border for better visibility
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = alpha),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

@Composable
fun ModernLevelBar(userXP: UserXP, xpSystem: XPSystem) {
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Level and Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Level ${userXP.currentLevel}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = levelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${userXP.totalXP} XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (userXP.xpToNextLevel > 0) {
                    Text(
                        text = "${userXP.xpToNextLevel} to next",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Modern Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressPercentage)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }
    }
}

