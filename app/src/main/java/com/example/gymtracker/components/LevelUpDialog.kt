package com.example.gymtracker.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Color
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
    var showDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showDialog = true
    }
    
    if (showDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Calculate XP needed for next level (moved to top level)
                    val xpSystem = XPSystem(AppDatabase.getDatabase(context).userXPDao())
                    val currentLevelStartXP = xpSystem.getLevelStartXP(currentLevel)
                    val nextLevelStartXP = xpSystem.getLevelStartXP(currentLevel + 1)
                    val xpNeededForNextLevel = nextLevelStartXP - currentLevelStartXP
                    
                    // Calculate current XP within the level (before the gain)
                    val xpBeforeGain = currentXP - xpGained
                    val xpWithinCurrentLevel = xpBeforeGain - currentLevelStartXP
                    
                    // Calculate new XP within the level (after the gain)
                    val xpAfterGain = currentXP - currentLevelStartXP
                    
                    // Animate the progress
                    var showAnimation by remember { mutableStateOf(false) }
                    var showLevelUpEffect by remember { mutableStateOf(false) }
                    var showLeftoverAnimation by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        delay(500) // Brief delay before starting animation
                        showAnimation = true
                    }
                    
                    // Calculate XP for the new level when level changes
                    val newLevelStartXP = xpSystem.getLevelStartXP(newLevel)
                    val nextNewLevelStartXP = xpSystem.getLevelStartXP(newLevel + 1)
                    val xpNeededForNewLevel = nextNewLevelStartXP - newLevelStartXP
                    
                    // Calculate leftover XP after leveling up
                    val xpUsedForLevelUp = xpNeededForNextLevel - xpWithinCurrentLevel
                    val leftoverXP = xpGained - xpUsedForLevelUp
                    
                    // Calculate XP within the new level (starts at 0, then adds leftover XP)
                    val xpWithinNewLevel = if (leftoverXP > 0) leftoverXP else 0
                    
                    Log.d("LevelUpDialog", "XP Calculation: xpGained=$xpGained, xpWithinCurrentLevel=$xpWithinCurrentLevel, xpNeededForNextLevel=$xpNeededForNextLevel")
                    Log.d("LevelUpDialog", "Leftover Calculation: xpUsedForLevelUp=$xpUsedForLevelUp, leftoverXP=$leftoverXP, xpWithinNewLevel=$xpWithinNewLevel")
                    
                    val animatedXP by animateFloatAsState(
                        targetValue = if (showLeftoverAnimation) {
                            // Animate leftover XP in new level
                            xpWithinNewLevel.toFloat()
                        } else if (showLevelUpEffect) {
                            // Jump to 0 instantly when level changes
                            0f
                        } else if (showAnimation) {
                            // Normal XP animation within current level - clamp to max XP for current level
                            minOf(xpAfterGain.toFloat(), xpNeededForNextLevel.toFloat())
                        } else {
                            // Starting position
                            xpWithinCurrentLevel.toFloat()
                        },
                        animationSpec = if (showLeftoverAnimation) {
                            // Animate leftover XP
                            tween(1500, easing = LinearEasing)
                        } else if (showLevelUpEffect) {
                            // Jump to 0 instantly
                            tween(0, easing = LinearEasing)
                        } else {
                            // Normal animation for XP gain
                            tween(2000, easing = LinearEasing)
                        },
                        label = "xp_progress"
                    )
                    
                    // Use the appropriate XP needed value based on level
                    val currentXpNeeded = if (showLevelUpEffect) xpNeededForNewLevel else xpNeededForNextLevel
                    
                    // Current Level Display - Animated
                    LaunchedEffect(showAnimation, xpAfterGain, xpNeededForNextLevel) {
                        if (showAnimation && xpAfterGain >= xpNeededForNextLevel && !showLevelUpEffect) {
                            Log.d("LevelUpDialog", "Triggering level up effect - xpAfterGain=$xpAfterGain, xpNeededForNextLevel=$xpNeededForNextLevel")
                            delay(2000) // Wait for XP animation to complete
                            showLevelUpEffect = true
                            delay(800) // Wait for level change animation
                            if (leftoverXP > 0) {
                                Log.d("LevelUpDialog", "Starting leftover animation with $leftoverXP XP")
                                showLeftoverAnimation = true
                            }
                        }
                    }
                    
                    val animatedLevel by animateFloatAsState(
                        targetValue = if (showLevelUpEffect) newLevel.toFloat() else currentLevel.toFloat(),
                        animationSpec = tween(800, easing = LinearEasing),
                        label = "level_animation"
                    )
                    
                    Text(
                        text = "Current Level: ${animatedLevel.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    // Current XP Display
                    Text(
                        text = "Total XP: $currentXP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    // XP Gained Display
                    Text(
                        text = "XP Gained: +$xpGained",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    
                    // XP Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Progress text (n/m format) - animated
                            Text(
                                text = "${animatedXP.toInt()}/$currentXpNeeded",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Progress bar - animated
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val progress = (animatedXP / currentXpNeeded.toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                    
                    // Level Change Display (if applicable)
                    if (currentLevel != newLevel) {
                        Text(
                            text = "Level Up! $currentLevel → $newLevel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Continue button
                    Button(
                        onClick = {
                            showDialog = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
} 