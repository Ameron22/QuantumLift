package com.example.gymtracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gymtracker.R

/**
 * Broadcast receiver to handle delayed soreness assessment notifications
 */
class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationReceiver"
        private const val CHANNEL_ID = "soreness_assessment_channel"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Received notification broadcast - creating notification for soreness assessment")
            
            val notificationId = intent.getIntExtra("notificationId", -1)
            val sessionId = intent.getLongExtra("sessionId", -1L)
            val muscleGroups = intent.getStringArrayExtra("muscleGroups")?.toList() ?: emptyList()
            val assessmentDay = intent.getIntExtra("assessmentDay", 1)
            val title = intent.getStringExtra("title") ?: "Soreness Assessment"
            val text = intent.getStringExtra("text") ?: "How are you feeling after your workout?"
            
            // Create notification channel if needed
            createNotificationChannel(context)
            
            // Create the notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
            // Add click action to open the soreness assessment screen
            val assessmentIntent = Intent(context, com.example.gymtracker.SorenessAssessmentActivity::class.java).apply {
                putExtra("sessionId", sessionId)
                putExtra("muscleGroups", muscleGroups.toTypedArray())
                putExtra("assessmentDay", assessmentDay)
                putExtra("fromNotification", true) // Flag to indicate this came from notification
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            // Use TaskStackBuilder to properly handle navigation when app is already running
            val taskStackBuilder = TaskStackBuilder.create(context).apply {
                addNextIntentWithParentStack(assessmentIntent)
            }
            
            val pendingIntent = taskStackBuilder.getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notification.setContentIntent(pendingIntent)
            
            // Send the notification
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification.build())
            
            Log.d(TAG, "Sent delayed notification $notificationId for session $sessionId, day $assessmentDay")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification broadcast", e)
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Soreness Assessment",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for soreness assessment"
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
