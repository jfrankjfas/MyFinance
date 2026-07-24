package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ExtraordinaryFundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraordinaryFundDao {
    @Query("SELECT * FROM extraordinary_funds ORDER BY createdAt DESC")
    fun getAllFunds(): Flow<List<ExtraordinaryFundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFund(fund: ExtraordinaryFundEntity): Long

    @Delete
    suspend fun deleteFund(fund: ExtraordinaryFundEntity)

    @Query("DELETE FROM extraordinary_funds")
    suspend fun deleteAll()
}
