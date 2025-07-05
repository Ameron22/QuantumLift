package com.example.gymtracker.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
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
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.view.ViewGroup

private const val FLOATING_TIMER_TOUCH_START_TIME = -101

private fun setTouchListenerRecursively(view: View, listener: View.OnTouchListener) {
    view.setOnTouchListener(listener)
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            setTouchListenerRecursively(view.getChildAt(i), listener)
        }
    }
}

class FloatingTimerService : Service() {
    
    companion object {
        private const val TAG = "FloatingTimerService"
        var isTimerRunning = false
        var remainingTime = 0
        var isBreakRunning = false
        var exerciseName = ""
        var isPaused = false
        // Navigation parameters
        var exerciseId = 0
        var sessionId = 0L
        var workoutId = 0
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var timerTextView: TextView
    private lateinit var exerciseTextView: TextView
    private lateinit var pausePlayButton: ImageButton
    private lateinit var gestureDetector: GestureDetector
    
    // Delete zone components
    private lateinit var deleteZoneView: View
    private lateinit var deleteZoneIcon: ImageView
    private var isDeleteZoneVisible = false
    private var isDragging = false
    private var isOverDeleteZone = false
    private var dragStarted = false
    private var downX = 0f
    private var downY = 0f
    
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
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
        setupGestureDetector()
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
                val exId = intent.getIntExtra("exercise_id", 0)
                val sessId = intent.getLongExtra("session_id", 0L)
                val wId = intent.getIntExtra("workout_id", 0)
                startTimer(time, isBreak, exercise, exId, sessId, wId)
            }
            "UPDATE_TIMER" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                val exId = intent.getIntExtra("exercise_id", 0)
                val sessId = intent.getLongExtra("session_id", 0L)
                val wId = intent.getIntExtra("workout_id", 0)
                updateTimer(time, isBreak, exercise, exId, sessId, wId)
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
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Log.d(TAG, "Gesture detected: single tap confirmed")
                if (!isDragging) {
                    Log.d(TAG, "Single tap confirmed - bringing app to foreground")
                    navigateToExerciseScreen()
                    return true
                }
                return false
            }
        })
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
                layoutParams = LinearLayout.LayoutParams(80, 80) // Smaller size
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
        
        // Ensure delete zone starts hidden and invisible
        isDeleteZoneVisible = false
        deleteZoneView.alpha = 0f
        deleteZoneView.visibility = View.INVISIBLE
        
        Log.d(TAG, "Delete zone setup complete - hidden and invisible by default")
    }
    
    private fun showDeleteZone() {
        if (!isDeleteZoneVisible) {
            try {
                // Ensure delete zone is positioned correctly at the very bottom
                deleteZoneParams.y = 0 // Position it at the very bottom of the screen
                deleteZoneParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                windowManager.addView(deleteZoneView, deleteZoneParams)
                isDeleteZoneVisible = true
                
                // Make it visible with animation
                deleteZoneView.visibility = View.VISIBLE
                deleteZoneView.animate().alpha(0.85f).setDuration(200).start()
                
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
                // Fade out animation before removing
                deleteZoneView.animate().alpha(0f).setDuration(200).withEndAction {
                    try {
                        windowManager.removeView(deleteZoneView)
                        deleteZoneView.visibility = View.INVISIBLE
                        isDeleteZoneVisible = false
                        Log.d(TAG, "Delete zone hidden and removed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing delete zone view: ${e.message}")
                    }
                }.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding delete zone: ${e.message}")
            }
        }
    }
    
    private fun isInDeleteZone(): Boolean {
        // Get floating timer center in screen coordinates
        val floatLoc = IntArray(2)
        floatingView.getLocationOnScreen(floatLoc)
        val floatCenterX = floatLoc[0] + floatingView.width / 2
        val floatCenterY = floatLoc[1] + floatingView.height / 2

        // Get delete zone bounds in screen coordinates
        val binLoc = IntArray(2)
        deleteZoneView.getLocationOnScreen(binLoc)
        val binLeft = binLoc[0]
        val binTop = binLoc[1]
        val binRight = binLeft + deleteZoneView.width
        val binBottom = binTop + deleteZoneView.height

        // Check if floating timer center is inside the bin
        return floatCenterX in binLeft..binRight && floatCenterY in binTop..binBottom
    }
    
    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_timer, null)
        timerTextView = floatingView.findViewById(R.id.timer_text)
        exerciseTextView = floatingView.findViewById(R.id.exercise_text)
        pausePlayButton = floatingView.findViewById(R.id.pause_play_button)

        // Set a custom outline provider for true rounded corners
        val radiusPx = resources.displayMetrics.density * 25 // 25dp to px
        floatingView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        floatingView.clipToOutline = true

        // Debug logging
        Log.d(TAG, "Pause play button found: ${pausePlayButton != null}")
        
        // Remove click listeners from timerTextView and exerciseTextView
        timerTextView.setOnClickListener(null)
        exerciseTextView.setOnClickListener(null)

        // Custom touch logic for drag vs click
        val dragClickTouchListener = View.OnTouchListener { view, event ->
            val CLICK_DRAG_TOLERANCE = 8 // dp
            val density = resources.displayMetrics.density
            val threshold = CLICK_DRAG_TOLERANCE * density
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    floatingView.tag = Triple(event.rawX, event.rawY, Pair(layoutParams.x, layoutParams.y))
                    floatingView.setTag(FLOATING_TIMER_TOUCH_START_TIME, System.currentTimeMillis())
                    isDragging = false
                    dragStarted = false
                    downX = event.rawX
                    downY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragStarted && (Math.abs(dx) > threshold || Math.abs(dy) > threshold)) {
                        dragStarted = true
                        isDragging = true
                        showDeleteZone()
                    }
                    if (dragStarted) {
                        val touchData = floatingView.tag as? Triple<Float, Float, Pair<Int, Int>>
                        touchData?.let { (_, _, initialWindowPos) ->
                            layoutParams.x = (initialWindowPos.first - dx).toInt()
                            layoutParams.y = (initialWindowPos.second - dy).toInt()
                            val screenWidth = resources.displayMetrics.widthPixels
                            val screenHeight = resources.displayMetrics.heightPixels
                            val timerWidth = floatingView.width
                            val timerHeight = floatingView.height
                            layoutParams.x = layoutParams.x.coerceIn(0, screenWidth - timerWidth)
                            layoutParams.y = layoutParams.y.coerceIn(0, screenHeight - timerHeight)
                            try {
                                windowManager.updateViewLayout(floatingView, layoutParams)
                            } catch (_: Exception) {}
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val isPauseButton = run {
                        val location = IntArray(2)
                        pausePlayButton.getLocationOnScreen(location)
                        val left = location[0]
                        val top = location[1]
                        val right = left + pausePlayButton.width
                        val bottom = top + pausePlayButton.height
                        val x = event.rawX.toInt()
                        val y = event.rawY.toInt()
                        x in left..right && y in top..bottom
                    }
                    if (isPauseButton) {
                        // Only trigger pause/play, do not trigger go-to-app
                        togglePausePlay()
                        isDragging = false
                        dragStarted = false
                        return@OnTouchListener true
                    }
                    if (dragStarted) {
                        if (isInDeleteZone()) {
                            forceRemoveFloatingView()
                            // Do NOT call stopTimer() or FloatingTimerManager.onTimerDeleted?.invoke()
                        }
                        isOverDeleteZone = false
                        hideDeleteZone()
                        isDragging = false
                        dragStarted = false
                    } else {
                        val startTime = floatingView.getTag(FLOATING_TIMER_TOUCH_START_TIME) as? Long ?: 0L
                        val duration = System.currentTimeMillis() - startTime
                        if (duration < 300) {
                            floatingView.animate().alpha(0.7f).setDuration(100).withEndAction {
                                floatingView.animate().alpha(1.0f).setDuration(100).start()
                            }.start()
                            android.widget.Toast.makeText(this, "Opening app!", android.widget.Toast.LENGTH_SHORT).show()
                            navigateToExerciseScreen()
                        }
                    }
                    isDragging = false
                    dragStarted = false
                    true
                }
                else -> true
            }
        }
        setTouchListenerRecursively(floatingView, dragClickTouchListener)
        
        // Add pause/play button functionality
        pausePlayButton.setOnClickListener {
            togglePausePlay()
        }
        
        // Add click handler to navigate back to exercise screen
        floatingView.setOnClickListener {
            Log.d(TAG, "Floating timer clicked - isDragging: $isDragging")
            if (!isDragging) {
                Log.d(TAG, "Floating timer clicked - bringing app to foreground")
                // Add visual feedback
                floatingView.animate().alpha(0.7f).setDuration(100).withEndAction {
                    floatingView.animate().alpha(1.0f).setDuration(100).start()
                }.start()
                // Add a simple toast for testing
                android.widget.Toast.makeText(this, "Opening app!", android.widget.Toast.LENGTH_SHORT).show()
                navigateToExerciseScreen()
            } else {
                Log.d(TAG, "Floating timer clicked but was dragging - ignoring click")
            }
        }
        
        // Also add a simple test click to the timer text view
        timerTextView.setOnClickListener {
            Log.d(TAG, "Timer text clicked - bringing app to foreground")
            // Add a simple toast for testing
            android.widget.Toast.makeText(this, "Opening app!", android.widget.Toast.LENGTH_SHORT).show()
            navigateToExerciseScreen()
        }
        
        // Add click handler to exercise text as well
        exerciseTextView.setOnClickListener {
            Log.d(TAG, "Exercise text clicked - bringing app to foreground")
            android.widget.Toast.makeText(this, "Opening app!", android.widget.Toast.LENGTH_SHORT).show()
            navigateToExerciseScreen()
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
    
    private fun startTimer(initialTime: Int, isBreak: Boolean, exercise: String, exId: Int, sessId: Long, wId: Int) {
        try {
            Log.d(TAG, "Starting floating timer: $initialTime seconds, break: $isBreak, exercise: $exercise")
            
            isTimerRunning = true
            isPaused = false
            remainingTime = initialTime
            isBreakRunning = isBreak
            exerciseName = exercise
            exerciseId = exId
            sessionId = sessId
            workoutId = wId
            
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
    
    private fun updateTimer(time: Int, isBreak: Boolean, exercise: String, exId: Int, sessId: Long, wId: Int) {
        Log.d(TAG, "Updating floating timer: time=$time, isBreak=$isBreak, exercise=$exercise")
        remainingTime = time
        isBreakRunning = isBreak
        exerciseName = exercise
        exerciseId = exId
        sessionId = sessId
        workoutId = wId
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
    
    private fun navigateToExerciseScreen() {
        try {
            Log.d(TAG, "Bringing app to foreground")
            
            // First, just bring the app to the foreground
            val intent = Intent(this, com.example.gymtracker.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("from_floating_timer", true)
            }
            
            startActivity(intent)
            Log.d(TAG, "App brought to foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing app to foreground: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun testDeleteZone() {
        Log.d(TAG, "Testing delete zone functionality")
        
        // Log the floating window position
        val floatingViewRect = Rect()
        floatingView.getGlobalVisibleRect(floatingViewRect)
        Log.d(TAG, "Floating window rect: $floatingViewRect")
        Log.d(TAG, "Floating window center: (${floatingViewRect.centerX()}, ${floatingViewRect.centerY()})")
        
        val isInZone = isInDeleteZone()
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