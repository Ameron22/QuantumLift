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
}