package com.example.gymtracker.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(
        UserSettings(
            defaultWorkTime = prefs.getInt("default_work_time", 30),
            defaultBreakTime = prefs.getInt("default_break_time", 60),
            defaultPreSetBreakTime = prefs.getInt("default_pre_set_break_time", 10),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            soundVolume = prefs.getFloat("sound_volume", 0.5f),
            loadFromHistory = prefs.getBoolean("load_from_history", true),
            notificationPermissionRequested = prefs.getBoolean("notification_permission_requested", false)
        )
    )
    val settingsFlow: Flow<UserSettings> = _settingsFlow.asStateFlow()

    fun updateWorkTime(seconds: Int) {
        Log.d("UserSettingsPreferences", "Saving work time: $seconds seconds")
        prefs.edit().putInt("default_work_time", seconds).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultWorkTime = seconds)
        Log.d("UserSettingsPreferences", "Work time saved successfully")
    }

    fun updateBreakTime(seconds: Int) {
        Log.d("UserSettingsPreferences", "Saving break time: $seconds seconds")
        prefs.edit().putInt("default_break_time", seconds).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultBreakTime = seconds)
        Log.d("UserSettingsPreferences", "Break time saved successfully")
    }
    
    fun updatePreSetBreakTime(seconds: Int) {
        Log.d("UserSettingsPreferences", "Saving pre-set break time: $seconds seconds")
        prefs.edit().putInt("default_pre_set_break_time", seconds).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultPreSetBreakTime = seconds)
        Log.d("UserSettingsPreferences", "Pre-set break time saved successfully")
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        Log.d("UserSettingsPreferences", "Saving sound enabled: $enabled")
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(soundEnabled = enabled)
        Log.d("UserSettingsPreferences", "Sound setting saved successfully")
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        Log.d("UserSettingsPreferences", "Saving vibration enabled: $enabled")
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(vibrationEnabled = enabled)
        Log.d("UserSettingsPreferences", "Vibration setting saved successfully")
    }
    
    fun updateSoundVolume(volume: Float) {
        Log.d("UserSettingsPreferences", "Saving sound volume: $volume")
        prefs.edit().putFloat("sound_volume", volume).apply()
        _settingsFlow.value = _settingsFlow.value.copy(soundVolume = volume)
        Log.d("UserSettingsPreferences", "Sound volume saved successfully")
    }
    
    fun updateLoadFromHistory(enabled: Boolean) {
        Log.d("UserSettingsPreferences", "Saving load from history: $enabled")
        prefs.edit().putBoolean("load_from_history", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(loadFromHistory = enabled)
        Log.d("UserSettingsPreferences", "Load from history setting saved successfully")
    }
    
    fun updateNotificationPermissionRequested(requested: Boolean) {
        Log.d("UserSettingsPreferences", "Saving notification permission requested: $requested")
        prefs.edit().putBoolean("notification_permission_requested", requested).apply()
        _settingsFlow.value = _settingsFlow.value.copy(notificationPermissionRequested = requested)
        Log.d("UserSettingsPreferences", "Notification permission requested setting saved successfully")
    }
    
    fun getCurrentSettings(): UserSettings {
        val workTime = prefs.getInt("default_work_time", 30)
        val breakTime = prefs.getInt("default_break_time", 60)
        val preSetBreakTime = prefs.getInt("default_pre_set_break_time", 10)
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundVolume = prefs.getFloat("sound_volume", 0.5f)
        val loadFromHistory = prefs.getBoolean("load_from_history", true)
        val notificationPermissionRequested = prefs.getBoolean("notification_permission_requested", false)
        Log.d("UserSettingsPreferences", "Current settings - Work: $workTime, Break: $breakTime, Pre-Set: $preSetBreakTime, Sound: $soundEnabled, Vibration: $vibrationEnabled, Volume: $soundVolume, LoadFromHistory: $loadFromHistory, NotificationPermissionRequested: $notificationPermissionRequested")
        return UserSettings(
            defaultWorkTime = workTime, 
            defaultBreakTime = breakTime,
            defaultPreSetBreakTime = preSetBreakTime,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            soundVolume = soundVolume,
            loadFromHistory = loadFromHistory,
            notificationPermissionRequested = notificationPermissionRequested
        )
    }
} 