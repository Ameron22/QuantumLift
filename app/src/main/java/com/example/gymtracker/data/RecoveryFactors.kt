package com.example.gymtracker.data

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Data class to track various recovery factors that affect muscle soreness
 * @property sleepQuality Quality of sleep (1-10 scale)
 * @property proteinIntake Protein intake in grams
 * @property hydration Hydration level (1-10 scale)
 * @property stressLevel Stress level (1-10 scale)
 */
data class RecoveryFactors(
    val sleepQuality: Int,
    val proteinIntake: Int,
    val hydration: Int,
    val stressLevel: Int
)

/**
 * Type converters for Room database to handle RecoveryFactors serialization
 */
class RecoveryFactorsConverters {
    @TypeConverter
    fun fromRecoveryFactors(value: RecoveryFactors): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toRecoveryFactors(value: String): RecoveryFactors {
        val type = object : TypeToken<RecoveryFactors>() {}.type
        return Gson().fromJson(value, type)
    }
} 