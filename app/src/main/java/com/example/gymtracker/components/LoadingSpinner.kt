package com.example.gymtracker.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    proton: Color = MaterialTheme.colorScheme.primary,
    neutron: Color = Color(0xFFC20044),
    electronColor: Color = Color(0xFF00FFFF) // Cyan/glowing color for electron
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // Fast rotation animation for the single electron
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing), // Fast rotation
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulse animation for the electron glow
    val electronScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "electronGlow"
    )
    
    // Trail effect - multiple positions for the electron trail (closer together)
    val trailPositions = listOf(0f, -15f, -30f, -45f, -60f, -75f, -90f, -105f, -120f, -135f, -150f, -165f)
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Nucleus with 3 nucleons in triangular formation
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            // Proton (top center)
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .offset(y = (-12).dp)
                    .background(
                        color = proton.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )
            
            // First neutron (bottom left)
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .offset(x = (-10).dp, y = 8.dp)
                    .background(
                        color = neutron.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )
            
            // Second neutron (bottom right)
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .offset(x = 10.dp, y = 8.dp)
                    .background(
                        color = neutron.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )
        }
        
        // Electron trail effect - multiple semi-transparent copies
        trailPositions.forEachIndexed { index, offset ->
            val trailAlpha = (1f - (index * 0.08f)).coerceAtLeast(0.1f) // Fade out trail
            val trailScale = (1f - (index * 0.05f)).coerceAtLeast(0.3f) // Scale down trail
            
            Box(
                modifier = Modifier
                    .offset(
                        x = (120 * cos(Math.toRadians((rotation + offset).toDouble()))).toFloat().dp,
                        y = (120 * sin(Math.toRadians((rotation + offset).toDouble()))).toFloat().dp
                    )
                    .size(8.dp)
                    .scale(trailScale)
                    .background(
                        color = electronColor.copy(alpha = trailAlpha), // Cyan with fading alpha
                        shape = CircleShape
                    )
            )
        }
        
        // Main electron (brightest)
        Box(
            modifier = Modifier
                .offset(
                    x = (120 * cos(Math.toRadians(rotation.toDouble()))).toFloat().dp,
                    y = (120 * sin(Math.toRadians(rotation.toDouble()))).toFloat().dp
                )
                .size(8.dp)
                .scale(electronScale)
                .background(
                    color = electronColor, // Cyan color for electron
                    shape = CircleShape
                )
        )
    }
} 