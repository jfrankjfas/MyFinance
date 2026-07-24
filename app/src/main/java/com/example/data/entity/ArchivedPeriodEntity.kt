package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_periods")
data class ArchivedPeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val periodMode: String,
    val archivedAt: Long,
    val budgetLimit: Double,
    val totalSpent: Double,
    val totalScheduled: Double,
    val currencySymbol: String,
    val note: String = "",
    val transactionsJson: String,
    val scheduledJson: String,
    val isClosed: Boolean = false
)
