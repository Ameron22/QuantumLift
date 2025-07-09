package com.example.gymtracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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

class TimerService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_channel"
        private const val CHANNEL_NAME = "Timer Service"
        
        // Timer state
        var isTimerRunning = false
        var remainingTime = 0
        var isBreakRunning = false
        var exerciseName = ""
        var shouldShowNotification = true // New flag to control notification display
        var isPaused = false // New flag to control pause state
        
        // Callback for UI updates
        var onTimerUpdate: ((Int, Boolean, String) -> Unit)? = null
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerService", "Service started with action: ${intent?.action}")
        Log.d("TimerService", "Intent extras: ${intent?.extras}")
        
        when (intent?.action) {
            "START_TIMER" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                Log.d("TimerService", "Starting timer with: time=$time, isBreak=$isBreak, exercise=$exercise")
                startTimer(time, isBreak, exercise)
            }
            "STOP_TIMER" -> {
                Log.d("TimerService", "Stopping timer")
                stopTimer()
            }
            "UPDATE_TIMER" -> {
                val time = intent.getIntExtra("remaining_time", 0)
                val isBreak = intent.getBooleanExtra("is_break", false)
                val exercise = intent.getStringExtra("exercise_name") ?: "Exercise"
                Log.d("TimerService", "Updating timer with: time=$time, isBreak=$isBreak, exercise=$exercise")
                updateTimer(time, isBreak, exercise)
            }
            "PAUSE_TIMER" -> {
                Log.d("TimerService", "Pausing timer")
                pauseTimer()
            }
            "RESUME_TIMER" -> {
                Log.d("TimerService", "Resuming timer")
                resumeTimer()
            }
            else -> {
                Log.d("TimerService", "Unknown action: ${intent?.action}")
            }
        }
        
        return START_STICKY
    }
    
    private fun startTimer(initialTime: Int, isBreak: Boolean, exercise: String) {
        try {
            Log.d("TimerService", "Starting timer: $initialTime seconds, break: $isBreak, exercise: $exercise")
            
            isTimerRunning = true
            shouldShowNotification = true
            isPaused = false
            remainingTime = initialTime
            isBreakRunning = isBreak
            exerciseName = exercise
            
            // Create notification immediately
            val notification = createNotification(remainingTime, isBreakRunning, exerciseName)
            Log.d("TimerService", "Created notification: $notification")
            
            // Start foreground service immediately
            Log.d("TimerService", "Starting foreground service with notification")
            startForeground(NOTIFICATION_ID, notification)
            Log.d("TimerService", "Foreground service started successfully")
            
            // Start countdown
            startCountdown()
        } catch (e: Exception) {
            Log.e("TimerService", "Error starting timer: ${e.message}")
            e.printStackTrace()
            isTimerRunning = false
            shouldShowNotification = false
        }
    }
    
    private fun updateTimer(time: Int, isBreak: Boolean, exercise: String) {
        try {
            Log.d("TimerService", "Updating timer: $time seconds, break: $isBreak, exercise: $exercise")
            
            remainingTime = time
            isBreakRunning = isBreak
            exerciseName = exercise
            
            updateNotification()
            // Don't call callback since UI controls countdown
        } catch (e: Exception) {
            Log.e("TimerService", "Error updating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun pauseTimer() {
        Log.d("TimerService", "Pausing timer")
        isPaused = true
        timerJob?.cancel()
        updateNotification()
    }
    
    private fun resumeTimer() {
        Log.d("TimerService", "Resuming timer")
        isPaused = false
        startCountdown()
        updateNotification()
    }
    
    private fun startCountdown() {
        timerJob = serviceScope.launch {
            try {
                while (isActive && isTimerRunning && shouldShowNotification && !isPaused) {
                    delay(1000)
                    
                    // Only update if timer is still running and should show notification and not paused
                    if (isTimerRunning && shouldShowNotification && !isPaused) {
                        remainingTime--
                        updateNotification()
                        Log.d("TimerService", "Notification updated: $remainingTime seconds remaining")
                    } else {
                        // Timer stopped, notification disabled, or paused, break out of loop
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("TimerService", "Error in countdown job: ${e.message}")
                e.printStackTrace()
                isTimerRunning = false
                shouldShowNotification = false
                stopForeground(true)
                stopSelf()
            }
        }
    }
    
    private fun stopTimer() {
        Log.d("TimerService", "Stopping timer")
        
        isTimerRunning = false
        shouldShowNotification = false
        isPaused = false
        timerJob?.cancel()
        
        // Remove the notification immediately and aggressively
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.cancelAll() // Cancel all notifications from this app
            Log.d("TimerService", "Notification cancelled")
        } catch (e: Exception) {
            Log.e("TimerService", "Error cancelling notification: ${e.message}")
        }
        
        // Force stop the foreground service and kill the service
        stopForeground(true)
        
        // Kill the service process to ensure it stops completely
        android.os.Process.killProcess(android.os.Process.myPid())
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        Log.d("TimerService", "Creating notification channel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Higher importance for faster display
            ).apply {
                description = "Shows timer progress"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null) // No sound for faster processing
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("TimerService", "Notification channel created: $CHANNEL_ID")
        } else {
            Log.d("TimerService", "Notification channel not needed for this Android version")
        }
    }
    
    private fun createNotification(time: Int, isBreak: Boolean, exercise: String): Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val timeString = String.format("%02d:%02d", time / 60, time % 60)
            val title = if (isBreak) "Break Time" else "Exercise Time"
            val content = "$exercise - $timeString"
            
            Log.d("TimerService", "Creating notification: title='$title', content='$content', time='$timeString'")
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.new_clock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Higher priority for faster display
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .build()
        } catch (e: Exception) {
            Log.e("TimerService", "Error creating notification: ${e.message}")
            e.printStackTrace()
            // Return a simple fallback notification
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
    
    private fun createSimpleNotification(): Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Running")
                .setContentText("Exercise timer is active")
                .setSmallIcon(R.drawable.new_clock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
        } catch (e: Exception) {
            Log.e("TimerService", "Error creating simple notification: ${e.message}")
            e.printStackTrace()
            // Return a minimal fallback notification
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer")
                .setContentText("Timer is running")
                .setSmallIcon(R.drawable.new_clock)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
    
    private fun updateNotification() {
        // Don't update notification if timer is not running or should not show notification
        if (!isTimerRunning || !shouldShowNotification) {
            Log.d("TimerService", "Not updating notification - timer not running or notification disabled")
            return
        }
        
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification(remainingTime, isBreakRunning, exerciseName))
        } catch (e: Exception) {
            Log.e("TimerService", "Error updating notification: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerService", "Service destroyed")
        timerJob?.cancel()
        isTimerRunning = false
        isPaused = false
        
        // Remove the notification when service is destroyed
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d("TimerService", "Notification cancelled on service destroy")
        } catch (e: Exception) {
            Log.e("TimerService", "Error cancelling notification on service destroy: ${e.message}")
        }
        
        onTimerUpdate = null // Clear callback to prevent memory leaks
    }
} 