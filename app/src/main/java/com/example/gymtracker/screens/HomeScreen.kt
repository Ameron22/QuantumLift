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
                WelcomeTabWithMuscles(paddingValues = paddingValues)
            }
            1 -> {
                Log.d("HomeScreen", "Rendering BodyScreen")
                BodyScreen(navController = navController, viewModel = physicalParametersViewModel, paddingValues = paddingValues)
            }
        }
    }
}

@Composable
fun WelcomeTabWithMuscles(paddingValues: PaddingValues) {
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // 3D Muscles Model (takes up the full space)
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { ctx ->
                Log.d("MUSCLES_DEBUG", "=== AndroidView factory called ===")
                
                // Create a simple SurfaceView for the 3D model
                SurfaceView(ctx).apply {
                    // Set transparent background
                    setZOrderOnTop(true)
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    Log.d("MUSCLES_DEBUG", "=== SurfaceView created ===")
                    
                    // Create shared lifecycle flags for both rendering and rotation
                    var isRenderingActive = true
                    var isAnimationActive = true
                    
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
                        
                        val modelViewer = ModelViewer(this)
                        Log.d("MUSCLES_DEBUG", "=== ModelViewer created successfully ===")
                        
                        // Wrap the entire 3D setup in a try-catch to handle any crashes
                        try {
                            // Configure transparent background
                            //modelViewer.view.blendMode = View.BlendMode.TRANSLUCENT    //makes the background light up slowly
                            modelViewer.scene.skybox = null
                                                                        // Configure camera position to make model more visible
                            try {
                                Log.d("MUSCLES_DEBUG", "=== Configuring camera position ===")
                                val camera = modelViewer.camera
                                // Move camera closer (half the distance) and adjust position
                                camera.setExposure(16.0f, 1.0f / 125.0f, 1.0f)
                                camera.setLensProjection(45.0, 1.0, 0.1, 1.0)
                                
                                // Camera will be configured after model loads
                            
                            // Configure view settings for proper rendering
                            val view = modelViewer.view
                            
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
                                modelViewer.renderer.clearOptions = modelViewer.renderer.clearOptions.apply {
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
                            modelViewer.loadModelGltfAsync(buffer) { uri -> 
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
                                val engine = modelViewer.engine
                                val scene = modelViewer.scene
                                
                                // Create indirect light for proper lighting
                                val ibl = "venetian_crossroads_2k"
                                val iblBuffer = ctx.assets.open("envs/$ibl/${ibl}_ibl.ktx").use { input ->
                                    val bytes = ByteArray(input.available())
                                    input.read(bytes)
                                        ByteBuffer.wrap(bytes)
                                }
                                val iblBundle = KTX1Loader.createIndirectLight(engine, iblBuffer)
                                scene.indirectLight = iblBundle.indirectLight
                                modelViewer.indirectLightCubemap = iblBundle.cubemap
                                scene.indirectLight!!.intensity = 30_000.0f
                                
                                Log.d("MUSCLES_DEBUG", "=== Lighting setup completed ===")
                            } catch (e: Exception) {
                                Log.e("MUSCLES_DEBUG", "=== FAILED to setup lighting: ${e.message} ===", e)
                            }
                            
                            // Wait a bit for model to load, then transform
                            this.postDelayed({
                                try {
                                    Log.d("MUSCLES_DEBUG", "=== Applying transformToUnitCube after model load ===")
                                    modelViewer.transformToUnitCube()
                                    Log.d("MUSCLES_DEBUG", "=== Transform applied successfully ===")
                                    
                                    // Wait a bit more for transform to settle, then apply camera changes
                                    this.postDelayed({
                                        try {
                                            Log.d("MUSCLES_DEBUG", "=== Applying camera positioning after transform ===")
                                            
                                            // Scale the model to appear closer
                                            try {
                                                Log.d("MUSCLES_DEBUG", "=== Scaling model to appear closer ===")
                                                // Apply a scale transform to make the model appear larger (closer)
                                                val engine = modelViewer.engine
                                                val asset = modelViewer.asset
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
                                                val engine = modelViewer.engine
                                                val asset = modelViewer.asset
                                                
                                                if (asset != null) {
                                                    val transformManager = engine.transformManager
                                                    val rootEntity = asset.root
                                                    
                                                    if (rootEntity != 0) {
                                                        val transform = transformManager.getInstance(rootEntity)
                                                        
                                                        // Animation parameters
                                                        var theta = 0.0  // Tracks rotation angle
                                                        val rotationSpeed = if (isLowerEndDevice) 0.0005 else 0.001  // Even slower for lower-end devices
                                                        
                                                        Log.d("MUSCLES_DEBUG", "=== Starting simple model rotation animation ===")
                                                        
                                                        // Store the original transform matrix
                                                        val originalMatrix = FloatArray(16)
                                                        transformManager.getTransform(transform, originalMatrix)
                                                        
                                                        // Check if we should use a simpler animation for older devices
                                                        val isOlderDevice = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O
                                                        if (isOlderDevice) {
                                                            Log.d("MUSCLES_DEBUG", "=== Detected older device, using static model ===")
                                                            // For older devices, just set a static rotation instead of continuous animation
                                                            val staticRotation = Math.PI / 4.0  // 45 degrees
                                                            val cosTheta = Math.cos(staticRotation).toFloat()
                                                            val sinTheta = Math.sin(staticRotation).toFloat()
                                                            
                                                            val newMatrix = originalMatrix.clone()
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
                                                                if (!isAnimationActive) return
                                                                
                                                                try {
                                                                    // Additional safety checks for older devices
                                                                    if (transform == 0 || transformManager == null) {
                                                                        Log.w("MUSCLES_DEBUG", "=== Transform or transformManager is null, stopping animation ===")
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
                                                                    
                                                                    // Increment angle
                                                                    theta += rotationSpeed
                                                                    if (theta > 2 * Math.PI) theta -= 2 * Math.PI
                                                                    
                                                                    // Apply rotation to the original matrix (not the current one)
                                                                    val cosTheta = Math.cos(theta).toFloat()
                                                                    val sinTheta = Math.sin(theta).toFloat()
                                                                    
                                                                    // Start with the original matrix
                                                                    val newMatrix = originalMatrix.clone()
                                                                    
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
                                val engine = modelViewer.engine
                                val scene = modelViewer.scene
                                
                                // Load skybox from assets
                                val skyboxName = "venetian_crossroads_2k"
                                val skyboxBuffer = ctx.assets.open("envs/$skyboxName/${skyboxName}_skybox.ktx").use { input ->
                                    val bytes = ByteArray(input.available())
                                    input.read(bytes)
                                    ByteBuffer.wrap(bytes)
                                }
                                val skyboxBundle = KTX1Loader.createSkybox(engine, skyboxBuffer)
                                scene.skybox = skyboxBundle.skybox
                                modelViewer.skyboxCubemap = skyboxBundle.cubemap
                                
                                Log.d("MUSCLES_DEBUG", "=== Skybox loaded successfully ===")
                            } catch (e: Exception) {
                                Log.e("MUSCLES_DEBUG", "=== FAILED to load skybox: ${e.message} ===", e)
                            }
                            
                            // Set all muscles to grey (undertrained) at the start
                            try {
                                Log.d("MUSCLES_DEBUG", "=== Setting all muscles to grey (undertrained) ===")
                                setAllMusclesToGrey(modelViewer)
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
                                    applySorenessBasedColors(modelViewer, muscleSorenessData)
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
                            
                            // Add surface holder callback to track rendering
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    Log.d("MUSCLES_DEBUG", "=== Surface created ===")
                                    
                                    // Start rendering loop with proper lifecycle management
                                    val choreographer = Choreographer.getInstance()
                                    
                                    // Use reduced rendering frequency for lower-end devices
                                    var frameCount = 0
                                    val frameCallback = object : Choreographer.FrameCallback {
                                        override fun doFrame(frameTimeNanos: Long) {
                                            if (!isRenderingActive) return
                                            
                                            try {
                                                // Additional safety checks for older devices
                                                if (modelViewer == null) {
                                                    Log.w("MUSCLES_DEBUG", "=== ModelViewer is null, stopping rendering ===")
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
                                                
                                                modelViewer.render(frameTimeNanos)
                                                //Log.d("MUSCLES_DEBUG", "=== Rendered frame at ${frameTimeNanos} ===")
                                            } catch (e: Exception) {
                                                Log.e("MUSCLES_DEBUG", "=== FAILED to render frame ===", e)
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
                                    Log.d("MUSCLES_DEBUG", "=== Surface changed: ${width}x${height} ===")
                                }
                                
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    Log.d("MUSCLES_DEBUG", "=== Surface destroyed ===")
                                    // Stop all animations when surface is destroyed
                                    try {
                                        // The animation will be stopped by the OnAttachStateChangeListener
                                        // when the view is detached from window
                                        Log.d("MUSCLES_DEBUG", "=== Surface destroyed, animation cleanup handled by view lifecycle ===")
                                    } catch (e: Exception) {
                                        Log.e("MUSCLES_DEBUG", "=== Error stopping animation on surface destroy ===", e)
                                    }
                                }
                            })
                            
                            // Re-enable touch controls but add our own camera positioning
                            setOnTouchListener { _, event ->
                                Log.d("MUSCLES_DEBUG", "=== Touch event: ${event.action} at (${event.x}, ${event.y}) ===")
                                // Pass touch events to modelViewer but also apply our camera positioning
                                modelViewer.onTouchEvent(event)
                                true
                            }

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
        
        // Modern Level Bar at the feet of the model
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