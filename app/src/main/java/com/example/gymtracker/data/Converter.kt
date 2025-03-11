package com.example.gymtracker.data

import androidx.room.TypeConverter

class Converter {
    private val delimiter = "," // Choose a delimiter that won't appear in your data

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(delimiter) // Convert List<String> to a single String
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return value.split(delimiter) // Convert String back to List<String>
    }
    @TypeConverter
    fun fromIntList(value: List<Int?>): String {
        return value.joinToString(",") // Convert List<Int?> to a comma-separated String
    }

    @TypeConverter
    fun toIntList(value: String): List<Int?> {
        return value.split(",").map { it.toIntOrNull() } // Convert String back to List<Int?>
    }
}