package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ArchivedPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivedPeriodDao {
    @Query("SELECT * FROM archived_periods ORDER BY archivedAt DESC")
    fun getAllArchivedPeriods(): Flow<List<ArchivedPeriodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedPeriod(period: ArchivedPeriodEntity): Long

    @Delete
    suspend fun deleteArchivedPeriod(period: ArchivedPeriodEntity)

    @Query("DELETE FROM archived_periods")
    suspend fun deleteAll()
}
