package com.example.gymtracker.components

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.example.gymtracker.data.XPSystem
import com.example.gymtracker.data.AppDatabase
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import kotlin.math.cos
import kotlin.math.sin

data class LevelUpStep(
    val level: Int,
    val xpStart: Int,
    val xpEnd: Int,
    val xpNeeded: Int,
    val willLevelUp: Boolean
)

// Cyberpunk color palette
object CyberpunkColors {
    val NeonBlue = Color(0xFF00FFFF)
    val NeonPurple = Color(0xFF8A2BE2)
    val NeonPink = Color(0xFFFF1493)
    val NeonGreen = Color(0xFF00FF41)
    val DarkBackground = Color(0xFF0A0A0A)
    val DarkSurface = Color(0xFF1A1A1A)
    val GlowBlue = Color(0xFF0080FF)
    val GlowPurple = Color(0xFF8000FF)
}

@Composable
fun LevelUpDialog(
    onDismiss: () -> Unit,
    xpGained: Int,
    currentLevel: Int,
    newLevel: Int,
    currentXP: Int,
    xpForNextLevel: Int,
    previousLevelXP: Int
) {
    val context = LocalContext.current
    
    // Animation states for cyberpunk effects
    val glowAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    

    
    val levelUpGlitch by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, easing = LinearEasing)
    )
    
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = CyberpunkColors.NeonBlue,
                    spotColor = CyberpunkColors.NeonPurple
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CyberpunkColors.DarkSurface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyberpunkColors.DarkSurface,
                                CyberpunkColors.DarkBackground
                            )
                        )
                    )
            ) {
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val xpSystem = XPSystem(AppDatabase.getDatabase(context).userXPDao())
                    
                    // Calculate all level-ups that will occur
                    val levelUps = mutableListOf<LevelUpStep>()
                    var currentLevelForCalc = currentLevel
                    var remainingXP = xpGained
                    var currentXPInLevel = xpSystem.getXPWithinLevel(currentXP - xpGained, currentLevel)
                    
                    while (remainingXP > 0 && currentLevelForCalc < newLevel) {
                        val xpNeededForLevel = xpSystem.getXPNeededForLevel(currentLevelForCalc)
                        val xpCanAddToLevel = xpNeededForLevel - currentXPInLevel
                        
                        if (xpCanAddToLevel > 0) {
                            val xpToAdd = minOf(remainingXP, xpCanAddToLevel)
                            levelUps.add(LevelUpStep(
                                level = currentLevelForCalc,
                                xpStart = currentXPInLevel,
                                xpEnd = currentXPInLevel + xpToAdd,
                                xpNeeded = xpNeededForLevel,
                                willLevelUp = currentXPInLevel + xpToAdd >= xpNeededForLevel
                            ))
                            
                            remainingXP -= xpToAdd
                            if (currentXPInLevel + xpToAdd >= xpNeededForLevel) {
                                currentLevelForCalc++
                                currentXPInLevel = 0
                            } else {
                                currentXPInLevel += xpToAdd
                            }
                        } else {
                            currentLevelForCalc++
                            currentXPInLevel = 0
                        }
                    }
                    
                    if (remainingXP > 0) {
                        levelUps.add(LevelUpStep(
                            level = currentLevelForCalc,
                            xpStart = 0,
                            xpEnd = remainingXP,
                            xpNeeded = xpSystem.getXPNeededForLevel(currentLevelForCalc),
                            willLevelUp = false
                        ))
                    }
                    
                    // Animation state
                    var currentStepIndex by remember { mutableStateOf(0) }
                    var showAnimation by remember { mutableStateOf(false) }
                    var animationComplete by remember { mutableStateOf(false) }
                    
                    val xpAnimatable = remember { Animatable(0f) }
                    val levelAnimatable = remember { Animatable(currentLevel.toFloat()) }
                    
                    var finalXP by remember { mutableStateOf(0) }
                    var finalLevel by remember { mutableStateOf(currentLevel) }
                    var finalXPNeeded by remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        delay(500)
                        showAnimation = true
                    }
                    
                    val currentStep = if (currentStepIndex < levelUps.size) levelUps[currentStepIndex] else null
                    
                    LaunchedEffect(showAnimation, currentStepIndex) {
                        if (!showAnimation || currentStep == null) return@LaunchedEffect

                        xpAnimatable.snapTo(currentStep.xpStart.toFloat())

                        xpAnimatable.animateTo(
                            targetValue = currentStep.xpEnd.toFloat(),
                            animationSpec = tween(2000, easing = LinearEasing)
                        )

                        if (currentStep.willLevelUp) {
                            levelAnimatable.animateTo(
                                targetValue = (currentStep.level + 1).toFloat(),
                                animationSpec = tween(800, easing = LinearEasing)
                            )
                            xpAnimatable.snapTo(0f)
                        }

                        currentStepIndex++

                        if (currentStepIndex >= levelUps.size) {
                            finalXP = xpAnimatable.value.toInt()
                            finalLevel = levelAnimatable.value.toInt()
                            finalXPNeeded = if (levelUps.isNotEmpty()) {
                                val lastStep = levelUps.last()
                                lastStep.xpNeeded
                            } else {
                                0
                            }
                            animationComplete = true
                        }
                    }
                    
                    val displayLevel = if (animationComplete) finalLevel else levelAnimatable.value.toInt()
                    val displayXP = if (animationComplete) finalXP else xpAnimatable.value.toInt()
                    val displayXPNeeded = if (animationComplete) finalXPNeeded else (currentStep?.xpNeeded ?: 0)
                    
                    // Cyberpunk Level Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        CyberpunkColors.NeonBlue.copy(alpha = 0.15f),
                                        CyberpunkColors.NeonPurple.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "LEVEL $displayLevel",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberpunkColors.NeonBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // XP Gained with Glow Effect
                    Text(
                        text = "+$xpGained XP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberpunkColors.NeonGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Holographic Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$displayXP/$displayXPNeeded",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyberpunkColors.NeonBlue
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .padding(start = 8.dp)
                            ) {
                                // Background circles
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    repeat(20) { index ->
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = CyberpunkColors.DarkSurface,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                                
                                // Progress parallelograms
                                val progress = if (displayXPNeeded > 0) {
                                    (displayXP.toFloat() / displayXPNeeded.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                
                                val filledCount = (progress * 20).toInt()
                                
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    repeat(20) { index ->
                                        if (index < filledCount) {
                                            // Filled parallelogram
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 10.dp, height = 8.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                CyberpunkColors.NeonBlue,
                                                                CyberpunkColors.NeonPurple
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                                    .rotate(15f)
                                            )
                                        } else {
                                            // Empty space
                                            Spacer(modifier = Modifier.size(width = 10.dp, height = 8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Level Up Effect
                    if (currentLevel != newLevel) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LEVEL UP!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberpunkColors.NeonPink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = "$currentLevel → $newLevel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyberpunkColors.NeonBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cyberpunk Continue Button
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        CyberpunkColors.NeonBlue.copy(alpha = 0.15f),
                                        CyberpunkColors.NeonPurple.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberpunkColors.NeonBlue
                        )
                    }
                }
            }
        }
    }
}
