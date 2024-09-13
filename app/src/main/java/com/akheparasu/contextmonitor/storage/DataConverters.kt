package com.akheparasu.contextmonitor.storage

import androidx.room.TypeConverter
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MapStringIntConverter {
    @TypeConverter
    fun fromMap(value: Map<String, Int>?): String {
        return if (value == null) {
            ""
        } else {
            Gson().toJson(value)
        }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Int> {
        return if (value.isNullOrEmpty()) {
            emptyMap()
        } else {
            val mapType = object : TypeToken<Map<String, Int>>() {}.type
            Gson().fromJson(value, mapType)
        }
    }
}

class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
