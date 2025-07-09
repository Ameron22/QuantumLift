package com.example.gymtracker.data

enum class AchievementCategory {
    WORKOUT_MILESTONES,
    STRENGTH_GOALS,
    CONSISTENCY_AWARDS,
    SPECIAL_CHALLENGES
}

enum class AchievementStatus {
    LOCKED,
    IN_PROGRESS,
    UNLOCKED
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val iconResId: Int,
    val currentProgress: Int = 0,
    val maxProgress: Int = 1,
    val status: AchievementStatus = AchievementStatus.LOCKED
) 