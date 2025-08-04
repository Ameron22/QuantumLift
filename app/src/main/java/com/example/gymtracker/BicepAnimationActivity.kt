package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.gymtracker.services.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

private const val TAG = "BicepAnimation"
private const val FRAME_DURATION = 16L // ~60fps
private const val TOTAL_DURATION = 2000L // 2 seconds total animation
private const val TOTAL_FRAMES = (TOTAL_DURATION / FRAME_DURATION).toInt()

// Animation phases
private const val PHASE_1_END = 0.15f // Initial growth (0.3s)
private const val PHASE_2_END = 0.45f // Pause (0.6s)
private const val PHASE_3_END = 0.7f // Vibrate and rotate (0.5s)
private const val PHASE_4_END = 0.8f // Shrink (0.2s)
private const val PHASE_5_END = 1.0f // Bicep animation (0.4s)

class BicepAnimationActivity : ComponentActivity() {
    private var currentFrame = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var imageView: ImageView
    private var isLoggedIn = false
    private var authCheckCompleted = false

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (currentFrame <= TOTAL_FRAMES) {
                val progress = currentFrame.toFloat() / TOTAL_FRAMES
                animatePhase(progress)
                
                Log.d(TAG, "Frame $currentFrame: progress=${(progress * 100).toInt()}%")
                
                currentFrame++
                handler.postDelayed(this, FRAME_DURATION)
            } else {
                Log.d(TAG, "Animation complete")
                // Wait for auth check to complete before navigating
                if (authCheckCompleted) {
                    navigateToNextScreen()
                } else {
                    // If auth check is still pending, wait a bit more
                    handler.postDelayed({
                        if (authCheckCompleted) {
                            navigateToNextScreen()
                        } else {
                            // Force navigation after timeout
                            Log.d(TAG, "Auth check timeout, navigating to login")
                            startActivity(Intent(this@BicepAnimationActivity, LoginActivity::class.java))
                            finish()
                        }
                    }, 500)
                }
            }
        }
    }

    private fun animatePhase(progress: Float) {
        when {
            // Phase 1: Initial growth and rotation
            progress <= PHASE_1_END -> {
                val phaseProgress = progress / PHASE_1_END
                val scale = interpolateOvershoot(phaseProgress) * 0.85f
                imageView.scaleX = scale
                imageView.scaleY = scale
                imageView.rotation = 360f * phaseProgress
            }
            
            // Phase 2: Brief pause
            progress <= PHASE_2_END -> {
                // Keep the same scale but reset rotation to 0
                imageView.rotation = 0f
            }
            
            // Phase 3: Vibrate and rotate
            progress <= PHASE_3_END -> {
                val phaseProgress = (progress - PHASE_2_END) / (PHASE_3_END - PHASE_2_END)
                // Add vibration effect using sine wave
                val vibrationScale = 0.85f + (sin(phaseProgress * 30) * 0.05f)
                imageView.scaleX = vibrationScale
                imageView.scaleY = vibrationScale
                imageView.rotation = 360f + (phaseProgress * 720f) // Two additional rotations
            }
            
            // Phase 4: Shrink
            progress <= PHASE_4_END -> {
                val phaseProgress = (progress - PHASE_3_END) / (PHASE_4_END - PHASE_3_END)
                val scale = 0.85f * (1 - phaseProgress)
                imageView.scaleX = scale
                imageView.scaleY = scale
                // Switch to bicep image at the end of shrinking
                if (phaseProgress > 0.9f && imageView.tag != "bicep") {
                    imageView.setImageResource(R.drawable.avd_bicepanim)
                    imageView.tag = "bicep"
                }
            }
            
            // Phase 5: Bicep animation
            else -> {
                val phaseProgress = (progress - PHASE_4_END) / (PHASE_5_END - PHASE_4_END)
                
                when {
                    // First half: Fast rotation and growth
                    phaseProgress < 0.5f -> {
                        val firstHalfProgress = phaseProgress * 2f // normalize to 0-1
                        // Accelerated scale with stronger overshoot
                        val scale = interpolateOvershoot(firstHalfProgress) * 0.6f
                        // Fast rotation (3 full spins in first half)
                        val rotation = firstHalfProgress * 1080f // 3 * 360
                        
                        imageView.scaleX = scale
                        imageView.scaleY = scale
                        imageView.rotation = rotation
                    }
                    // Second half normal animation
                    phaseProgress < 0.95f -> {
                        val secondHalfProgress = (phaseProgress - 0.5f) * 2.22f // normalize to 0-1 (adjusted for 95%)
                        // Slower growth to final size with gentle overshoot
                        val scale = 0.6f + (0.25f * interpolateSettling(secondHalfProgress))
                        // Slower rotation (1 more full spin)
                        val rotation = 1080f + (secondHalfProgress * 360f)
                        
                        imageView.scaleX = scale
                        imageView.scaleY = scale
                        imageView.rotation = rotation
                        
                        // Add subtle vibration that diminishes towards the end
                        val vibrationAmount = (1f - secondHalfProgress) * 0.02f
                        val vibration = sin(secondHalfProgress * 30) * vibrationAmount
                        imageView.scaleX += vibration
                        imageView.scaleY += vibration
                    }
                    // Final pulsating growth
                    else -> {
                        val finalProgress = (phaseProgress - 0.95f) * 20f // normalize 0.95-1.0 to 0-1
                        // Create a pulsating effect that grows larger
                        val pulseWave = sin(finalProgress * 12f * Math.PI.toFloat()) * 0.1f
                        val growthBase = 0.85f + (finalProgress * 0.25f) // Grow from 0.85 to 1.1
                        val finalScale = growthBase + (pulseWave * (1f - finalProgress))
                        
                        imageView.scaleX = finalScale
                        imageView.scaleY = finalScale
                        // Keep the last rotation value
                        imageView.rotation = 1440f // 1080 + 360
                    }
                }
            }
        }
    }

    private fun interpolateOvershoot(progress: Float): Float {
        // Overshoot interpolation
        return if (progress < 0.6f) {
            2.5f * progress * progress
        } else {
            1.2f - (1.0f - progress) * (1.0f - progress) * 0.2f
        }
    }

    private fun interpolateSettling(progress: Float): Float {
        // Smooth settling function
        return 1f - (1f - progress) * (1f - progress) * (1f - progress)
    }
    
    private fun checkAuthentication() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authRepository = AuthRepository(this@BicepAnimationActivity)
                isLoggedIn = authRepository.isLoggedIn()
                authCheckCompleted = true
                Log.d(TAG, "Auth check completed: isLoggedIn = $isLoggedIn")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking authentication", e)
                isLoggedIn = false
                authCheckCompleted = true
            }
        }
    }
    
    private fun navigateToNextScreen() {
        if (isLoggedIn) {
            Log.d(TAG, "User is logged in, navigating to MainActivity")
            startActivity(Intent(this@BicepAnimationActivity, MainActivity::class.java))
        } else {
            Log.d(TAG, "User is not logged in, navigating to LoginActivity")
            startActivity(Intent(this@BicepAnimationActivity, LoginActivity::class.java))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "onCreate started")
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()

            // Create a container layout
            val container = FrameLayout(this).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }

            // Create the ImageView for our animation
            val size = (288 * resources.displayMetrics.density).toInt() // 288dp
            imageView = ImageView(this).apply {
                setImageResource(R.drawable.test_square)
                scaleX = 0f
                scaleY = 0f
                tag = "square"
                
                // Set fixed size for the image
                layoutParams = FrameLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER
                }
            }

            // Add ImageView to container
            container.addView(imageView)
            setContentView(container)

            // Start authentication check immediately
            checkAuthentication()
            
            // Start animation after a delay
            handler.postDelayed({
                Log.d(TAG, "Animation started at ${System.currentTimeMillis()}")
                handler.post(animationRunnable)
            }, 500)

            Log.d(TAG, "onCreate completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in BicepAnimationActivity", e)
        }
    }

    override fun onDestroy() {
        try {
            handler.removeCallbacks(animationRunnable)
            super.onDestroy()
            Log.d(TAG, "onDestroy")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
} 