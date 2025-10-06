package com.example.gymtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warm_up_templates")
data class WarmUpTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // e.g., "General Full Body", "Upper Body Focus", "Quick Cardio"
    val description: String = "", // Description of the warm-up routine
    val category: String = "General", // General, Muscle-Specific, Equipment-Based, Intensity-Based
    val targetMuscleGroups: String = "", // JSON string of target muscle groups
    val difficulty: String = "Beginner", // Beginner, Intermediate, Advanced
    val estimatedDuration: Int = 10, // Duration in minutes
    val isDefault: Boolean = false, // Whether this is a system-provided template
    val createdBy: String = "system", // "system" for default templates, user ID for custom ones
    val createdAt: Long = System.currentTimeMillis()
)

