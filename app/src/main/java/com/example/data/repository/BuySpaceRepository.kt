package com.example.data.repository

import com.example.data.local.ActivityDao
import com.example.data.local.ProductDao
import com.example.data.local.SavingsDao
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ActivityType
import com.example.data.model.ProductEntity
import com.example.data.model.ProductPriority
import com.example.data.model.ProductStatus
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val productsCount: Int,
    val transactionsCount: Int,
    val activitiesCount: Int,
    val totalSaved: Double,
    val unallocatedBalance: Double
)

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

    suspend fun exportAllDataJson(): String = withContext(Dispatchers.IO) {
        val account = savingsDao.getAccountSync() ?: SavingsAccountEntity(id = 1, totalDeposited = 0.0, unallocatedBalance = 0.0)
        val products = productDao.getAllProductsList()
        val transactions = savingsDao.getAllTransactionsList()
        val activities = activityDao.getAllActivitiesList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "BuySpace")
        root.put("exportedAt", System.currentTimeMillis())

        // Account
        val accountJson = JSONObject().apply {
            put("id", account.id)
            put("totalDeposited", account.totalDeposited)
            put("unallocatedBalance", account.unallocatedBalance)
            put("lastUpdated", account.lastUpdated)
        }
        root.put("account", accountJson)

        // Products
        val productsArray = JSONArray()
        for (p in products) {
            val pJson = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("targetPrice", p.targetPrice)
                put("savedAmount", p.savedAmount)
                put("imageUrl", p.imageUrl ?: "")
                put("originalUrl", p.originalUrl ?: "")
                put("storeName", p.storeName)
                put("category", p.category)
                put("priority", p.priority)
                put("notes", p.notes)
                put("targetDate", p.targetDate ?: JSONObject.NULL)
                put("status", p.status)
                put("createdAt", p.createdAt)
            }
            productsArray.put(pJson)
        }
        root.put("products", productsArray)

        // Transactions
        val txArray = JSONArray()
        for (t in transactions) {
            val tJson = JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amount", t.amount)
                put("productId", t.productId ?: JSONObject.NULL)
                put("productName", t.productName ?: "")
                put("note", t.note ?: "")
                put("timestamp", t.timestamp)
            }
            txArray.put(tJson)
        }
        root.put("transactions", txArray)

        // Activities
        val actArray = JSONArray()
        for (a in activities) {
            val aJson = JSONObject().apply {
                put("id", a.id)
                put("type", a.type)
                put("title", a.title)
                put("description", a.description)
                put("amount", a.amount ?: JSONObject.NULL)
                put("productId", a.productId ?: JSONObject.NULL)
                put("timestamp", a.timestamp)
            }
            actArray.put(aJson)
        }
        root.put("activities", actArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonString)

            // Parse Account
            val accountObj = root.optJSONObject("account")
            val totalDeposited = accountObj?.optDouble("totalDeposited", 0.0) ?: 0.0
            val unallocatedBalance = accountObj?.optDouble("unallocatedBalance", 0.0) ?: 0.0
            val lastUpdated = accountObj?.optLong("lastUpdated", System.currentTimeMillis()) ?: System.currentTimeMillis()

            // Parse Products
            val productsArray = root.optJSONArray("products") ?: JSONArray()
            val productsList = mutableListOf<ProductEntity>()
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                val img = obj.optString("imageUrl", "").takeIf { it.isNotBlank() }
                val url = obj.optString("originalUrl", "").takeIf { it.isNotBlank() }
                val store = obj.optString("storeName", "Store").ifBlank { "Store" }
                val notes = obj.optString("notes", "")
                val targetDate = if (obj.has("targetDate") && !obj.isNull("targetDate")) obj.getLong("targetDate") else null

                productsList.add(
                    ProductEntity(
                        id = if (obj.has("id")) obj.getLong("id") else 0L,
                        name = obj.getString("name"),
                        targetPrice = obj.getDouble("targetPrice"),
                        savedAmount = obj.optDouble("savedAmount", 0.0),
                        imageUrl = img,
                        originalUrl = url,
                        storeName = store,
                        category = obj.optString("category", "Tech & Gadgets"),
                        priority = obj.optString("priority", ProductPriority.MEDIUM.name),
                        notes = notes,
                        targetDate = targetDate,
                        status = obj.optString("status", ProductStatus.SAVING.name),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse Transactions
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            val txList = mutableListOf<SavingsTransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val prodId = if (obj.has("productId") && !obj.isNull("productId")) obj.getLong("productId") else null
                val prodName = obj.optString("productName", "").takeIf { it.isNotBlank() }
                val note = obj.optString("note", "").takeIf { it.isNotBlank() }
                txList.add(
                    SavingsTransactionEntity(
                        id = if (obj.has("id")) obj.getLong("id") else 0L,
                        type = obj.optString("type", TransactionType.DEPOSIT.name),
                        amount = obj.getDouble("amount"),
                        productId = prodId,
                        productName = prodName,
                        note = note,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Parse Activities
            val actArray = root.optJSONArray("activities") ?: JSONArray()
            val actList = mutableListOf<ActivityLogEntity>()
            for (i in 0 until actArray.length()) {
                val obj = actArray.getJSONObject(i)
                val amt = if (obj.has("amount") && !obj.isNull("amount")) obj.getDouble("amount") else null
                val prodId = if (obj.has("productId") && !obj.isNull("productId")) obj.getLong("productId") else null
                actList.add(
                    ActivityLogEntity(
                        id = if (obj.has("id")) obj.getLong("id") else 0L,
                        type = obj.optString("type", ActivityType.MONEY_ADDED.name),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        amount = amt,
                        productId = prodId,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Clean existing database and populate with imported data
            productDao.deleteAllProducts()
            savingsDao.deleteAllTransactions()
            activityDao.clearAll()

            savingsDao.insertOrUpdateAccount(
                SavingsAccountEntity(
                    id = 1,
                    totalDeposited = totalDeposited,
                    unallocatedBalance = unallocatedBalance,
                    lastUpdated = lastUpdated
                )
            )

            if (productsList.isNotEmpty()) {
                productDao.insertProducts(productsList)
            }
            if (txList.isNotEmpty()) {
                savingsDao.insertTransactions(txList)
            }
            if (actList.isNotEmpty()) {
                activityDao.insertActivities(actList)
            }

            activityDao.insertActivity(
                ActivityLogEntity(
                    type = ActivityType.MONEY_ADDED.name,
                    title = "Data Restored from Backup",
                    description = "Restored ${productsList.size} products & ${CurrencyFormatter.format(totalDeposited)} savings",
                    amount = totalDeposited,
                    timestamp = System.currentTimeMillis()
                )
            )

            BackupSummary(
                productsCount = productsList.size,
                transactionsCount = txList.size,
                activitiesCount = actList.size,
                totalSaved = totalDeposited,
                unallocatedBalance = unallocatedBalance
            )
        }
    }
}
