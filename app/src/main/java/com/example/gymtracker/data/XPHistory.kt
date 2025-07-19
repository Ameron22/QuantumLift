package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xp_history")
data class XPHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val xpEarned: Int,
    val source: String, // "workout_completion", "exercise_set", "personal_record", "streak_bonus", etc.
    val sourceId: String? = null, // ID of the workout/exercise that earned XP
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "" // Human-readable description of what earned the XP
) 