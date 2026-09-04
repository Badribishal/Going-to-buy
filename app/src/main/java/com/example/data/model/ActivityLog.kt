package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityType(val label: String) {
    PRODUCT_ADDED("Added Product"),
    MONEY_ADDED("Added Money"),
    MONEY_ALLOCATED("Allocated Money"),
    PRODUCT_FUNDED("Product Fully Funded"),
    PRODUCT_PURCHASED("Marked Purchased"),
    PRODUCT_REMOVED("Product Removed")
}

@Immutable
@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val description: String,
    val amount: Double? = null,
    val productId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
