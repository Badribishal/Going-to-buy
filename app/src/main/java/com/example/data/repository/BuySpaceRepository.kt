package com.example.data.repository

import com.example.data.local.ActivityDao
import com.example.data.local.ProductDao
import com.example.data.local.SavingsDao
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ActivityType
import com.example.data.model.ProductEntity
import com.example.data.model.ProductStatus
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BuySpaceRepository(
    private val productDao: ProductDao,
    private val savingsDao: SavingsDao,
    private val activityDao: ActivityDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val savingsAccount: Flow<SavingsAccountEntity?> = savingsDao.getAccount()
    val allTransactions: Flow<List<SavingsTransactionEntity>> = savingsDao.getAllTransactions()
    val allActivities: Flow<List<ActivityLogEntity>> = activityDao.getAllActivities()

    suspend fun ensureAccountInitialized() = withContext(Dispatchers.IO) {
        val existing = savingsDao.getAccountSync()
        if (existing == null) {
            savingsDao.insertOrUpdateAccount(
                SavingsAccountEntity(
                    id = 1,
                    totalDeposited = 0.0,
                    unallocatedBalance = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else if (existing.totalDeposited == 15000.0 && existing.unallocatedBalance == 3500.0) {
            // Clean up any legacy example data to ensure the app is completely empty
            productDao.deleteAllProducts()
            savingsDao.deleteAllTransactions()
            activityDao.clearAll()
            savingsDao.insertOrUpdateAccount(
                SavingsAccountEntity(
                    id = 1,
                    totalDeposited = 0.0,
                    unallocatedBalance = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        productDao.deleteAllProducts()
        savingsDao.deleteAllTransactions()
        activityDao.clearAll()
        savingsDao.insertOrUpdateAccount(
            SavingsAccountEntity(
                id = 1,
                totalDeposited = 0.0,
                unallocatedBalance = 0.0,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun addProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        activityDao.insertActivity(
            ActivityLogEntity(
                type = ActivityType.PRODUCT_ADDED.name,
                title = "Added ${product.name}",
                description = "Goal: ${CurrencyFormatter.format(product.targetPrice)} (${product.storeName})",
                productId = id,
                amount = product.targetPrice,
                timestamp = System.currentTimeMillis()
            )
        )

        if (product.savedAmount >= product.targetPrice && product.targetPrice > 0) {
            activityDao.insertActivity(
                ActivityLogEntity(
                    type = ActivityType.PRODUCT_FUNDED.name,
                    title = "${product.name} is Ready to Buy!",
                    description = "Goal reached: ${CurrencyFormatter.format(product.targetPrice)}",
                    productId = id,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        id
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        val current = productDao.getProductById(product.id)
        val wasFunded = current?.isFunded == true
        val isNowFunded = product.savedAmount >= product.targetPrice && product.targetPrice > 0

        productDao.updateProduct(product)

        if (!wasFunded && isNowFunded) {
            activityDao.insertActivity(
                ActivityLogEntity(
                    type = ActivityType.PRODUCT_FUNDED.name,
                    title = "${product.name} is Fully Funded!",
                    description = "Ready to buy for ${CurrencyFormatter.format(product.targetPrice)}",
                    productId = product.id,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        // Return allocated funds back to unallocated savings pool so money is preserved
        if (product.savedAmount > 0) {
            val acc = savingsDao.getAccountSync() ?: SavingsAccountEntity()
            savingsDao.insertOrUpdateAccount(
                acc.copy(
                    unallocatedBalance = acc.unallocatedBalance + product.savedAmount,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            savingsDao.insertTransaction(
                SavingsTransactionEntity(
                    type = TransactionType.DEALLOCATE.name,
                    amount = product.savedAmount,
                    productId = product.id,
                    productName = product.name,
                    note = "Returned to unallocated savings after item removal",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        productDao.deleteProductById(product.id)

        activityDao.insertActivity(
            ActivityLogEntity(
                type = ActivityType.PRODUCT_REMOVED.name,
                title = "Removed ${product.name}",
                description = if (product.savedAmount > 0) {
                    "Returned ${CurrencyFormatter.format(product.savedAmount)} to unallocated savings"
                } else {
                    "Item removed from wishlist"
                },
                productId = product.id,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun addMoneyToSavings(amount: Double, note: String? = null) = withContext(Dispatchers.IO) {
        val acc = savingsDao.getAccountSync() ?: SavingsAccountEntity()
        val updated = acc.copy(
            totalDeposited = acc.totalDeposited + amount,
            unallocatedBalance = acc.unallocatedBalance + amount,
            lastUpdated = System.currentTimeMillis()
        )
        savingsDao.insertOrUpdateAccount(updated)

        savingsDao.insertTransaction(
            SavingsTransactionEntity(
                type = TransactionType.DEPOSIT.name,
                amount = amount,
                note = note ?: "Deposit to savings",
                timestamp = System.currentTimeMillis()
            )
        )

        activityDao.insertActivity(
            ActivityLogEntity(
                type = ActivityType.MONEY_ADDED.name,
                title = "Added ${CurrencyFormatter.format(amount)} to Savings",
                description = "Total pool: ${CurrencyFormatter.format(updated.totalDeposited)}",
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun allocateMoneyToProduct(
        product: ProductEntity,
        amount: Double,
        deductFromUnallocated: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val acc = savingsDao.getAccountSync() ?: SavingsAccountEntity()

        if (deductFromUnallocated) {
            val newUnallocated = maxOf(0.0, acc.unallocatedBalance - amount)
            val newTotal = if (amount > acc.unallocatedBalance) {
                // If user allocated more than unallocated, increase totalDeposited by difference
                acc.totalDeposited + (amount - acc.unallocatedBalance)
            } else {
                acc.totalDeposited
            }
            savingsDao.insertOrUpdateAccount(
                acc.copy(
                    totalDeposited = newTotal,
                    unallocatedBalance = newUnallocated,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            // Fresh money directly allocated to this product
            savingsDao.insertOrUpdateAccount(
                acc.copy(
                    totalDeposited = acc.totalDeposited + amount,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        val newSaved = product.savedAmount + amount
        val isNowFunded = newSaved >= product.targetPrice && product.targetPrice > 0
        val newStatus = if (isNowFunded && product.status != ProductStatus.PURCHASED.name) {
            ProductStatus.READY_TO_BUY.name
        } else {
            product.status
        }

        productDao.updateSavedAmount(product.id, newSaved, newStatus)

        savingsDao.insertTransaction(
            SavingsTransactionEntity(
                type = TransactionType.ALLOCATE.name,
                amount = amount,
                productId = product.id,
                productName = product.name,
                note = "Allocated to ${product.name}",
                timestamp = System.currentTimeMillis()
            )
        )

        activityDao.insertActivity(
            ActivityLogEntity(
                type = ActivityType.MONEY_ALLOCATED.name,
                title = "Allocated ${CurrencyFormatter.format(amount)} to ${product.name}",
                description = "Now saved: ${CurrencyFormatter.format(newSaved)} of ${CurrencyFormatter.format(product.targetPrice)}",
                productId = product.id,
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
        )

        if (!product.isFunded && isNowFunded) {
            activityDao.insertActivity(
                ActivityLogEntity(
                    type = ActivityType.PRODUCT_FUNDED.name,
                    title = "${product.name} is Fully Funded!",
                    description = "100% saved (${CurrencyFormatter.format(product.targetPrice)})",
                    productId = product.id,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun markProductPurchased(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.updateStatus(product.id, ProductStatus.PURCHASED.name)

        activityDao.insertActivity(
            ActivityLogEntity(
                type = ActivityType.PRODUCT_PURCHASED.name,
                title = "Purchased ${product.name}!",
                description = "Goal achieved: ${CurrencyFormatter.format(product.targetPrice)} from ${product.storeName}",
                productId = product.id,
                amount = product.targetPrice,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun markProductActive(product: ProductEntity) = withContext(Dispatchers.IO) {
        val newStatus = if (product.isFunded) ProductStatus.READY_TO_BUY.name else ProductStatus.SAVING.name
        productDao.updateStatus(product.id, newStatus)
    }
}
