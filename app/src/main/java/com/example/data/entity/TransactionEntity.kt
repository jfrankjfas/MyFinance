package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double, // Base amount in primary currency (Córdobas NIO)
    val category: String,
    val type: String, // "EXPENSE" or "INCOME"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val isAiCategorized: Boolean = false,
    val attachmentUri: String? = null,
    val dueDate: Long? = null,
    val hasReminderScheduled: Boolean = false,
    val originalAmount: Double = 0.0,
    val originalCurrency: String = "NIO",
    val exchangeRate: Double = 1.0
)
