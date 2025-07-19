package com.example.gymtracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gymtracker.MainActivity
import com.example.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

class TimerService : Service() {
    
    companion object {
        private const val TAG = "TimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_channel"
        private const val CHANNEL_NAME = "Timer Service"
        
        // Timer state
        var isTimerRunning = false
        var remainingTime = 0
        var isBreakRunning = false
        var exerciseName = ""
        var isPaused = false
        var shouldShowNotification = true
        var isFloatingMode = false
        
        // Navigation parameters for floating timer
        var exerciseId = 0
        var sessionId = 0L
        var workoutId = 0
        
        // Callbacks
        var onTimerUpdate: ((Int, Boolean, String) -> Unit)? = null
        var onPauseRequest: (() -> Unit)? = null
        var onResumeRequest: (() -> Unit)? = null
        var onTimerDeleted: (() -> Unit)? = null
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Floating timer components
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var timerTextView: TextView? = null
    private var exerciseTextView: TextView? = null
    private var pausePlayButton: ImageButton? = null
    private var gestureDetector: GestureDetector? = null
    
    // Delete zone components
    private var deleteZoneView: View? = null
    private var deleteZoneIcon: ImageView? = null
    private var isDeleteZoneVisible = false
    private var isDragging = false
    private var isOverDeleteZone = false
    private var dragStarted = false
    private var downX = 0f
    private var downY = 0f
    
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
        createNotificationChannel()
        setupGestureDetector()
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
            "RESUME_TIMER" -> {
                resumeTimer()
            }
            "START_FLOATING" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                val exId = intent.getIntExtra("exercise_id", 0)
                val sessId = intent.getLongExtra("session_id", 0L)
                val wId = intent.getIntExtra("workout_id", 0)
                startFloatingTimer(time, isBreak, exercise, exId, sessId, wId)
            }
            "STOP_FLOATING" -> {
                stopFloatingTimer()
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
    
    private fun startTimer(initialTime: Int, isBreak: Boolean, exercise: String, exId: Int = 0, sessId: Long = 0L, wId: Int = 0) {
        try {
            Log.d(TAG, "Starting timer: $initialTime seconds, break: $isBreak, exercise: $exercise")
            
            isTimerRunning = true
            shouldShowNotification = true
            isPaused = false
            remainingTime = initialTime
            isBreakRunning = isBreak
            exerciseName = exercise
            exerciseId = exId
            sessionId = sessId
            workoutId = wId
            
            // Create and show notification only - no countdown in service
            val notification = createNotification(remainingTime, isBreakRunning, exerciseName)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting timer: ${e.message}")
            e.printStackTrace()
            isTimerRunning = false
            shouldShowNotification = false
        }
    }
    
    private fun startFloatingTimer(initialTime: Int, isBreak: Boolean, exercise: String, exId: Int, sessId: Long, wId: Int) {
        try {
            Log.d(TAG, "Starting floating timer: $initialTime seconds, break: $isBreak, exercise: $exercise")
            
            // Update timer state - no countdown, just display
            remainingTime = initialTime
            isBreakRunning = isBreak
            exerciseName = exercise
            exerciseId = exId
            sessionId = sessId
            workoutId = wId
            TimerService.isFloatingMode = true
            isTimerRunning = true
            shouldShowNotification = true
            
            // Start the notification
            val notification = createNotification(remainingTime, isBreakRunning, exerciseName)
            startForeground(NOTIFICATION_ID, notification)
            
            setupFloatingView()
            
            // Show the floating window
            floatingView?.let { view ->
                windowManager.addView(view, layoutParams)
                updateFloatingTimerDisplay()
                updatePausePlayButton()
                Log.d(TAG, "Floating timer view added and updated")
            }
            
            Log.d(TAG, "Floating timer started, isPaused: $isPaused, isTimerRunning: $isTimerRunning")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun updateTimer(time: Int, isBreak: Boolean, exercise: String, exId: Int = 0, sessId: Long = 0L, wId: Int = 0) {
        try {
            Log.d(TAG, "Updating timer: $time seconds, break: $isBreak, exercise: $exercise")
            
            remainingTime = time
            isBreakRunning = isBreak
            exerciseName = exercise
            exerciseId = exId
            sessionId = sessId
            workoutId = wId
            
            updateNotification()
            if (TimerService.isFloatingMode) {
                updateFloatingTimerDisplay()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun pauseTimer() {
        Log.d(TAG, "Pausing timer - updating UI only, no countdown control")
        isPaused = true
        updateNotification()
        if (TimerService.isFloatingMode) {
            updatePausePlayButton()
        }
        onPauseRequest?.invoke()
    }
    
    private fun resumeTimer() {
        Log.d(TAG, "Resuming timer - updating UI only, no countdown control")
        isPaused = false
        
        // Update UI
        if (shouldShowNotification) {
            updateNotification()
        }
        if (TimerService.isFloatingMode) {
            updatePausePlayButton()
            updateFloatingTimerDisplay()
        }
        
        // Notify the app
        onResumeRequest?.invoke()
        
        Log.d(TAG, "Timer resumed successfully")
    }
    
    // Countdown is now handled by ExerciseScreen - service only displays
    
    private fun stopTimer() {
        Log.d(TAG, "Stopping timer")
        
        isTimerRunning = false
        shouldShowNotification = false
        isPaused = false
        
        // Remove notification
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification: ${e.message}")
        }
        
        // Remove floating timer if active
        stopFloatingTimer()
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun stopFloatingTimer() {
        Log.d(TAG, "Stopping floating timer")
        
        TimerService.isFloatingMode = false
        
        try {
            floatingView?.let { view ->
                windowManager.removeView(view)
                floatingView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating view: ${e.message}")
        }
        
        hideDeleteZone()
    }
    
    // Floating timer setup and management
    private fun setupFloatingView() {
        if (floatingView != null) return
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_timer, null)
        timerTextView = floatingView?.findViewById(R.id.timer_text)
        exerciseTextView = floatingView?.findViewById(R.id.exercise_text)
        pausePlayButton = floatingView?.findViewById(R.id.pause_play_button)

        floatingView?.let { view ->
            // Set custom outline provider for rounded corners
            val radiusPx = resources.displayMetrics.density * 25
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
            view.clipToOutline = true

            // Custom touch logic for drag vs click - exclude pause button
            val dragClickTouchListener = View.OnTouchListener { touchView, event ->
                // Don't handle touch events on the pause button - let it handle its own clicks
                if (touchView == pausePlayButton) {
                    return@OnTouchListener false
                }
                
                val CLICK_DRAG_TOLERANCE = 8
                val density = resources.displayMetrics.density
                val threshold = CLICK_DRAG_TOLERANCE * density
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.tag = Triple(event.rawX, event.rawY, Pair(layoutParams.x, layoutParams.y))
                        view.setTag(FLOATING_TIMER_TOUCH_START_TIME, System.currentTimeMillis())
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
                            val touchData = view.tag as? Triple<Float, Float, Pair<Int, Int>>
                            touchData?.let { (_, _, initialWindowPos) ->
                                layoutParams.x = (initialWindowPos.first - dx).toInt()
                                layoutParams.y = (initialWindowPos.second - dy).toInt()
                                val screenWidth = resources.displayMetrics.widthPixels
                                val screenHeight = resources.displayMetrics.heightPixels
                                val timerWidth = view.width
                                val timerHeight = view.height
                                layoutParams.x = layoutParams.x.coerceIn(0, screenWidth - timerWidth)
                                layoutParams.y = layoutParams.y.coerceIn(0, screenHeight - timerHeight)
                                try {
                                    windowManager.updateViewLayout(view, layoutParams)
                                } catch (_: Exception) {}
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragStarted) {
                            if (isInDeleteZone()) {
                                onTimerDeleted?.invoke()
                                forceRemoveFloatingView()
                                stopFloatingTimer()
                            }
                            hideDeleteZone()
                            isDragging = false
                            dragStarted = false
                        } else {
                            val startTime = view.getTag(FLOATING_TIMER_TOUCH_START_TIME) as? Long ?: 0L
                            val duration = System.currentTimeMillis() - startTime
                            if (duration < 300) {
                                view.animate().alpha(0.7f).setDuration(100).withEndAction {
                                    view.animate().alpha(1.0f).setDuration(100).start()
                                }.start()
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
            
            // Apply touch listener to all views except the pause button
            fun setTouchListenerRecursivelyExcludingButton(view: View, listener: View.OnTouchListener, excludeView: View?) {
                if (view != excludeView) {
                    view.setOnTouchListener(listener)
                }
                if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        setTouchListenerRecursivelyExcludingButton(view.getChildAt(i), listener, excludeView)
                    }
                }
            }
            setTouchListenerRecursivelyExcludingButton(view, dragClickTouchListener, pausePlayButton)
            
            // Pause/play button functionality with improved click handling
            pausePlayButton?.setOnClickListener {
                Log.d(TAG, "Pause button clicked, current state: isPaused=$isPaused")
                togglePausePlay()
            }
        }
        
        setupDeleteZone()
    }
    
    private fun setupDeleteZone() {
        if (deleteZoneView != null) return
        
        deleteZoneView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.delete_zone_background)
            setPadding(10, 10, 10, 10)
            
            deleteZoneIcon = ImageView(this@TimerService).apply {
                setImageResource(R.drawable.bin_icon)
                layoutParams = LinearLayout.LayoutParams(80, 80)
            }
            addView(deleteZoneIcon)
            
            setOnClickListener {
                Log.d(TAG, "Delete zone clicked - removing floating timer only")
                onTimerDeleted?.invoke()
                forceRemoveFloatingView()
                stopFloatingTimer()
            }
        }
        
        deleteZoneView?.let { view ->
            isDeleteZoneVisible = false
            view.alpha = 0f
            view.visibility = View.INVISIBLE
        }
    }
    
    private fun showDeleteZone() {
        deleteZoneView?.let { view ->
            if (!isDeleteZoneVisible) {
                try {
                    deleteZoneParams.y = 0
                    deleteZoneParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    windowManager.addView(view, deleteZoneParams)
                    isDeleteZoneVisible = true
                    
                    view.visibility = View.VISIBLE
                    view.animate().alpha(0.85f).setDuration(200).start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing delete zone: ${e.message}")
                }
            }
        }
    }
    
    private fun hideDeleteZone() {
        deleteZoneView?.let { view ->
            if (isDeleteZoneVisible) {
                try {
                    view.animate().alpha(0f).setDuration(200).withEndAction {
                        try {
                            windowManager.removeView(view)
                            view.visibility = View.INVISIBLE
                            isDeleteZoneVisible = false
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing delete zone view: ${e.message}")
                        }
                    }.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error hiding delete zone: ${e.message}")
                }
            }
        }
    }
    
    private fun isInDeleteZone(): Boolean {
        val floatingView = this.floatingView ?: return false
        val deleteZoneView = this.deleteZoneView ?: return false
        
        val floatLoc = IntArray(2)
        floatingView.getLocationOnScreen(floatLoc)
        val floatCenterX = floatLoc[0] + floatingView.width / 2
        val floatCenterY = floatLoc[1] + floatingView.height / 2

        val binLoc = IntArray(2)
        deleteZoneView.getLocationOnScreen(binLoc)
        val binLeft = binLoc[0]
        val binTop = binLoc[1]
        val binRight = binLeft + deleteZoneView.width
        val binBottom = binTop + deleteZoneView.height

        return floatCenterX in binLeft..binRight && floatCenterY in binTop..binBottom
    }
    
    private fun togglePausePlay() {
        Log.d(TAG, "togglePausePlay called, current isPaused: $isPaused")
        if (isPaused) {
            Log.d(TAG, "Resuming timer from floating button")
            resumeTimer()
        } else {
            Log.d(TAG, "Pausing timer from floating button")
            pauseTimer()
        }
        // Force update the button immediately
        updatePausePlayButton()
    }
    
    private fun updatePausePlayButton() {
        pausePlayButton?.let { button ->
            Log.d(TAG, "Updating pause button icon, isPaused: $isPaused")
            if (isPaused) {
                button.setImageResource(android.R.drawable.ic_media_play)
                button.setColorFilter(android.graphics.Color.WHITE) // Better visibility on dark background
                Log.d(TAG, "Set button to PLAY icon")
            } else {
                button.setImageResource(android.R.drawable.ic_media_pause)
                button.setColorFilter(android.graphics.Color.WHITE) // Better visibility on dark background
                Log.d(TAG, "Set button to PAUSE icon")
            }
        } ?: run {
            Log.w(TAG, "pausePlayButton is null, cannot update button")
        }
    }
    
    private fun updateFloatingTimerDisplay() {
        try {
            val timeString = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
            val title = if (isBreakRunning) "Break" else "Exercise"
            
            timerTextView?.text = timeString
            exerciseTextView?.text = "$title: $exerciseName"
            
            val backgroundDrawable = if (isBreakRunning) {
                R.drawable.floating_timer_background_break
            } else {
                R.drawable.floating_timer_background_exercise
            }
            floatingView?.setBackgroundResource(backgroundDrawable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating floating timer display: ${e.message}")
        }
    }
    
    private fun navigateToExerciseScreen() {
        try {
            // Stop the floating timer when user taps it to navigate back to app
            stopFloatingTimer()
            
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("from_floating_timer", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing app to foreground: ${e.message}")
        }
    }
    
    private fun forceRemoveFloatingView() {
        try {
            floatingView?.let { view ->
                windowManager.removeView(view)
                floatingView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error force removing floating view: ${e.message}")
        }
    }
    
    // Notification management
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows timer progress"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(time: Int, isBreak: Boolean, exercise: String): Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("from_notification", true)
                putExtra("exercise_id", exerciseId)
                putExtra("session_id", sessionId)
                putExtra("workout_id", workoutId)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val timeString = String.format("%02d:%02d", time / 60, time % 60)
            val title = if (isBreak) "Break Time" else "Exercise Time"
            val content = "$exercise - $timeString"
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.new_clock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification: ${e.message}")
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer")
                .setContentText("Timer is running")
                .setSmallIcon(R.drawable.new_clock)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }
    }
    
    private fun updateNotification() {
        if (!isTimerRunning || !shouldShowNotification) {
            return
        }
        
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification(remainingTime, isBreakRunning, exerciseName))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification: ${e.message}")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isTimerRunning = false
        isPaused = false
        
        // Clean up floating components
        try {
            floatingView?.let { view ->
                windowManager.removeView(view)
            }
            deleteZoneView?.let { view ->
                if (isDeleteZoneVisible) {
                    windowManager.removeView(view)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up views on destroy: ${e.message}")
        }
        
        // Remove notification
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification on destroy: ${e.message}")
        }
        
        // Clear callbacks
        onTimerUpdate = null
        onPauseRequest = null
        onResumeRequest = null
        onTimerDeleted = null
    }
} 