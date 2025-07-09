package com.example.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievement(achievementId: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE id = 'workout_streak'")
    suspend fun getWorkoutStreak(): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE id = 'bench_press_100'")
    suspend fun getBenchPressProgress(): AchievementEntity?

    @Transaction
    suspend fun updateWorkoutStreak(date: Long) {
        val streak = getWorkoutStreak() ?: AchievementEntity(
            id = "workout_streak",
            status = AchievementStatus.IN_PROGRESS,
            streakDates = emptyList()
        )

        val dates = streak.streakDates.toMutableList()
        dates.add(date)
        
        // Keep only last 7 dates
        while (dates.size > 7) {
            dates.removeAt(0)
        }

        // Sort dates in ascending order
        dates.sort()

        // Check if dates are consecutive
        var isConsecutive = true
        for (i in 1 until dates.size) {
            val daysDiff = (dates[i] - dates[i-1]) / (24 * 60 * 60 * 1000) // Convert to days
            if (daysDiff != 1L) {
                isConsecutive = false
                break
            }
        }

        val newStatus = when {
            dates.size >= 7 && isConsecutive -> AchievementStatus.UNLOCKED
            dates.isNotEmpty() -> AchievementStatus.IN_PROGRESS
            else -> AchievementStatus.LOCKED
        }

        insertOrUpdateAchievement(streak.copy(
            status = newStatus,
            streakDates = dates,
            currentProgress = if (isConsecutive) dates.size else 0
        ))
    }

    @Transaction
    suspend fun updateBenchPressProgress(weight: Float): Boolean {
        val benchPress = getBenchPressProgress() ?: AchievementEntity(
            id = "bench_press_100",
            status = AchievementStatus.LOCKED,
            maxValue = 0f,
            targetValue = 100f,
            currentProgress = 0
        )

        // Only update if new weight is higher
        if (weight > benchPress.maxValue) {
            val newStatus = when {
                weight >= 100f -> AchievementStatus.UNLOCKED
                weight > 0f -> AchievementStatus.IN_PROGRESS
                else -> AchievementStatus.LOCKED
            }

            val wasUnlocked = benchPress.status != AchievementStatus.UNLOCKED && newStatus == AchievementStatus.UNLOCKED

            insertOrUpdateAchievement(benchPress.copy(
                status = newStatus,
                maxValue = weight,
                currentProgress = weight.toInt(),
                targetValue = 100f  // Maintain target value when updating
            ))

            // Return whether the achievement was just unlocked
            return wasUnlocked
        }
        return false
    }

    @Query("DELETE FROM achievements")
    suspend fun clearAllAchievements()
} 