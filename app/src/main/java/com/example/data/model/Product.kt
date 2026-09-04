package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProductPriority(val label: String, val level: Int) {
    HIGH("High Priority", 1),
    MEDIUM("Medium", 2),
    LOW("Low", 3);

    companion object {
        fun fromString(value: String): ProductPriority =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

enum class ProductStatus(val label: String) {
    SAVING("Saving"),
    READY_TO_BUY("Ready to buy"),
    PURCHASED("Purchased");

    companion object {
        fun fromString(value: String): ProductStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SAVING
    }
}

@Immutable
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetPrice: Double,
    val savedAmount: Double = 0.0,
    val imageUrl: String? = null,
    val originalUrl: String? = null,
    val storeName: String = "Store",
    val category: String = "Tech & Gadgets",
    val priority: String = ProductPriority.MEDIUM.name,
    val notes: String = "",
    val targetDate: Long? = null,
    val status: String = ProductStatus.SAVING.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = maxOf(0.0, targetPrice - savedAmount)

    val progress: Float
        get() = if (targetPrice <= 0) 0f else (savedAmount / targetPrice).toFloat().coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isFunded: Boolean
        get() = savedAmount >= targetPrice && targetPrice > 0

    val currentStatus: ProductStatus
        get() = when {
            status == ProductStatus.PURCHASED.name -> ProductStatus.PURCHASED
            isFunded -> ProductStatus.READY_TO_BUY
            else -> ProductStatus.SAVING
        }
}
