package com.example.gymtracker.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.gymtracker.services.FloatingTimerService

object FloatingTimerManager {
    
    // Callback for pause/resume requests from floating timer
    var onPauseRequest: (() -> Unit)? = null
    var onResumeRequest: (() -> Unit)? = null
    // Callback for when floating timer is deleted
    var onTimerDeleted: (() -> Unit)? = null
    
    fun startTimer(context: Context, remainingTime: Int, isBreak: Boolean, exerciseName: String, exerciseId: Int = 0, sessionId: Long = 0L, workoutId: Int = 0) {
        try {
            Log.d("FloatingTimerManager", "Starting floating timer: $remainingTime seconds, break: $isBreak, exercise: $exerciseName, exerciseId: $exerciseId, sessionId: $sessionId, workoutId: $workoutId")
            
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = "START_TIMER"
                putExtra("remaining_time", remainingTime)
                putExtra("is_break", isBreak)
                putExtra("exercise_name", exerciseName)
                putExtra("exercise_id", exerciseId)
                putExtra("session_id", sessionId)
                putExtra("workout_id", workoutId)
            }
            
            context.startService(intent)
            Log.d("FloatingTimerManager", "Floating timer service started")
        } catch (e: Exception) {
            Log.e("FloatingTimerManager", "Error starting floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun updateTimer(context: Context, remainingTime: Int, isBreak: Boolean, exerciseName: String, exerciseId: Int = 0, sessionId: Long = 0L, workoutId: Int = 0) {
        try {
            Log.d("FloatingTimerManager", "Updating floating timer: $remainingTime seconds, break: $isBreak, exercise: $exerciseName, exerciseId: $exerciseId, sessionId: $sessionId, workoutId: $workoutId")
            
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = "UPDATE_TIMER"
                putExtra("remaining_time", remainingTime)
                putExtra("is_break", isBreak)
                putExtra("exercise_name", exerciseName)
                putExtra("exercise_id", exerciseId)
                putExtra("session_id", sessionId)
                putExtra("workout_id", workoutId)
            }
            
            context.startService(intent)
            Log.d("FloatingTimerManager", "Update intent sent to service")
        } catch (e: Exception) {
            Log.e("FloatingTimerManager", "Error updating floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun pauseTimer(context: Context) {
        try {
            Log.d("FloatingTimerManager", "Pausing floating timer")
            
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = "PAUSE_TIMER"
            }
            
            context.startService(intent)
            Log.d("FloatingTimerManager", "Pause intent sent to service")
        } catch (e: Exception) {
            Log.e("FloatingTimerManager", "Error pausing floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stopTimer(context: Context) {
        try {
            Log.d("FloatingTimerManager", "Stopping floating timer")
            
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = "STOP_TIMER"
            }
            
            context.startService(intent)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e("FloatingTimerManager", "Error stopping floating timer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun isTimerRunning(): Boolean {
        return FloatingTimerService.isTimerRunning
    }
    
    fun getRemainingTime(): Int {
        return FloatingTimerService.remainingTime
    }
    
    fun isBreakRunning(): Boolean {
        return FloatingTimerService.isBreakRunning
    }
    
    fun getExerciseName(): String {
        return FloatingTimerService.exerciseName
    }
    
    fun isTimerPaused(): Boolean {
        return FloatingTimerService.isPaused
    }
    
    fun hideDeleteZone(context: Context) {
        try {
            Log.d("FloatingTimerManager", "Hiding delete zone")
            
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = "HIDE_DELETE_ZONE"
            }
            
            context.startService(intent)
            Log.d("FloatingTimerManager", "Hide delete zone intent sent to service")
        } catch (e: Exception) {
            Log.e("FloatingTimerManager", "Error hiding delete zone: ${e.message}")
            e.printStackTrace()
        }
    }
} 