package com.akheparasu.contextmonitor.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataDao {
    @Insert
    suspend fun insertRow(dataEntity: DataEntity)

    @Query("SELECT * FROM health_datatable")
    fun getAllRows(): Flow<List<DataEntity>>
}
