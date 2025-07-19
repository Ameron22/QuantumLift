package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "physical_parameters")
@TypeConverters(Converter::class)
data class PhysicalParameters(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val userId: String, // Link to user
    val date: Long, // Timestamp
    val weight: Float?, // in kg
    val height: Float?, // in cm
    val bmi: Float?, // calculated
    val bodyFatPercentage: Float?, // optional
    val muscleMass: Float?, // optional
    val notes: String = ""
) 