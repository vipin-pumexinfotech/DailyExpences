package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorHex: String,
    val timestamp: Long,
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringEndDate: Long? = null,
    val recurringFrequency: String = "Monthly",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

