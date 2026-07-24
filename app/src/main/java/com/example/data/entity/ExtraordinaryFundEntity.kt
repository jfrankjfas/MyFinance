package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extraordinary_funds")
data class ExtraordinaryFundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val currencySymbol: String = "$",
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val allocationsJson: String = "[]"
)
