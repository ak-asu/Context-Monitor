package com.akheparasu.contextmonitor.storage

import androidx.room.TypeConverter
import java.util.Date

class DataConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
