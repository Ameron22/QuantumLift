package com.example.gymtracker.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.graphics.Rect
import com.example.gymtracker.R
import com.example.gymtracker.utils.FloatingTimerManager
import com.example.gymtracker.utils.TimerServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingTimerService : Service() {
    
    companion object {
        private const val TAG = "FloatingTimerService"
        var isTimerRunning = false
        var remainingTime = 0
        var isBreakRunning = false
        var exerciseName = ""
        var isPaused = false
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var timerTextView: TextView
    private lateinit var exerciseTextView: TextView
    private lateinit var pausePlayButton: ImageButton
    
    // Delete zone components
    private lateinit var deleteZoneView: View
    private lateinit var deleteZoneIcon: ImageView
    private var isDeleteZoneVisible = false
    private var isDragging = false
    private var isOverDeleteZone = false
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Window parameters for floating timer
    private val layoutParams = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.BOTTOM or Gravity.END
        x = 50
        y = 50
    }
    
    // Window parameters for delete zone
    private val deleteZoneParams = WindowManager.LayoutParams().apply {
        width = 250
        height = 250
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 0
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingView()
        setupDeleteZone()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            "START_TIMER" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                startTimer(time, isBreak, exercise)
            }
            "UPDATE_TIMER" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                updateTimer(time, isBreak, exercise)
            }
            "STOP_TIMER" -> {
                stopTimer()
            }
            "PAUSE_TIMER" -> {
                pauseTimer()
            }
            "HIDE_DELETE_ZONE" -> {
                hideDeleteZone()
            }
        }
        
        return START_STICKY
    }
    
    private fun setupDeleteZone() {
        // Create delete zone layout
        deleteZoneView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.delete_zone_background)
            
            // Add a border to make it more visible
            setPadding(10, 10, 10, 10)
            
            // Add bin icon
            deleteZoneIcon = ImageView(this@FloatingTimerService).apply {
                setImageResource(R.drawable.bin_icon)
                layoutParams = LinearLayout.LayoutParams(120, 120)
            }
            addView(deleteZoneIcon)
            
            // Add click handler as backup
            setOnClickListener {
                Log.d(TAG, "Delete zone clicked - removing floating timer")
                FloatingTimerManager.onTimerDeleted?.invoke()
                forceRemoveFloatingView()
                stopTimer()
            }
        }
        
        // Ensure delete zone starts hidden
        isDeleteZoneVisible = false
        
        Log.d(TAG, "Delete zone setup complete - hidden by default")
    }
    
    private fun showDeleteZone() {
        if (!isDeleteZoneVisible) {
            try {
                // Ensure delete zone is positioned correctly at the very bottom
                deleteZoneParams.y = 0 // Position it at the very bottom of the screen
                deleteZoneParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                windowManager.addView(deleteZoneView, deleteZoneParams)
                isDeleteZoneVisible = true
                
                // Get the actual position of the delete zone window
                val deleteZoneRect = Rect()
                deleteZoneView.getGlobalVisibleRect(deleteZoneRect)
                Log.d(TAG, "COLLISION_DEBUG: Delete zone shown at position: ${deleteZoneParams.x}, ${deleteZoneParams.y} with gravity: ${deleteZoneParams.gravity}, screenHeight: ${resources.displayMetrics.heightPixels}")
                Log.d(TAG, "COLLISION_DEBUG: Actual delete zone window bounds: $deleteZoneRect")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing delete zone: ${e.message}")
            }
        }
    }
    
    private fun hideDeleteZone() {
        if (isDeleteZoneVisible) {
            try {
                windowManager.removeView(deleteZoneView)
                isDeleteZoneVisible = false
                Log.d(TAG, "Delete zone hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding delete zone: ${e.message}")
            }
        }
    }
    
    private fun isInDeleteZone(x: Int, y: Int): Boolean {
        // Use the actual window position instead of getGlobalVisibleRect which doesn't work for overlay windows
        val floatingCenterX = layoutParams.x + (floatingView.width / 2)
        val floatingCenterY = layoutParams.y + (floatingView.height / 2)
        
        // Use the actual delete zone window position to match the visual bin icon
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val zoneCenterX = screenWidth / 2
        // The delete zone appears at the TOP, so its center is at the top of the screen
        val zoneCenterY = (deleteZoneParams.height / 2) // Center of the delete zone at top
        val zoneRadius = 125 // Half of the delete zone size
        
        val distance = Math.sqrt(((floatingCenterX - zoneCenterX) * (floatingCenterX - zoneCenterX) + (floatingCenterY - zoneCenterY) * (floatingCenterY - zoneCenterY)).toDouble())
        val isInZone = distance <= zoneRadius
        
        Log.d(TAG, "COLLISION_DEBUG: floating center($floatingCenterX, $floatingCenterY), zone center($zoneCenterX, $zoneCenterY), distance=$distance, radius=$zoneRadius, isInZone=$isInZone, screenHeight=$screenHeight, calculatedY=${screenHeight - deleteZoneParams.y - (deleteZoneParams.height / 2)}")
        
        return isInZone
    }
    
    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_timer, null)
        timerTextView = floatingView.findViewById(R.id.timer_text)
        exerciseTextView = floatingView.findViewById(R.id.exercise_text)
        pausePlayButton = floatingView.findViewById(R.id.pause_play_button)
        
        // Debug logging
        Log.d(TAG, "Pause play button found: ${pausePlayButton != null}")
        
        // Make the view draggable with delete zone functionality
        floatingView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Store initial touch position and current window position
                    view.tag = Triple(event.rawX, event.rawY, Pair(layoutParams.x, layoutParams.y))
                    isDragging = false
                    Log.d(TAG, "Touch started at: ${event.rawX}, ${event.rawY}, window at: ${layoutParams.x}, ${layoutParams.y}")
                }
                MotionEvent.ACTION_MOVE -> {
                    val touchData = view.tag as? Triple<Float, Float, Pair<Int, Int>>
                    touchData?.let { (initialX, initialY, initialWindowPos) ->
                        // Calculate movement delta
                        val deltaX = event.rawX - initialX
                        val deltaY = event.rawY - initialY
                        
                        // Check if we've moved enough to start dragging
                        if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                            isDragging = true
                            Log.d(TAG, "Started dragging - showing delete zone")
                            showDeleteZone()
                        }
                        
                        if (isDragging) {
                            // Update position from the initial window position
                            layoutParams.x = (initialWindowPos.first - deltaX).toInt()
                            layoutParams.y = (initialWindowPos.second - deltaY).toInt()
                            
                            try {
                                windowManager.updateViewLayout(floatingView, layoutParams)
                                
                                // Check if we're in the delete zone using center point
                                val isInDelete = isInDeleteZone(0, 0) // Parameters ignored, using center point
                                isOverDeleteZone = isInDelete // Track the state
                                Log.d(TAG, "COLLISION_DEBUG: ACTION_MOVE: isInDelete=$isInDelete, isOverDeleteZone=$isOverDeleteZone")
                                
                                // Also check proximity for visual feedback
                                val floatingCenterX = layoutParams.x + (floatingView.width / 2)
                                val floatingCenterY = layoutParams.y + (floatingView.height / 2)
                                val screenWidth = resources.displayMetrics.widthPixels
                                val screenHeight = resources.displayMetrics.heightPixels
                                val zoneCenterX = screenWidth / 2
                                val zoneCenterY = (deleteZoneParams.height / 2)
                                val distance = Math.sqrt(((floatingCenterX - zoneCenterX) * (floatingCenterX - zoneCenterX) + (floatingCenterY - zoneCenterY) * (floatingCenterY - zoneCenterY)).toDouble())
                                val isNearZone = distance <= 200 // Larger radius for visual feedback
                                
                                if (isInDelete) {
                                    // Highlight delete zone with pulsing effect
                                    deleteZoneView.alpha = 1.0f
                                    deleteZoneView.scaleX = 1.4f
                                    deleteZoneView.scaleY = 1.4f
                                    // Add pulsing animation
                                    deleteZoneView.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).start()
                                    // Add a glowing red background
                                    deleteZoneView.setBackgroundColor(android.graphics.Color.parseColor("#FFFF4444"))
                                } else if (isNearZone) {
                                    // Show proximity feedback with smooth animation
                                    deleteZoneView.alpha = 0.95f
                                    deleteZoneView.scaleX = 1.2f
                                    deleteZoneView.scaleY = 1.2f
                                    deleteZoneView.setBackgroundColor(android.graphics.Color.parseColor("#FFFF6666"))
                                } else {
                                    // Normal delete zone appearance with smooth transition
                                    deleteZoneView.alpha = 0.85f
                                    deleteZoneView.scaleX = 1.0f
                                    deleteZoneView.scaleY = 1.0f
                                    // Reset to gradient background
                                    deleteZoneView.setBackgroundResource(R.drawable.delete_zone_background)
                                }
                                
                                Log.d(TAG, "COLLISION_DEBUG: Moved by delta: $deltaX, $deltaY, new pos: ${layoutParams.x}, ${layoutParams.y}, in delete zone: $isInDelete")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating view layout: ${e.message}")
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Touch ended at: ${event.rawX}, ${event.rawY}, final pos: ${layoutParams.x}, ${layoutParams.y}, isDragging: $isDragging")
                    
                    if (isDragging) {
                        // Only remove if the window was over the delete zone when dropped
                        Log.d(TAG, "COLLISION_DEBUG: ACTION_UP: isOverDeleteZone = $isOverDeleteZone")
                        
                        if (isOverDeleteZone) {
                            Log.d(TAG, "COLLISION_DEBUG: Floating timer dropped in delete zone - removing")
                            // Notify that timer was deleted
                            FloatingTimerManager.onTimerDeleted?.invoke()
                            forceRemoveFloatingView()
                            stopTimer()
                        } else {
                            Log.d(TAG, "COLLISION_DEBUG: Floating timer not dropped in delete zone - keeping timer")
                        }
                        
                        // Reset state and hide delete zone
                        isOverDeleteZone = false
                        hideDeleteZone()
                        isDragging = false
                    } else {
                        Log.d(TAG, "ACTION_UP: Not dragging, ignoring")
                    }
                }
            }
            true
        }
        
        // Add pause/play button functionality
        pausePlayButton.setOnClickListener {
            togglePausePlay()
        }
        
        // Add long press handler for testing delete functionality
        floatingView.setOnLongClickListener {
            Log.d(TAG, "Long press detected - testing delete zone")
            testDeleteZone()
            true
        }
        
        // Set initial icon
        updatePausePlayButton()
    }
    
    private fun togglePausePlay() {
        if (isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }
    
    private fun pauseTimer() {
        Log.d(TAG, "Pausing timer")
        isPaused = true
        updatePausePlayButton()
        FloatingTimerManager.onPauseRequest?.invoke()
    }
    
    private fun resumeTimer() {
        Log.d(TAG, "Resuming timer")
        isPaused = false
        updatePausePlayButton()
        FloatingTimerManager.onResumeRequest?.invoke()
    }
    
    private fun updatePausePlayButton() {
        if (isPaused) {
            pausePlayButton.setImageResource(android.R.drawable.ic_media_play) // Play icon from Android library
            pausePlayButton.setColorFilter(android.graphics.Color.BLACK) // Make play icon black
        } else {
            pausePlayButton.setImageResource(android.R.drawable.ic_media_pause) // Use Android library pause icon
            pausePlayButton.setColorFilter(android.graphics.Color.BLACK) // Make pause icon black
        }
        Log.d(TAG, "Updated pause/play button - isPaused: $isPaused")
    }
    
    private fun startTimer(initialTime: Int, isBreak: Boolean, exercise: String) {
        try {
            Log.d(TAG, "Starting floating timer: $initialTime seconds, break: $isBreak, exercise: $exercise")
            
            isTimerRunning = true
            isPaused = false
            remainingTime = initialTime
            isBreakRunning = isBreak
            exerciseName = exercise
            
            // Show the floating window
            windowManager.addView(floatingView, layoutParams)
            
            // Update display immediately
            updateTimerDisplay()
            updatePausePlayButton()
            
            // Don't show delete zone immediately - only show during dragging
            
            Log.d(TAG, "Floating timer started (display only)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun updateTimer(time: Int, isBreak: Boolean, exercise: String) {
        Log.d(TAG, "Updating floating timer: time=$time, isBreak=$isBreak, exercise=$exercise")
        remainingTime = time
        isBreakRunning = isBreak
        exerciseName = exercise
        updateTimerDisplay()
    }
    
    private fun updateTimerDisplay() {
        try {
            val timeString = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
            val title = if (isBreakRunning) "Break" else "Exercise"
            
            Log.d(TAG, "Updating display: time=$timeString, mode=$title, exercise=$exerciseName")
            
            timerTextView.text = timeString
            exerciseTextView.text = "$title: $exerciseName"
            
            // Change background drawable based on timer type
            val backgroundDrawable = if (isBreakRunning) {
                R.drawable.floating_timer_background_break
            } else {
                R.drawable.floating_timer_background_exercise
            }
            floatingView.setBackgroundResource(backgroundDrawable)
            
            Log.d(TAG, "Display updated: background=${if (isBreakRunning) "RED" else "GREEN"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating timer display: ${e.message}")
        }
    }
    
    private fun testDeleteZone() {
        Log.d(TAG, "Testing delete zone functionality")
        
        // Log the floating window position
        val floatingViewRect = Rect()
        floatingView.getGlobalVisibleRect(floatingViewRect)
        Log.d(TAG, "Floating window rect: $floatingViewRect")
        Log.d(TAG, "Floating window center: (${floatingViewRect.centerX()}, ${floatingViewRect.centerY()})")
        
        val isInZone = isInDeleteZone(0, 0)
        Log.d(TAG, "Test result: isInZone=$isInZone")
        
        if (isInZone) {
            Log.d(TAG, "Test: Floating timer is in delete zone - removing")
            FloatingTimerManager.onTimerDeleted?.invoke()
            forceRemoveFloatingView()
            stopTimer()
        } else {
            Log.d(TAG, "Test: Floating timer is NOT in delete zone")
        }
    }

    private fun forceRemoveFloatingView() {
        Log.d(TAG, "Force removing floating view")
        try {
            windowManager.removeView(floatingView)
            Log.d(TAG, "Floating view force removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error force removing floating view: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun stopTimer() {
        Log.d(TAG, "Stopping floating timer")
        
        isTimerRunning = false
        isPaused = false
        
        try {
            Log.d(TAG, "Attempting to remove floating view")
            windowManager.removeView(floatingView)
            Log.d(TAG, "Floating view removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating view: ${e.message}")
            e.printStackTrace()
        }
        
        // Hide delete zone if it's visible
        hideDeleteZone()
        
        Log.d(TAG, "Stopping service")
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Floating timer service destroyed")
        isTimerRunning = false
        isPaused = false
        
        // Clean up delete zone
        try {
            if (isDeleteZoneVisible) {
                windowManager.removeView(deleteZoneView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing delete zone on destroy: ${e.message}")
        }
    }
} 