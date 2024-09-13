package com.akheparasu.contextmonitor.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "health_data")
data class DataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "heartRate") val heartRate: Float,
    @ColumnInfo(name = "respiratoryRate") val respiratoryRate: Float,
    @ColumnInfo(name = "symptoms") val symptoms: Map<String, Int>,
    @ColumnInfo(name = "recordedOn") val recordedOn: Date
)
