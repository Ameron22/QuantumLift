package com.example.gymtracker.data

data class UserSettings(
    val defaultWorkTime: Int = 30, // seconds
    val defaultBreakTime: Int = 60, // seconds
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundVolume: Float = 0.5f // Volume level from 0.0f to 1.0f
) 