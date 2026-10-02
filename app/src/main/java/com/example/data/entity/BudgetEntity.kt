package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // "GENERAL" for total budget or specific category name
    val limitAmount: Double,
    val alertThresholdPercent: Int = 80, // Default alert when 80% reached
    val currency: String = "C$"
)
