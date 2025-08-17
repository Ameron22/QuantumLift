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
        const val XP_LEVEL_11_25 = 200 // 1000-4000 XP for levels 11-25
        const val XP_LEVEL_26_50 = 300 // 4000-11500 XP for levels 26-50
        const val XP_LEVEL_51_75 = 400 // 11500-21500 XP for levels 51-75
        const val XP_LEVEL_76_100 = 500 // 21500+ XP for levels 76-100
    }
    
    /**
     * Calculate XP for workout completion based on duration and sets
     */
    fun calculateWorkoutXP(durationMinutes: Int, totalSets: Int): Int {
        val baseXP = XP_WORKOUT_BASE
        val durationXP = durationMinutes * XP_WORKOUT_PER_MINUTE
        val setsXP = totalSets * XP_PER_SET
        
        val totalXP = baseXP + durationXP + setsXP
        Log.d("XPSystem", "=== WORKOUT XP CALCULATION ===")
        Log.d("XPSystem", "Duration minutes: $durationMinutes")
        Log.d("XPSystem", "Total sets: $totalSets")
        Log.d("XPSystem", "Base XP: $baseXP")
        Log.d("XPSystem", "Duration XP: $durationXP")
        Log.d("XPSystem", "Sets XP: $setsXP")
        Log.d("XPSystem", "Total XP: $totalXP")
        Log.d("XPSystem", "=== END WORKOUT XP ===")
        
        return totalXP
    }
    
    /**
     * Calculate level from total XP
     */
    fun calculateLevel(totalXP: Int): Int {
        val level = when {
            totalXP < 1000 -> (totalXP / XP_LEVEL_1_10) + 1
            totalXP < 4000 -> 10 + ((totalXP - 1000) / XP_LEVEL_11_25) + 1
            totalXP < 11500 -> 25 + ((totalXP - 4000) / XP_LEVEL_26_50) + 1
            totalXP < 21500 -> 50 + ((totalXP - 11500) / XP_LEVEL_51_75) + 1
            else -> 75 + ((totalXP - 21500) / XP_LEVEL_76_100) + 1
        }.coerceAtMost(100) // Cap at level 100
        
        Log.d("XPSystem", "Level calculation: totalXP=$totalXP, calculatedLevel=$level")
        
        return level
    }
    
    /**
     * Calculate XP needed for next level
     */
    fun calculateXPToNextLevel(currentLevel: Int, totalXP: Int): Int {
        // If at max level, return 0 (no more levels to gain)
        if (currentLevel >= 100) {
            return 0
        }
        
        val xpForCurrentLevel = when {
            currentLevel <= 10 -> (currentLevel - 1) * XP_LEVEL_1_10
            currentLevel <= 25 -> 1000 + (currentLevel - 11) * XP_LEVEL_11_25
            currentLevel <= 50 -> 4000 + (currentLevel - 26) * XP_LEVEL_26_50
            currentLevel <= 75 -> 11500 + (currentLevel - 51) * XP_LEVEL_51_75
            else -> 21500 + (currentLevel - 76) * XP_LEVEL_76_100
        }
        
        val xpForNextLevel = when {
            currentLevel <= 10 -> currentLevel * XP_LEVEL_1_10
            currentLevel <= 25 -> 1000 + (currentLevel - 10) * XP_LEVEL_11_25
            currentLevel <= 50 -> 4000 + (currentLevel - 25) * XP_LEVEL_26_50
            currentLevel <= 75 -> 11500 + (currentLevel - 50) * XP_LEVEL_51_75
            else -> 21500 + (currentLevel - 75) * XP_LEVEL_76_100
        }
        
        val xpNeeded = xpForNextLevel - totalXP
        Log.d("XPSystem", "XP calculation: level=$currentLevel, totalXP=$totalXP, xpForNext=$xpForNextLevel, xpNeeded=$xpNeeded")
        
        return xpNeeded
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
     * Calculate XP needed to start a level
     */
    fun getLevelStartXP(level: Int): Int {
        return when {
            level <= 10 -> (level - 1) * 100
            level <= 25 -> 1000 + (level - 11) * 200
            level <= 50 -> 4000 + (level - 26) * 300
            level <= 75 -> 11500 + (level - 51) * 400
            else -> 21500 + (level - 76) * 500
        }
    }
    
    /**
     * Calculate XP needed to complete a level
     */
    fun getLevelEndXP(level: Int): Int {
        return when {
            level <= 10 -> level * 100
            level <= 25 -> 1000 + (level - 10) * 200
            level <= 50 -> 4000 + (level - 25) * 300
            level <= 75 -> 11500 + (level - 50) * 400
            else -> 21500 + (level - 75) * 500
        }
    }
    
    /**
     * Calculate XP within a level (how much XP you have in the current level)
     */
    fun getXPWithinLevel(totalXP: Int, level: Int): Int {
        val levelStartXP = getLevelStartXP(level)
        return totalXP - levelStartXP
    }
    
    /**
     * Calculate XP needed to complete the current level
     */
    fun getXPNeededForLevel(level: Int): Int {
        val levelStartXP = getLevelStartXP(level)
        val levelEndXP = getLevelEndXP(level)
        return levelEndXP - levelStartXP
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