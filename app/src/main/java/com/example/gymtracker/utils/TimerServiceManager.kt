package com.example.gymtracker.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.gymtracker.services.TimerService

object TimerServiceManager {
    
    fun startTimer(context: Context, remainingTime: Int, isBreak: Boolean, exerciseName: String) {
        try {
            Log.d("TimerServiceManager", "Starting timer service: $remainingTime seconds, break: $isBreak, exercise: $exerciseName")
            
            val intent = Intent(context, TimerService::class.java).apply {
                action = "START_TIMER"
                putExtra("remaining_time", remainingTime)
                putExtra("is_break", isBreak)
                putExtra("exercise_name", exerciseName)
            }
            
            Log.d("TimerServiceManager", "Starting service with intent: $intent")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Log.d("TimerServiceManager", "Using startForegroundService")
                context.startForegroundService(intent)
            } else {
                Log.d("TimerServiceManager", "Using startService")
                context.startService(intent)
            }
            
            Log.d("TimerServiceManager", "Service start command sent")
        } catch (e: Exception) {
            Log.e("TimerServiceManager", "Error starting timer service: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun updateTimer(context: Context, remainingTime: Int, isBreak: Boolean, exerciseName: String) {
        Log.d("TimerServiceManager", "Updating timer service: $remainingTime seconds, break: $isBreak, exercise: $exerciseName")
        
        val intent = Intent(context, TimerService::class.java).apply {
            action = "UPDATE_TIMER"
            putExtra("remaining_time", remainingTime)
            putExtra("is_break", isBreak)
            putExtra("exercise_name", exerciseName)
        }
        
        context.startService(intent)
    }
    
    fun pauseTimer(context: Context) {
        Log.d("TimerServiceManager", "Pausing timer service")
        
        val intent = Intent(context, TimerService::class.java).apply {
            action = "PAUSE_TIMER"
        }
        
        context.startService(intent)
    }
    
    fun resumeTimer(context: Context) {
        Log.d("TimerServiceManager", "Resuming timer service")
        
        val intent = Intent(context, TimerService::class.java).apply {
            action = "RESUME_TIMER"
        }
        
        context.startService(intent)
    }
    
    fun stopTimer(context: Context) {
        Log.d("TimerServiceManager", "Stopping timer service")
        
        // Set the service state to stopped immediately
        TimerService.isTimerRunning = false
        TimerService.shouldShowNotification = false
        
        // Cancel all notifications immediately
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            Log.d("TimerServiceManager", "All notifications cancelled")
        } catch (e: Exception) {
            Log.e("TimerServiceManager", "Error cancelling notifications: ${e.message}")
        }
        
        val intent = Intent(context, TimerService::class.java).apply {
            action = "STOP_TIMER"
        }
        
        // Stop the service directly first
        try {
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e("TimerServiceManager", "Error stopping service: ${e.message}")
        }
        
        // Then send the stop command
        context.startService(intent)
    }
    
    fun isTimerRunning(): Boolean {
        return TimerService.isTimerRunning
    }
    
    fun getRemainingTime(): Int {
        return TimerService.remainingTime
    }
    
    fun isBreakRunning(): Boolean {
        return TimerService.isBreakRunning
    }
    
    fun getExerciseName(): String {
        return TimerService.exerciseName
    }
    
    fun isTimerPaused(): Boolean {
        return TimerService.isPaused
    }
    
    fun setOnTimerUpdateCallback(callback: (Int, Boolean, String) -> Unit) {
        TimerService.onTimerUpdate = callback
    }
    
    fun clearOnTimerUpdateCallback() {
        TimerService.onTimerUpdate = null
    }
} 