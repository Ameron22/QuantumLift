package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_xp")
data class UserXP(
    @PrimaryKey val userId: String,
    val totalXP: Int = 0,
    val currentLevel: Int = 1,
    val xpToNextLevel: Int = 100,
    val lastUpdated: Long = System.currentTimeMillis()
) 