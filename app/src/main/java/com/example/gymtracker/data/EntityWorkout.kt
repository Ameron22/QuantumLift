package com.example.gymtracker.data
import androidx.room.*

@Entity
data class EntityWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)