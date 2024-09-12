package com.akheparasu.contextmonitor.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "health_data")
data class DataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val heartRate: Float,
    val respiratoryRate: Float,
    val symptoms: Map<String, Int>,
    val recordedOn: Date
)
