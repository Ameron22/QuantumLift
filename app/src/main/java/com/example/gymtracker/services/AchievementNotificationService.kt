package com.example.gymtracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gymtracker.MainActivity
import com.example.gymtracker.R
import com.example.gymtracker.data.AchievementManager

class AchievementNotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "achievement_channel"
        private const val CHANNEL_NAME = "Achievement Notifications"
        private const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for unlocked achievements"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                setSound(null, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("AchievementNotificationService", "Achievement notification channel created")
        }
    }
    
    fun showAchievementNotification(achievementId: String) {
        try {
            val achievementManager = AchievementManager.getInstance()
            val achievement = achievementManager.achievements.value.find { it.id == achievementId }
            
            if (achievement != null) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_achievements", true)
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    achievementId.hashCode(), 
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("🏆 Achievement Unlocked!")
                    .setContentText(achievement.title)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("${achievement.title}\n${achievement.description}"))
                    .setSmallIcon(R.drawable.trophy)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(achievementId.hashCode(), notification)
                
                Log.d("AchievementNotificationService", "Achievement notification shown for: ${achievement.title}")
            }
        } catch (e: Exception) {
            Log.e("AchievementNotificationService", "Error showing achievement notification: ${e.message}")
        }
    }
    
    fun showMultipleAchievementsNotification(achievementIds: Set<String>) {
        try {
            val achievementManager = AchievementManager.getInstance()
            val achievements = achievementManager.achievements.value.filter { it.id in achievementIds }
            
            if (achievements.isNotEmpty()) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_achievements", true)
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    NOTIFICATION_ID, 
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val achievementTitles = achievements.joinToString(", ") { it.title }
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("🏆 ${achievements.size} Achievement${if (achievements.size > 1) "s" else ""} Unlocked!")
                    .setContentText(achievementTitles)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("${achievements.size} new achievement${if (achievements.size > 1) "s" else ""} unlocked!\n$achievementTitles"))
                    .setSmallIcon(R.drawable.trophy)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
                
                Log.d("AchievementNotificationService", "Multiple achievements notification shown for ${achievements.size} achievements")
            }
        } catch (e: Exception) {
            Log.e("AchievementNotificationService", "Error showing multiple achievements notification: ${e.message}")
        }
    }
} 