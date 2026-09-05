package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.local.BuySpaceDatabase
import com.example.data.model.ProductEntity
import com.example.data.repository.BuySpaceRepository
import com.example.ui.components.AddMoneyDialog
import com.example.ui.components.AddProductModal
import com.example.ui.components.AllocateMoneyDialog
import com.example.ui.components.EditProductModal
import com.example.ui.components.FloatingBottomNav
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.NavTab
import com.example.ui.components.ProductOptionsBottomSheet
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.BuySpaceTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferences
import com.example.ui.viewmodel.BuySpaceViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedUrl = extractUrlFromIntent(intent)

        val database = BuySpaceDatabase.getDatabase(applicationContext)
        val repository = BuySpaceRepository(
            productDao = database.productDao(),
            savingsDao = database.savingsDao(),
            activityDao = database.activityDao()
        )

        setContent {
            val viewModel: BuySpaceViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return BuySpaceViewModel(repository) as T
                    }
                }
            )

            val themePreferences = remember { ThemePreferences(applicationContext) }
            var themeMode by remember { mutableStateOf(themePreferences.getThemeMode()) }

            BuySpaceTheme(themeMode = themeMode) {
                BuySpaceApp(
                    viewModel = viewModel,
                    initialSharedUrl = sharedUrl,
                    currentThemeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        themePreferences.setThemeMode(newMode)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val urlRegex = "(https?://[^\\s]+)".toRegex()
                val match = urlRegex.find(sharedText)
                return match?.value ?: sharedText.trim()
            }
        }
        return null
    }
}

@Composable
fun BuySpaceApp(
    viewModel: BuySpaceViewModel,
    initialSharedUrl: String? = null,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 0) { NavTab.entries.size }
    val coroutineScope = rememberCoroutineScope()
    val currentTab = NavTab.entries.getOrElse(pagerState.currentPage) { NavTab.HOME }

    // Preserve scroll position for each tab independently
    val homeListState = rememberLazyListState()
    val wishlistListState = rememberLazyListState()
    val savingsListState = rememberLazyListState()
    val activityListState = rememberLazyListState()

    // Dialog & Modal visibility states
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showAddProductModal by remember { mutableStateOf(false) }
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var productForAllocation by remember { mutableStateOf<ProductEntity?>(null) }
    var productForOptions by remember { mutableStateOf<ProductEntity?>(null) }
    var productForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var currentSharedUrl by remember { mutableStateOf(initialSharedUrl) }

    // Auto-open Add Product modal if opened via Share Intent
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            currentSharedUrl = initialSharedUrl
            showAddProductModal = true
            coroutineScope.launch {
                pagerState.scrollToPage(0)
            }
        }
    }

    // Handle back button: return to Home tab if on another tab
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    // Reactive states from ViewModel using lifecycle-aware collectors
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val onlyReady by viewModel.filterOnlyReady.collectAsStateWithLifecycle()
    val savingsAccount by viewModel.savingsAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            FloatingBottomNav(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                },
                onAddClicked = { showQuickAddSheet = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Horizontal swipe to change tab
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> NavTab.entries[page].name }
            ) { page ->
                when (NavTab.entries[page]) {
                    NavTab.HOME -> {
                        HomeScreen(
                            summary = dashboardSummary,
                            listState = homeListState,
                            onNavigateTab = { targetTab ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetTab.ordinal)
                                }
                            },
                            onAddProductClick = { showAddProductModal = true },
                            onAddMoneyClick = { showAddMoneyDialog = true },
                            onProductClick = { productForOptions = it },
                            onAllocateClick = { productForAllocation = it },
                            onOptionsClick = { productForOptions = it },
                            themeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange
                        )
                    }
                    NavTab.WISHLIST -> {
                        WishlistScreen(
                            products = filteredProducts,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            selectedSort = selectedSort,
                            onlyReady = onlyReady,
                            listState = wishlistListState,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onCategorySelected = { viewModel.setCategory(it) },
                            onSortSelected = { viewModel.setSort(it) },
                            onToggleOnlyReady = { viewModel.toggleFilterOnlyReady() },
                            onAddProductClick = { showAddProductModal = true },
                            onProductClick = { productForOptions = it },
                            onAllocateClick = { productForAllocation = it },
                            onOptionsClick = { productForOptions = it }
                        )
                    }
                    NavTab.SAVINGS -> {
                        SavingsScreen(
                            savingsAccount = savingsAccount,
                            summary = dashboardSummary,
                            transactions = transactions,
                            listState = savingsListState,
                            onAddMoneyClick = { showAddMoneyDialog = true }
                        )
                    }
                    NavTab.ACTIVITY -> {
                        ActivityScreen(
                            activities = activities,
                            listState = activityListState,
                            onImportExportClick = { showImportExportDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Modal: Quick Add Bottom Sheet
    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            onAddProduct = {
                currentSharedUrl = null
                showAddProductModal = true
            },
            onAddMoney = { showAddMoneyDialog = true },
            onDismiss = { showQuickAddSheet = false }
        )
    }

    // Modal: Add Product (From Link or Manually)
    if (showAddProductModal) {
        AddProductModal(
            initialUrl = currentSharedUrl,
            isExtracting = isExtracting,
            onExtractUrl = { url, callback ->
                viewModel.extractFromLink(url, callback)
            },
            onAddProduct = { name, price, saved, imageUrl, originalUrl, storeName, category, priority, notes ->
                viewModel.addProduct(
                    name = name,
                    targetPrice = price,
                    savedAmount = saved,
                    imageUrl = imageUrl,
                    originalUrl = originalUrl,
                    storeName = storeName,
                    category = category,
                    priority = priority,
                    notes = notes
                )
            },
            onDismiss = {
                showAddProductModal = false
                currentSharedUrl = null
            }
        )
    }

    // Modal: Add Money Dialog
    if (showAddMoneyDialog) {
        AddMoneyDialog(
            currentBalance = savingsAccount.unallocatedBalance,
            onAddMoney = { amount, note ->
                viewModel.addMoneyToSavings(amount, note)
            },
            onDismiss = { showAddMoneyDialog = false }
        )
    }

    // Modal: Allocate Money to Product Dialog
    productForAllocation?.let { product ->
        AllocateMoneyDialog(
            product = product,
            unallocatedBalance = savingsAccount.unallocatedBalance,
            onAllocate = { amount, deductFromUnallocated ->
                viewModel.allocateMoneyToProduct(product, amount, deductFromUnallocated)
            },
            onDismiss = { productForAllocation = null }
        )
    }

    // Modal: Product Options Bottom Sheet
    productForOptions?.let { product ->
        ProductOptionsBottomSheet(
            product = product,
            onMarkPurchased = { viewModel.markProductPurchased(product) },
            onMarkActive = { viewModel.markProductActive(product) },
            onDelete = { viewModel.deleteProduct(product) },
            onEdit = { productForEdit = product },
            onDismiss = { productForOptions = null }
        )
    }

    // Modal: Edit Product Modal
    productForEdit?.let { product ->
        EditProductModal(
            product = product,
            onSave = { updated -> viewModel.updateProduct(updated) },
            onDismiss = { productForEdit = null }
        )
    }

    // Modal: Import & Export Data Backup
    if (showImportExportDialog) {
        ImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showImportExportDialog = false }
        )
    }
}
