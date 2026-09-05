package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductPriority
import com.example.data.model.ProductStatus
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import com.example.data.repository.BackupSummary
import com.example.data.repository.BuySpaceRepository
import com.example.util.ExtractedProductInfo
import com.example.util.LinkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WishlistSortOption(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    PROGRESS_HIGH("Closest to Goal"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    PRIORITY("Priority"),
    READY_TO_BUY("Ready to Buy First")
}

@Immutable
data class DashboardSummary(
    val totalNeeded: Double = 0.0,
    val totalSaved: Double = 0.0,
    val totalAllocated: Double = 0.0,
    val unallocatedSavings: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progress: Float = 0f,
    val progressPercent: Int = 0,
    val totalItemsCount: Int = 0,
    val readyToBuyCount: Int = 0,
    val purchasedCount: Int = 0,
    val recentlyAdded: List<ProductEntity> = emptyList(),
    val closestToFunded: List<ProductEntity> = emptyList()
)

class BuySpaceViewModel(
    private val repository: BuySpaceRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureAccountInitialized()
        }
    }

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsAccount: StateFlow<SavingsAccountEntity> = repository.savingsAccount
        .combine(MutableStateFlow(SavingsAccountEntity())) { account, default ->
            account ?: default
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsAccountEntity())

    val allTransactions: StateFlow<List<SavingsTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityLogEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter & Sort State for Wishlist
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow(WishlistSortOption.RECENTLY_ADDED)
    val selectedSort: StateFlow<WishlistSortOption> = _selectedSort.asStateFlow()

    private val _filterOnlyReady = MutableStateFlow(false)
    val filterOnlyReady: StateFlow<Boolean> = _filterOnlyReady.asStateFlow()

    // Link Extraction State
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractionResult = MutableStateFlow<ExtractedProductInfo?>(null)
    val extractionResult: StateFlow<ExtractedProductInfo?> = _extractionResult.asStateFlow()

    // Dashboard calculations combined reactively
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allProducts,
        savingsAccount
    ) { products, account ->
        val activeProducts = products.filter { it.status != ProductStatus.PURCHASED.name }
        val purchased = products.filter { it.status == ProductStatus.PURCHASED.name }

        val totalNeeded = activeProducts.sumOf { it.targetPrice }
        val totalAllocated = activeProducts.sumOf { it.savedAmount }
        val unallocated = account.unallocatedBalance
        val totalSaved = totalAllocated + unallocated
        val remaining = maxOf(0.0, totalNeeded - totalAllocated)
        val progress = if (totalNeeded <= 0) 0f else (totalAllocated / totalNeeded).toFloat().coerceIn(0f, 1f)

        val readyToBuy = activeProducts.filter { it.isFunded }

        val closest = activeProducts
            .filter { !it.isFunded && it.targetPrice > 0 }
            .sortedByDescending { it.progress }
            .take(5)

        val recent = activeProducts
            .sortedByDescending { it.createdAt }
            .take(5)

        DashboardSummary(
            totalNeeded = totalNeeded,
            totalSaved = totalSaved,
            totalAllocated = totalAllocated,
            unallocatedSavings = unallocated,
            remainingAmount = remaining,
            progress = progress,
            progressPercent = (progress * 100).toInt(),
            totalItemsCount = activeProducts.size,
            readyToBuyCount = readyToBuy.size,
            purchasedCount = purchased.size,
            recentlyAdded = recent,
            closestToFunded = closest
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Filtered wishlist items
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory,
        _selectedSort,
        _filterOnlyReady
    ) { products, query, category, sort, onlyReady ->
        var list = products

        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.storeName.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            }
        }

        if (category != null && category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (onlyReady) {
            list = list.filter { it.isFunded && it.status != ProductStatus.PURCHASED.name }
        }

        when (sort) {
            WishlistSortOption.RECENTLY_ADDED -> list.sortedByDescending { it.createdAt }
            WishlistSortOption.PROGRESS_HIGH -> list.sortedWith(
                compareByDescending<ProductEntity> { it.progress }
                    .thenByDescending { it.savedAmount }
            )
            WishlistSortOption.PRICE_LOW_HIGH -> list.sortedBy { it.targetPrice }
            WishlistSortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.targetPrice }
            WishlistSortOption.PRIORITY -> list.sortedBy {
                ProductPriority.fromString(it.priority).level
            }
            WishlistSortOption.READY_TO_BUY -> list.sortedWith(
                compareByDescending<ProductEntity> { it.isFunded }
                    .thenByDescending { it.progress }
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSort(sort: WishlistSortOption) {
        _selectedSort.value = sort
    }

    fun toggleFilterOnlyReady() {
        _filterOnlyReady.value = !_filterOnlyReady.value
    }

    fun extractFromLink(url: String, onComplete: (ExtractedProductInfo) -> Unit) {
        viewModelScope.launch {
            _isExtracting.value = true
            val result = LinkExtractor.extract(url)
            _extractionResult.value = result
            _isExtracting.value = false
            onComplete(result)
        }
    }

    fun clearExtraction() {
        _extractionResult.value = null
    }

    fun addProduct(
        name: String,
        targetPrice: Double,
        savedAmount: Double = 0.0,
        imageUrl: String? = null,
        originalUrl: String? = null,
        storeName: String = "Store",
        category: String = "Tech & Gadgets",
        priority: String = ProductPriority.MEDIUM.name,
        notes: String = "",
        targetDate: Long? = null
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                name = name.trim(),
                targetPrice = targetPrice,
                savedAmount = savedAmount,
                imageUrl = imageUrl?.trim()?.ifBlank { null },
                originalUrl = originalUrl?.trim()?.ifBlank { null },
                storeName = storeName.trim().ifBlank { "Store" },
                category = category,
                priority = priority,
                notes = notes.trim(),
                targetDate = targetDate,
                status = if (savedAmount >= targetPrice && targetPrice > 0) {
                    ProductStatus.READY_TO_BUY.name
                } else {
                    ProductStatus.SAVING.name
                }
            )
            repository.addProduct(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun addMoneyToSavings(amount: Double, note: String? = null) {
        viewModelScope.launch {
            if (amount > 0) {
                repository.addMoneyToSavings(amount, note)
            }
        }
    }

    fun allocateMoneyToProduct(product: ProductEntity, amount: Double, deductFromUnallocated: Boolean = true) {
        viewModelScope.launch {
            if (amount > 0) {
                repository.allocateMoneyToProduct(product, amount, deductFromUnallocated)
            }
        }
    }

    fun markProductPurchased(product: ProductEntity) {
        viewModelScope.launch {
            repository.markProductPurchased(product)
        }
    }

    fun markProductActive(product: ProductEntity) {
        viewModelScope.launch {
            repository.markProductActive(product)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun loadSampleRoomData() {
        viewModelScope.launch {
            repository.populateDefaultRoomData()
        }
    }

    suspend fun getBackupJson(): String {
        return repository.exportAllDataJson()
    }

    suspend fun restoreFromJson(json: String): Result<BackupSummary> {
        return repository.importDataFromJson(json)
    }
}
