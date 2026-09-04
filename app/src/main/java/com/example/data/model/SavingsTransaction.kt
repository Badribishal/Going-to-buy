package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType(val label: String) {
    DEPOSIT("Added Money"),
    ALLOCATE("Allocated to Product"),
    DEALLOCATE("Returned to Savings"),
    WITHDRAW("Withdrawn")
}

@Immutable
@Entity(tableName = "savings_transactions")
data class SavingsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val productId: Long? = null,
    val productName: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
