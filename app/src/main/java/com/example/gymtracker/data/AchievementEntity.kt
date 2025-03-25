package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "achievements")
@TypeConverters(Converter::class)
data class AchievementEntity(
    @PrimaryKey
    val id: String,  // Achievement identifier (e.g., "bench_press_100", "workout_streak")
    val status: AchievementStatus,
    val currentProgress: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val streakDates: List<Long> = emptyList(),  // For storing streak dates
    val maxValue: Float = 0f,  // For storing max weights or other numerical achievements
    val targetValue: Float = 0f,  // For storing target values (e.g., 100kg for bench press)
    val additionalData: String = ""  // For storing any additional JSON data if needed
) 