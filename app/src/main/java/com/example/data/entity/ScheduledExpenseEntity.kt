package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_expenses")
data class ScheduledExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dueDate: Long, // timestamp in milliseconds
    val isPaid: Boolean = false,
    val notifyReminder: Boolean = true,
    val attachmentUri: String? = null,
    val note: String = "",
    val isEmergencyPriority: Boolean = false, // Gasto vital / prioritario
    val isRecurringMonthly: Boolean = false, // Recurrente cada mes
    val currency: String = "C$"
)
