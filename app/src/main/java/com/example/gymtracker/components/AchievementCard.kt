package com.example.gymtracker.components

import android.util.Log
// import android.R.attr.translationZ
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymtracker.data.Achievement
import com.example.gymtracker.data.AchievementStatus
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.example.gymtracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    isNewlyUnlocked: Boolean = false
) {
    var wasUnlocked by remember { mutableStateOf(false) }
    var startAnimation by remember { mutableStateOf(false) }
    var animationPhase by remember { mutableStateOf(0) }
    val shouldAnimate = startAnimation && isNewlyUnlocked && !wasUnlocked
    
    // Track if we've initialized the animation state
    var isInitialized by remember { mutableStateOf(true) }

    // Visual state - show as locked until animation starts
    val visuallyUnlocked = achievement.status == AchievementStatus.UNLOCKED && !isNewlyUnlocked || 
                          (isNewlyUnlocked && wasUnlocked)

    // Trophy animations
    val trophyScale by animateFloatAsState(
        targetValue = when {
            !shouldAnimate -> 1f
            animationPhase == 0 -> 1f
            animationPhase == 1 -> 1.3f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = 1500,
            delayMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (shouldAnimate && animationPhase == 1) {
                animationPhase = 2
            }
        },
        label = "trophyScale"
    )

    val trophyAlpha by animateFloatAsState(
        targetValue = when {
            !shouldAnimate -> if (visuallyUnlocked) 0f else 1f
            animationPhase >= 2 -> 0f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "trophyAlpha"
    )

    // Achievement icon animations
    val achievementScale by animateFloatAsState(
        targetValue = when {
            !shouldAnimate -> if (visuallyUnlocked) 1f else 0f
            animationPhase < 2 -> 0f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = 1500,
            delayMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "achievementScale"
    )

    val achievementAlpha by animateFloatAsState(
        targetValue = when {
            !shouldAnimate -> if (visuallyUnlocked) 1f else 0f
            animationPhase < 2 -> 0f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = 1500,
            delayMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "achievementAlpha"
    )

    // State for 3D interaction
    var isExpanded by remember { mutableStateOf(false) }
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(36000f) }  // Back to 100 full rotations
    var rotationZ by remember { mutableStateOf(0f) }
    
    // Track last logged angles
    var lastLoggedFrontAngle by remember { mutableStateOf(0f) }
    var lastLoggedBackAngle by remember { mutableStateOf(0f) }
    
    // Momentum state
    var rotationVelocityX by remember { mutableStateOf(0f) }
    var rotationVelocityY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isMomentumActive by remember { mutableStateOf(false) }
    var lastDragTime by remember { mutableStateOf(0L) }

    // Momentum effect
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            val currentTime = System.currentTimeMillis()
            // Only start momentum if we were dragging recently
            if (currentTime - lastDragTime < 100) {  // 100ms threshold
                isMomentumActive = true
                while (isMomentumActive && (kotlin.math.abs(rotationVelocityX) > 0.1f || kotlin.math.abs(rotationVelocityY) > 0.1f || kotlin.math.abs(rotationX) > 0.1f)) {
                    // Calculate dynamic friction based on velocity
                    val speedX = kotlin.math.abs(rotationVelocityX)
                    val speedY = kotlin.math.abs(rotationVelocityY)
                    
                    // Friction is much lower at high speeds (0.98 at high speed, 0.999 at low speed)
                    val frictionX = 0.98f + (0.019f * (1f - (speedX / 30f).coerceIn(0f, 1f)))
                    val frictionY = 0.98f + (0.019f * (1f - (speedY / 30f).coerceIn(0f, 1f)))
                    
                    // Apply rotation with dynamic friction
                    rotationX += rotationVelocityX
                    rotationY += rotationVelocityY
                    
                    // Apply friction
                    rotationVelocityX *= frictionX
                    rotationVelocityY *= frictionY
                    
                    // Add spring force to return X rotation to 0
                    val springForce = -rotationX * 0.02f  // Adjust this value to control return speed
                    rotationVelocityX += springForce
                    
                    // Stop if velocity is very small
                    if (kotlin.math.abs(rotationVelocityX) < 0.2f) rotationVelocityX = 0f
                    if (kotlin.math.abs(rotationVelocityY) < 0.2f) rotationVelocityY = 0f
                    
                    delay(16) // Approximately 60 FPS
                }
                isMomentumActive = false
                rotationVelocityX = 0f
                rotationVelocityY = 0f
            }
        }
    }
    
    // Smooth rotation values
    val smoothRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smoothRotationX"
    )
    
    val smoothRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smoothRotationY"
    )
    
    val smoothRotationZ by animateFloatAsState(
        targetValue = rotationZ,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smoothRotationZ"
    )
    
    // Animation states
    val scale by animateFloatAsState(
        targetValue = when {
            isExpanded -> 1.5f
            shouldAnimate -> 1.2f  // Increased zoom for new achievements
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val iconRotation by animateFloatAsState(
        targetValue = if (shouldAnimate) 720f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "iconRotation"
    )
    
    val shimmerEffect = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // Add a glow effect for newly unlocked achievements
    val glowAlpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 0.5f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "glowAlpha"
    )

    // Add pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val colorTransition by infiniteTransition.animateColor(
        initialValue = Color(0xFF007FFF), // Azure
        targetValue = Color(0xFFFFD700),  // Yellow
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorTransition"
    )

    LaunchedEffect(startAnimation) {
        if (startAnimation && isNewlyUnlocked && !wasUnlocked) {
            animationPhase = 0
            delay(100)
            animationPhase = 1
            wasUnlocked = true
        }
    }

    // Reset animation phase when animation completes
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            animationPhase = 0
            if (!isNewlyUnlocked) {
                wasUnlocked = false
                startAnimation = false
                delay(100)
                isInitialized = true
            }
        }
    }

    // Reset rotation values when dialog is opened or closed
    LaunchedEffect(isExpanded) {
        rotationX = 0f
        rotationY = 36000f
        rotationZ = 0f
        rotationVelocityX = 0f
        rotationVelocityY = 0f
        isMomentumActive = false
        lastDragTime = 0L
    }

    // Dialog for expanded view
    if (isExpanded) {
        Dialog(
            onDismissRequest = { 
                isExpanded = false 
                rotationX = 0f
                rotationY = 36000f
                rotationZ = 0f
                rotationVelocityX = 0f
                rotationVelocityY = 0f
                isMomentumActive = false
                lastDragTime = 0L
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                lastDragTime = System.currentTimeMillis()
                            },
                            onDragEnd = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                lastDragTime = System.currentTimeMillis()

                                // Log drag values
                                Log.d("AchievementCard", "Drag Event:")
                                Log.d("AchievementCard", "  Drag Amount X: ${dragAmount.x}")
                                Log.d("AchievementCard", "  Drag Amount Y: ${dragAmount.y}")

                                // Update X rotation (tilt)
                                rotationX = (rotationX - dragAmount.y * 0.3f).coerceIn(-90f, 90f)

                                // Update Y rotation (flip)
                                rotationY += dragAmount.x * 0.8f

                                // Update velocities with reduced multipliers and max limits
                                val maxVelocity = 10f
                                rotationVelocityX = (-dragAmount.y * 0.1f).coerceIn(-maxVelocity, maxVelocity)
                                rotationVelocityY = (dragAmount.x * 0.2f).coerceIn(-maxVelocity, maxVelocity)

                                // Log updated values
                                Log.d("AchievementCard", "  New Rotation X: $rotationX")
                                Log.d("AchievementCard", "  New Rotation Y: $rotationY")
                                Log.d("AchievementCard", "  New Velocity X: $rotationVelocityX")
                                Log.d("AchievementCard", "  New Velocity Y: $rotationVelocityY")
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 3D Card
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 1.5f
                            scaleY = 1.5f
                            this.rotationX = smoothRotationX
                            this.rotationY = smoothRotationY
                            this.rotationZ = 0f  // Keep Z rotation at 0
                            cameraDistance = 12f * density
                        }
                ) {
                    // Card container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(0.6f)
                            .graphicsLayer {
                                rotationY = smoothRotationY
                            }
                    ) {
                        // Front of card
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (smoothRotationY % 360f > 90f && smoothRotationY % 360f < 270f) 0f else 1f
                                },
                            shape = MaterialTheme.shapes.large,
                            color = Color.Transparent,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF06063F).copy(alpha = 0.7f),
                                                Color(0xFF2B0544).copy(alpha = 0.7f),
                                                Color(0xFF4D1212).copy(alpha = 0.7f)
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(1000f, 1000f)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Achievement Icon
                                    Image(
                                        painter = painterResource(id = achievement.iconResId),
                                        contentDescription = achievement.title,
                                        modifier = Modifier
                                            .fillMaxWidth(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Achievement Title only
                                    Text(
                                        text = achievement.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Front side glowing lines
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Maximum glow at 45 degrees
                                            val rotationFactor = kotlin.math.abs(
                                                (smoothRotationY % 180f - 90f).absoluteValue - 45f
                                            ) / 45f
                                            alpha = (1f - rotationFactor) * 0.9f  // Brightest at 45 degrees
                                        }
                                ) {
                                    // Calculate width scaling based on angle
                                    val angle = (smoothRotationY % 180f - 90f).absoluteValue
                                    val widthScale = when {
                                        angle <= 45f -> 0.1f + (angle / 45f) * 0.9f  // 0.1 to 1.0
                                        else -> 0.1f + ((90f - angle) / 45f) * 0.9f  // 1.0 to 0.1
                                    }
                                    
                                    // Calculate visibility based on rotation
                                    val normalizedRotation = smoothRotationY % 360f
                                    val isVisible = normalizedRotation <= 90f || normalizedRotation >= 270f
                                    
                                    // Log key variables for front face only when there's a 30-degree difference
                                    val currentAngle = smoothRotationY.absoluteValue
                                    if ((currentAngle - lastLoggedFrontAngle).absoluteValue >= 30f) {
                                        Log.d("AchievementCard", "Front Face:")
                                        Log.d("AchievementCard", "  Raw Rotation Y: $smoothRotationY")
                                        Log.d("AchievementCard", "  Normalized Rotation: ${smoothRotationY % 360f}")
                                        Log.d("AchievementCard", "  Absolute Value: ${smoothRotationY.absoluteValue}")
                                        Log.d("AchievementCard", "  Modulo 360: ${smoothRotationY.absoluteValue % 360f}")
                                        Log.d("AchievementCard", "  Is Visible: $isVisible")
                                        Log.d("AchievementCard", "  Width Scale: $widthScale")
                                        lastLoggedFrontAngle = currentAngle
                                    }
                                    
                                    // Front side glowing lines
                                    // Full width rectangle (background)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 1.0f * widthScale  // Full width scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = size.width * (0.6f - (progress % 2f))
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                    
                                    // Wide rectangle (background)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.3f * widthScale  // Wide rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = size.width * (0.6f - (progress % 2f))
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                    
                                    // Medium rectangle (middle)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.09f * widthScale  // Medium rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = size.width * (0.6f - (progress % 2f))
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.2f)
                                    ) {}
                                    
                                    // Thin rectangle (foreground)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.02f * widthScale  // Thin rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = size.width * (0.6f - (progress % 2f))
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.3f)
                                    ) {}
                                    
                                    // Thinnest rectangle (center)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.01f * widthScale  // Thinnest rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = size.width * (0.6f - (progress % 2f))
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                }
                            }
                        }

                        // Back of card
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (smoothRotationY % 360f > 90f && smoothRotationY % 360f < 270f) 1f else 0f
                                },
                            shape = MaterialTheme.shapes.large,
                            color = Color.Transparent,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF06063F).copy(alpha = 0.7f),
                                                Color(0xFF2B0544).copy(alpha = 0.7f),
                                                Color(0xFF4D1212).copy(alpha = 0.7f)
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(1000f, 1000f)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .graphicsLayer {
                                            rotationY = 180f  // Rotate the text container 180 degrees
                                            scaleX = -1f  // Flip the text horizontally
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = achievement.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = achievement.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                
                                // Back side glowing lines
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Maximum glow at 45 degrees
                                            val rotationFactor = kotlin.math.abs(
                                                (smoothRotationY % 180f - 90f).absoluteValue - 45f
                                            ) / 45f
                                            alpha = (1f - rotationFactor) * 0.9f  // Brightest at 45 degrees
                                        }
                                ) {
                                    // Calculate width scaling based on angle
                                    val angle = (smoothRotationY % 180f - 90f).absoluteValue
                                    val widthScale = when {
                                        angle <= 45f -> 0.1f + (angle / 45f) * 0.9f  // 0.1 to 1.0
                                        else -> 0.1f + ((90f - angle) / 45f) * 0.9f  // 1.0 to 0.1
                                    }
                                    
                                    // Calculate visibility based on rotation
                                    val normalizedRotation = smoothRotationY % 360f
                                    val isVisible = normalizedRotation > 90f && normalizedRotation < 270f
                                    
                                    // Back side glowing lines
                                    // Full width rectangle (background)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 1.0f * widthScale  // Full width scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = -size.width * (0.6f - (progress % 2f))  // Negative translation for opposite direction
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                    
                                    // Wide rectangle (background)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.3f * widthScale  // Wide rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = -size.width * (0.6f - (progress % 2f))  // Negative translation for opposite direction
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                    
                                    // Medium rectangle (middle)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.09f * widthScale  // Medium rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = -size.width * (0.6f - (progress % 2f))  // Negative translation for opposite direction
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.2f)
                                    ) {}
                                    
                                    // Thin rectangle (foreground)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.02f * widthScale  // Thin rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = -size.width * (0.6f - (progress % 2f))  // Negative translation for opposite direction
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.3f)
                                    ) {}
                                    
                                    // Thinnest rectangle (center)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = 45f + (smoothRotationY % 360f)  // Rotate with card
                                                scaleX = 0.01f * widthScale  // Thinnest rectangle scaled by angle
                                                val progress = (smoothRotationY % 360f) / 45f
                                                translationX = -size.width * (0.6f - (progress % 2f))  // Negative translation for opposite direction
                                                alpha = if (isVisible) 1f else 0f
                                            },
                                        color = Color(0xFF00FF8B).copy(alpha = 0.1f)
                                    ) {}
                                }
                            }
                        }
                    }
                }
                
                // Instructions text
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Drag horizontally to flip • Drag vertically to tilt",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Normal card view
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .scale(scale)
            .graphicsLayer {
                if (shouldAnimate) {
                    shadowElevation = 16f
                }
            }
            .clickable {
                if (isNewlyUnlocked && !wasUnlocked) {
                    startAnimation = true
                } else if (visuallyUnlocked) {
                    isExpanded = true
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (visuallyUnlocked) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNewlyUnlocked && !wasUnlocked) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isNewlyUnlocked && !wasUnlocked) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = pulseAlpha),
                                colorTransition.copy(alpha = pulseAlpha * 0.8f),
                                colorTransition.copy(alpha = pulseAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 1000f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = if (isNewlyUnlocked && !wasUnlocked) {
                            Brush.radialGradient(
                                colors =  listOf(
                                    Color.White.copy(alpha = pulseAlpha),
                                    colorTransition.copy(alpha = pulseAlpha * 0.8f),
                                    colorTransition.copy(alpha = pulseAlpha * 0.4f),
                                    Color.Transparent
                                ),
                                center = Offset(Float.POSITIVE_INFINITY, 0f),
                                radius = 1000f
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(end = 16.dp)
                ) {
                    // Trophy icon
                    if (!visuallyUnlocked || shouldAnimate) {
                        Image(
                            painter = painterResource(id = R.drawable.trophy),
                            contentDescription = "Locked Achievement",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = trophyAlpha
                                    scaleX = trophyScale
                                    scaleY = trophyScale
                                }
                        )
                    }
                    
                    // Achievement icon
                    Image(
                        painter = painterResource(id = achievement.iconResId),
                        contentDescription = achievement.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = achievementAlpha
                                scaleX = achievementScale
                                scaleY = achievementScale
                                if (shouldAnimate) {
                                    rotationZ = iconRotation
                                }
                            }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (visuallyUnlocked) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (visuallyUnlocked || achievement.status == AchievementStatus.IN_PROGRESS) {
                            achievement.description
                        } else {
                            achievement.description.split(".").first() + "."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (visuallyUnlocked) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                        maxLines = if (visuallyUnlocked) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (achievement.maxProgress > 0 && !visuallyUnlocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = achievement.currentProgress.toFloat() / achievement.maxProgress.toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${achievement.currentProgress}/${achievement.maxProgress}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Add a lock icon for locked achievements
                if (!visuallyUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
} 