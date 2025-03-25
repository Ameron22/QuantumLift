package com.example.gymtracker.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AchievementManager private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val achievementDao = db.achievementDao()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _newlyUnlockedAchievements = MutableStateFlow<Set<String>>(emptySet())
    val newlyUnlockedAchievements: StateFlow<Set<String>> = _newlyUnlockedAchievements.asStateFlow()

    init {
        // Initialize achievements if needed
        scope.launch {
            initializeAchievements()
            
            // Start observing achievements from database
            achievementDao.getAllAchievements().collect { achievementEntities ->
                _achievements.value = achievementEntities.map { entity ->
                    // Convert AchievementEntity to Achievement
                    Achievement(
                        id = entity.id,
                        title = getAchievementTitle(entity.id),
                        description = getAchievementDescription(entity.id),
                        iconResId = getAchievementIcon(entity.id, entity.status),
                        status = entity.status,
                        currentProgress = entity.currentProgress,
                        maxProgress = getMaxProgress(entity.id),
                        category = getAchievementCategory(entity.id)
                    )
                }
            }
        }
    }

    private suspend fun initializeAchievements() {
        withContext(Dispatchers.IO) {
            // List of all achievement IDs with their metadata
            val allAchievements = listOf(
                "first_workout",
                "workout_warrior",
                "workout_master",
                "bench_press_100",
                "consistency_week",
                "consistency_month",
                "night_owl"
            )

            // Initialize each achievement if it doesn't exist
            allAchievements.forEach { id ->
                if (achievementDao.getAchievement(id) == null) {
                    achievementDao.insertOrUpdateAchievement(
                        AchievementEntity(
                            id = id,
                            status = AchievementStatus.LOCKED,
                            currentProgress = 0,
                            maxValue = when (id) {
                                "bench_press_100" -> 0f
                                else -> 0f
                            },
                            targetValue = when (id) {
                                "bench_press_100" -> 100f
                                else -> when (id) {
                                    "workout_warrior" -> 10f
                                    "workout_master" -> 50f
                                    "consistency_week" -> 7f
                                    "consistency_month" -> 30f
                                    else -> 0f
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private fun getAchievementTitle(id: String): String = when (id) {
        "first_workout" -> "Quantum Awakening 🚀🏋️‍♂️"
        "workout_warrior" -> "Workout Warrior 💪"
        "workout_master" -> "Workout Master 🌟"
        "bench_press_100" -> "Centurion 💯"
        "consistency_week" -> "Week Warrior 📅"
        "consistency_month" -> "Monthly Master 🗓️"
        "night_owl" -> "Night Owl 🦉"
        else -> "Unknown Achievement"
    }

    private fun getAchievementDescription(id: String): String = when (id) {
        "first_workout" -> "You've taken the first step towards infinite gains. The quantum journey begins!"
        "workout_warrior" -> "Complete 10 workouts. You're building a strong foundation!"
        "workout_master" -> "Complete 50 workouts. You're a true fitness enthusiast!"
        "bench_press_100" -> "Bench press 100kg. Welcome to the strength elite!"
        "consistency_week" -> "Work out consistently for 7 days. Building healthy habits!"
        "consistency_month" -> "Maintain a 30-day workout streak. Now that's dedication!"
        "night_owl" -> "Complete a workout after 10 PM. The grind never stops!"
        else -> "Unknown Achievement"
    }

    private fun getMaxProgress(id: String): Int = when (id) {
        "workout_warrior" -> 10
        "workout_master" -> 50
        "bench_press_100" -> 100
        "consistency_week" -> 7
        "consistency_month" -> 30
        else -> 0
    }

    private fun getAchievementCategory(id: String): AchievementCategory = when (id) {
        "first_workout", "workout_warrior", "workout_master" -> AchievementCategory.WORKOUT_MILESTONES
        "bench_press_100" -> AchievementCategory.STRENGTH_GOALS
        "consistency_week", "consistency_month" -> AchievementCategory.CONSISTENCY_AWARDS
        "night_owl" -> AchievementCategory.SPECIAL_CHALLENGES
        else -> AchievementCategory.WORKOUT_MILESTONES
    }

    private fun getAchievementIcon(id: String, status: AchievementStatus): Int = when {
        status != AchievementStatus.UNLOCKED -> R.drawable.trophy  // Show trophy for locked/in-progress
        else -> when (id) {  // Show specific icon only for unlocked achievements
            "first_workout" -> R.drawable.achievement_first_workout
            "workout_warrior" -> R.drawable.achievement_workout_warrior_10
            "workout_master" -> R.drawable.achievement_50_warrior
            "bench_press_100" -> R.drawable.achievement_centurion_100
            "consistency_week" -> R.drawable.achievement_week_wariour
            "consistency_month" -> R.drawable.achievement_monthly_master
            "night_owl" -> R.drawable.achievement_night_owl
            else -> R.drawable.trophy
        }
    }

    suspend fun updateWorkoutCount(count: Int) {
        withContext(Dispatchers.IO) {
            // First Workout Achievement
            if (count >= 1) {
                val firstWorkout = achievementDao.getAchievement("first_workout")
                if (firstWorkout?.status != AchievementStatus.UNLOCKED) {
                    achievementDao.insertOrUpdateAchievement(
                        AchievementEntity(
                            id = "first_workout",
                            status = AchievementStatus.UNLOCKED,
                            currentProgress = 1
                        )
                    )
                    _newlyUnlockedAchievements.value += "first_workout"
                }
            }

            // Workout Warrior Achievement (10 workouts)
            updateProgressAchievement(
                id = "workout_warrior",
                currentValue = count,
                targetValue = 10
            )

            // Workout Master Achievement (50 workouts)
            updateProgressAchievement(
                id = "workout_master",
                currentValue = count,
                targetValue = 50
            )
        }
    }

    private suspend fun updateProgressAchievement(
        id: String,
        currentValue: Int,
        targetValue: Int
    ) {
        val achievement = achievementDao.getAchievement(id)
        val newStatus = when {
            currentValue >= targetValue -> AchievementStatus.UNLOCKED
            currentValue > 0 -> AchievementStatus.IN_PROGRESS
            else -> AchievementStatus.LOCKED
        }

        if (achievement?.status != newStatus || achievement.currentProgress != currentValue) {
            achievementDao.insertOrUpdateAchievement(
                AchievementEntity(
                    id = id,
                    status = newStatus,
                    currentProgress = currentValue
                )
            )
            if (newStatus == AchievementStatus.UNLOCKED && achievement?.status != AchievementStatus.UNLOCKED) {
                _newlyUnlockedAchievements.value += id
            }
        }
    }

    suspend fun updateConsistencyStreak(streakDays: Int) {
        withContext(Dispatchers.IO) {
            Log.d("AchievementManager", "Updating consistency streak: $streakDays days")
            
            // Weekly Warrior Achievement (7 days)
            updateProgressAchievement(
                id = "consistency_week",
                currentValue = minOf(streakDays, 7),
                targetValue = 7
            )

            // Monthly Master Achievement (30 days)
            updateProgressAchievement(
                id = "consistency_month",
                currentValue = minOf(streakDays, 30),
                targetValue = 30
            )

            // Also update the streak dates
            achievementDao.updateWorkoutStreak(System.currentTimeMillis())
        }
    }

    suspend fun updateStrengthProgress(exerciseName: String, weight: Float) {
        withContext(Dispatchers.IO) {
            Log.d("AchievementManager", "Updating strength progress for $exerciseName with weight: $weight")
            if (exerciseName.equals("Bench Press", ignoreCase = true) || exerciseName == "bench_press_100") {
                val wasUnlocked = achievementDao.updateBenchPressProgress(weight)
                Log.d("AchievementManager", "Updated bench press progress with weight: $weight")
                if (wasUnlocked) {
                    _newlyUnlockedAchievements.value += "bench_press_100"
                    Log.d("AchievementManager", "Bench press achievement unlocked!")
                }
            }
        }
    }

    suspend fun updateSpecialChallenges(challengeId: String) {
        withContext(Dispatchers.IO) {
            val achievement = achievementDao.getAchievement(challengeId)
            if (achievement?.status != AchievementStatus.UNLOCKED) {
                achievementDao.insertOrUpdateAchievement(
                    AchievementEntity(
                        id = challengeId,
                        status = AchievementStatus.UNLOCKED,
                        currentProgress = 1
                    )
                )
                _newlyUnlockedAchievements.value += challengeId
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AchievementManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AchievementManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): AchievementManager {
            return instance ?: throw IllegalStateException(
                "AchievementManager must be initialized first"
            )
        }
    }
} 