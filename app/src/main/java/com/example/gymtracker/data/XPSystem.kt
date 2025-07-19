package com.example.gymtracker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XPSystem(private val userXPDao: UserXPDao) {
    
    companion object {
        // XP constants
        const val XP_WORKOUT_BASE = 50
        const val XP_WORKOUT_PER_MINUTE = 2
        const val XP_PER_SET = 5
        const val XP_PERSONAL_RECORD = 25
        const val XP_STREAK_BONUS = 10
        
        // Level progression constants
        const val XP_LEVEL_1_10 = 100 // 0-1000 XP for levels 1-10
        const val XP_LEVEL_11_25 = 200 // 1000-5000 XP for levels 11-25
        const val XP_LEVEL_26_50 = 300 // 5000-15000 XP for levels 26-50
        const val XP_LEVEL_51_75 = 400 // 15000-30000 XP for levels 51-75
        const val XP_LEVEL_76_100 = 500 // 30000+ XP for levels 76-100
    }
    
    /**
     * Calculate XP for workout completion based on duration and sets
     */
    fun calculateWorkoutXP(durationMinutes: Int, totalSets: Int): Int {
        val baseXP = XP_WORKOUT_BASE
        val durationXP = durationMinutes * XP_WORKOUT_PER_MINUTE
        val setsXP = totalSets * XP_PER_SET
        
        val totalXP = baseXP + durationXP + setsXP
        Log.d("XPSystem", "Workout XP calculation: base=$baseXP, duration=$durationXP, sets=$setsXP, total=$totalXP")
        
        return totalXP
    }
    
    /**
     * Calculate level from total XP
     */
    fun calculateLevel(totalXP: Int): Int {
        return when {
            totalXP < 1000 -> (totalXP / XP_LEVEL_1_10) + 1
            totalXP < 5000 -> 10 + ((totalXP - 1000) / XP_LEVEL_11_25) + 1
            totalXP < 15000 -> 25 + ((totalXP - 5000) / XP_LEVEL_26_50) + 1
            totalXP < 30000 -> 50 + ((totalXP - 15000) / XP_LEVEL_51_75) + 1
            else -> 75 + ((totalXP - 30000) / XP_LEVEL_76_100) + 1
        }.coerceAtMost(100) // Cap at level 100
    }
    
    /**
     * Calculate XP needed for next level
     */
    fun calculateXPToNextLevel(currentLevel: Int, totalXP: Int): Int {
        val xpForCurrentLevel = when {
            currentLevel <= 10 -> (currentLevel - 1) * XP_LEVEL_1_10
            currentLevel <= 25 -> 1000 + (currentLevel - 11) * XP_LEVEL_11_25
            currentLevel <= 50 -> 5000 + (currentLevel - 26) * XP_LEVEL_26_50
            currentLevel <= 75 -> 15000 + (currentLevel - 51) * XP_LEVEL_51_75
            else -> 30000 + (currentLevel - 76) * XP_LEVEL_76_100
        }
        
        val xpForNextLevel = when {
            currentLevel < 10 -> currentLevel * XP_LEVEL_1_10
            currentLevel < 25 -> 1000 + (currentLevel - 10) * XP_LEVEL_11_25
            currentLevel < 50 -> 5000 + (currentLevel - 25) * XP_LEVEL_26_50
            currentLevel < 75 -> 15000 + (currentLevel - 50) * XP_LEVEL_51_75
            else -> 30000 + (currentLevel - 75) * XP_LEVEL_76_100
        }
        
        return xpForNextLevel - totalXP
    }
    
    /**
     * Award XP to user
     */
    suspend fun awardXP(
        userId: String,
        xpAmount: Int,
        source: String,
        sourceId: String? = null,
        description: String = ""
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Get current user XP
                var userXP = userXPDao.getUserXP(userId)
                
                if (userXP == null) {
                    // Create new user XP record
                    userXP = UserXP(userId = userId)
                }
                
                // Update XP
                val newTotalXP = userXP.totalXP + xpAmount
                val newLevel = calculateLevel(newTotalXP)
                val newXPToNextLevel = calculateXPToNextLevel(newLevel, newTotalXP)
                
                val updatedUserXP = userXP.copy(
                    totalXP = newTotalXP,
                    currentLevel = newLevel,
                    xpToNextLevel = newXPToNextLevel,
                    lastUpdated = System.currentTimeMillis()
                )
                
                // Save updated user XP
                userXPDao.insertUserXP(updatedUserXP)
                
                // Record XP history
                val xpHistory = XPHistory(
                    userId = userId,
                    xpEarned = xpAmount,
                    source = source,
                    sourceId = sourceId,
                    description = description
                )
                userXPDao.insertXPHistory(xpHistory)
                
                Log.d("XPSystem", "Awarded $xpAmount XP to user $userId. New total: $newTotalXP, Level: $newLevel")
                true
            } catch (e: Exception) {
                Log.e("XPSystem", "Error awarding XP: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get user XP data
     */
    suspend fun getUserXP(userId: String): UserXP? {
        return withContext(Dispatchers.IO) {
            try {
                userXPDao.getUserXP(userId)
            } catch (e: Exception) {
                Log.e("XPSystem", "Error getting user XP: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Get recent XP history
     */
    suspend fun getRecentXPHistory(userId: String, limit: Int = 10): List<XPHistory> {
        return withContext(Dispatchers.IO) {
            try {
                userXPDao.getRecentXPHistory(userId, limit)
            } catch (e: Exception) {
                Log.e("XPSystem", "Error getting XP history: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Get level title based on level
     */
    fun getLevelTitle(level: Int): String {
        return when {
            level <= 10 -> "Beginner"
            level <= 25 -> "Intermediate"
            level <= 50 -> "Advanced"
            level <= 75 -> "Expert"
            else -> "Master"
        }
    }
} 