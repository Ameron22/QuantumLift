package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gymtracker.data.Converter

/**
 * Entity class to store soreness assessments from users
 * This data will be used to train ML models for soreness prediction
 */
@Entity(tableName = "soreness_assessments")
@TypeConverters(Converter::class)
data class SorenessAssessment(
    @PrimaryKey(autoGenerate = true) val assessmentId: Long = 0,
    val sessionId: Long, // Links to workout session
    val exerciseId: Int, // Which exercise was performed
    val muscleGroups: List<String>, // ["shoulders", "triceps"]
    val muscleParts: List<String>, // ["anterior_deltoid", "lateral_tricep"]
    
    // Soreness ratings (simplified for testing)
    val soreness24hr: Map<String, Int>, // muscleGroup -> soreness level (1-10)
    val soreness48hr: Map<String, Int>?, // Optional 48hr follow-up
    val overallSoreness: Int, // Overall body soreness (1-10)
    
    // Context
    val assessmentDay: Int, // 1 = 24hrs, 2 = 48hrs
    val timestamp: Long,
    val notes: String? = null,
    val wasActive: Boolean = true, // Was user active between workout and assessment
    val sleepQuality: Int? = null, // 1-10 sleep quality (optional)
    val stressLevel: Int? = null // 1-10 stress level (optional)
)
