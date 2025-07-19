package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val parametersId: Long, // Foreign key to PhysicalParameters
    val measurementType: String, // e.g., "chest", "waist", "biceps", "thighs", "calves", "neck", "shoulders"
    val value: Float, // in cm
    val unit: String = "cm"
) 