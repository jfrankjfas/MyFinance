package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ScheduledExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledExpenseDao {
    @Query("SELECT * FROM scheduled_expenses ORDER BY dueDate ASC")
    fun getAllScheduledExpenses(): Flow<List<ScheduledExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledExpense(expense: ScheduledExpenseEntity): Long

    @Update
    suspend fun updateScheduledExpense(expense: ScheduledExpenseEntity)

    @Delete
    suspend fun deleteScheduledExpense(expense: ScheduledExpenseEntity)

    @Query("DELETE FROM scheduled_expenses")
    suspend fun deleteAll()
}
