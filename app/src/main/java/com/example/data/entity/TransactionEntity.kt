package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String, // "EXPENSE" or "INCOME"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val isAiCategorized: Boolean = false
)
