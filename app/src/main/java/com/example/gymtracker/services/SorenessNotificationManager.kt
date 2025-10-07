package com.example.gymtracker.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gymtracker.R
import com.example.gymtracker.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manages soreness assessment notifications
 * Schedules and sends notifications to collect soreness feedback from users
 */
class SorenessNotificationManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SorenessNotificationManager"
        private const val CHANNEL_ID = "soreness_assessment_channel"
        private const val CHANNEL_NAME = "Soreness Assessment"
        private const val CHANNEL_DESCRIPTION = "Notifications for soreness assessment"
        
        // Notification IDs
        private const val NOTIFICATION_ID_24HR = 1000
        private const val NOTIFICATION_ID_48HR = 1001
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Schedules soreness assessment notifications for a completed workout
     */
    fun scheduleSorenessAssessment(sessionId: Long, muscleGroups: List<String>) {
        Log.d(TAG, "Scheduling soreness assessment for session $sessionId with muscles: $muscleGroups")
        
        // Schedule 24 hour notification
        scheduleNotification(
            delay = 24 * 60, // 24 hours in minutes
            sessionId = sessionId,
            muscleGroups = muscleGroups,
            assessmentDay = 1,
            notificationId = NOTIFICATION_ID_24HR + sessionId.toInt()
        )
        
        // Schedule 48 hour notification
        scheduleNotification(
            delay = 48 * 60, // 48 hours in minutes
            sessionId = sessionId,
            muscleGroups = muscleGroups,
            assessmentDay = 2,
            notificationId = NOTIFICATION_ID_48HR + sessionId.toInt()
        )
    }
    
    /**
     * Schedules a single notification
     */
    private fun scheduleNotification(
        delay: Long,
        sessionId: Long,
        muscleGroups: List<String>,
        assessmentDay: Int,
        notificationId: Int
    ) {
        coroutineScope.launch {
            try {
                // Calculate notification time (delay is in minutes)
                val notificationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delay)
                
                // Create notification intent
                val intent = createAssessmentIntent(sessionId, muscleGroups, assessmentDay)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Create notification
                val notification = createNotification(
                    muscleGroups = muscleGroups,
                    assessmentDay = assessmentDay,
                    pendingIntent = pendingIntent
                )
                
                // Schedule notification using AlarmManager for proper delayed delivery (works when app is closed)
                scheduleDelayedNotificationWithAlarmManager(notificationId, notification, delay, sessionId, muscleGroups, assessmentDay)
                
                Log.d(TAG, "Scheduled $assessmentDay day notification for session $sessionId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling notification for session $sessionId", e)
            }
        }
    }
    
    /**
     * Creates intent for soreness assessment screen
     */
    private fun createAssessmentIntent(
        sessionId: Long,
        muscleGroups: List<String>,
        assessmentDay: Int
    ): Intent {
        return Intent(context, com.example.gymtracker.SorenessAssessmentActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("muscleGroups", muscleGroups.toTypedArray())
            putExtra("assessmentDay", assessmentDay)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
    
    /**
     * Creates the notification
     */
    private fun createNotification(
        muscleGroups: List<String>,
        assessmentDay: Int,
        pendingIntent: PendingIntent
    ): NotificationCompat.Builder {
        val title = "How are you feeling?"
        val message = getNotificationMessage(muscleGroups, assessmentDay)
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
    
    /**
     * Gets appropriate notification message based on muscle groups and day
     */
    private fun getNotificationMessage(muscleGroups: List<String>, assessmentDay: Int): String {
        val timeText = if (assessmentDay == 1) "24 hours ago" else "48 hours ago"
        val muscleText = when (muscleGroups.size) {
            1 -> muscleGroups[0]
            2 -> "${muscleGroups[0]} and ${muscleGroups[1]}"
            else -> "your muscles"
        }
        
        return "Rate your soreness for $muscleText after your workout ($timeText)"
    }
    
    /**
     * Sends the notification
     */
    private fun sendNotification(notificationId: Int, notification: NotificationCompat.Builder) {
        try {
            notificationManager.notify(notificationId, notification.build())
            Log.d(TAG, "Sent notification with ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for notifications", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }
    
    /**
     * Creates notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel: $CHANNEL_ID")
        }
    }
    
    /**
     * Cancels scheduled notifications for a session
     */
    fun cancelNotifications(sessionId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_24HR + sessionId.toInt())
        notificationManager.cancel(NOTIFICATION_ID_48HR + sessionId.toInt())
        Log.d(TAG, "Cancelled notifications for session $sessionId")
    }
    
    /**
     * Checks if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Requests notification permission if needed
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle notification permission request
            Log.d(TAG, "Notification permission may be needed for Android 13+")
        }
    }
    
    /**
     * Schedules a notification to be sent after a delay using AlarmManager (works when app is closed)
     */
    private fun scheduleDelayedNotificationWithAlarmManager(
        notificationId: Int,
        notification: NotificationCompat.Builder,
        delayMinutes: Long,
        sessionId: Long,
        muscleGroups: List<String>,
        assessmentDay: Int
    ) {
        try {
            Log.d(TAG, "Starting AlarmManager scheduling for notification $notificationId, delay: ${delayMinutes} minutes")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null!")
                throw RuntimeException("AlarmManager is null!")
            }
            
            // Check if we can schedule exact alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    throw RuntimeException("Cannot schedule exact alarms - permission not granted")
                }
                Log.d(TAG, "Can schedule exact alarms: true")
            }
            
            // Create intent for the notification receiver with all necessary data
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notificationId", notificationId)
                putExtra("sessionId", sessionId)
                putExtra("muscleGroups", muscleGroups.toTypedArray())
                putExtra("assessmentDay", assessmentDay)
                putExtra("title", "Soreness Assessment")
                putExtra("text", getNotificationMessage(muscleGroups, assessmentDay))
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Calculate trigger time
            val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)
            Log.d(TAG, "Trigger time: $triggerTime (${delayMinutes} minutes from now)")
            
            // Schedule the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Used setExactAndAllowWhileIdle for Android M+")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Used setExact for older Android")
            }
            
            Log.d(TAG, "Successfully scheduled notification $notificationId for ${delayMinutes} minutes from now")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling delayed notification with AlarmManager: ${e.message}", e)
            Log.e(TAG, "NOT falling back to immediate notification - scheduling failed")
            // Do NOT fall back to immediate notification - this should be scheduled
            throw e // Re-throw to make the error visible
        }
    }
}
