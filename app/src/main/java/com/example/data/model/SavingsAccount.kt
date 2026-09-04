package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "savings_account")
data class SavingsAccountEntity(
    @PrimaryKey val id: Int = 1,
    val totalDeposited: Double = 0.0,
    val unallocatedBalance: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
