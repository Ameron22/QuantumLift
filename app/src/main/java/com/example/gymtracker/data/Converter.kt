package com.example.gymtracker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converter {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromIntListToString(value: String?): List<Int?> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<Int?>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromIntList(list: List<Int?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromRecoveryFactors(value: RecoveryFactors?): String {
        return if (value == null) {
            ""
        } else {
            Gson().toJson(value)
        }
    }

    @TypeConverter
    fun toRecoveryFactors(value: String?): RecoveryFactors? {
        return if (value.isNullOrEmpty()) {
            null
        } else {
            val type = object : TypeToken<RecoveryFactors>() {}.type
            Gson().fromJson(value, type)
        }
    }
}