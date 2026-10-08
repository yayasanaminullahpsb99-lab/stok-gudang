package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.InventoryItem
import com.example.data.repository.WarehouseRepository
import com.example.ui.auth.signOutWarehouseSession
import com.example.ui.components.AddOrEditSkuDialog
import com.example.ui.components.StockMovementTransactionDialog
import com.example.ui.screens.AnalyticsDashboardScreen
import com.example.ui.screens.BarcodeScannerScreen
import com.example.ui.screens.InventoryCatalogScreen
import com.example.ui.screens.MovementsAndAuditScreen
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WarehouseViewModel
import com.google.firebase.firestore.FirebaseFirestore

enum class WarehouseTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    ANALYTICS("Analitik", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_tab_analytics"),
    INVENTORY("Inventaris", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_tab_inventory"),
    SCANNER("Pemindai", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_tab_scanner"),
    MOVEMENTS("Mutasi & Audit", Icons.Filled.SwapVert, Icons.Outlined.SwapVert, "nav_tab_movements")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWarehouseApp(
    currentUserId: String,
    userEmail: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: WarehouseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WarehouseViewModel(WarehouseRepository(db), currentUserId)
            }
        }
    )

    val inventoryState by viewModel.inventoryState.collectAsStateWithLifecycle()
    val movementsState by viewModel.movementsState.collectAsStateWithLifecycle()
    val auditsState by viewModel.auditsState.collectAsStateWithLifecycle()
    val analyticsSummary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredInventoryItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val movementFilterType by viewModel.movementFilterType.collectAsStateWithLifecycle()
    val lastScannedCode by viewModel.lastScannedCode.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(WarehouseTab.ANALYTICS) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var prefilledBarcodeForNewSku by remember { mutableStateOf("") }

    var activeTransactionItem by remember { mutableStateOf<InventoryItem?>(null) }
    var activeTransactionInitialType by remember { mutableStateOf("INBOUND") }

    BackHandler(enabled = currentTab != WarehouseTab.ANALYTICS) {
        currentTab = WarehouseTab.ANALYTICS
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val allItems = (inventoryState as? UiState.Success)?.data ?: emptyList()
    val allMovements = (movementsState as? UiState.Success)?.data ?: emptyList()
    val allAudits = (auditsState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Slate950,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "StokGudang Pro",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Surface(
                                    color = Emerald400.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Emerald400)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "REAL-TIME",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Emerald400
                                        )
                                    }
                                }
                            }
                            Text(
                                text = userEmail ?: "Operator Gudang Terautentikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.seedStandardWarehouseData() },
                            modifier = Modifier.testTag("topbar_seed_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Muat Dataset Gudang Standar",
                                tint = CyberTeal400
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutWarehouseSession(
                                    context = context,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Keluar Akun",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Slate900
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = Slate900,
                        tonalElevation = 8.dp
                    ) {
                        WarehouseTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Slate950,
                                    selectedTextColor = LaserAmber500,
                                    indicatorColor = LaserAmber500,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == WarehouseTab.INVENTORY || currentTab == WarehouseTab.ANALYTICS) {
                    FloatingActionButton(
                        onClick = {
                            editingItem = null
                            prefilledBarcodeForNewSku = ""
                            showAddOrEditDialog = true
                        },
                        containerColor = LaserAmber500,
                        contentColor = Slate950,
                        modifier = Modifier.testTag("fab_add_sku")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah SKU Baru")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SKU Baru",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isBusy || inventoryState is UiState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = LaserAmber500,
                        trackColor = Slate800
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = Slate900,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            WarehouseTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            WarehouseTab.ANALYTICS -> {
                                AnalyticsDashboardScreen(
                                    analytics = analyticsSummary,
                                    items = allItems,
                                    movements = allMovements,
                                    onNavigateToScanner = { currentTab = WarehouseTab.SCANNER },
                                    onOpenAddSku = {
                                        editingItem = null
                                        prefilledBarcodeForNewSku = ""
                                        showAddOrEditDialog = true
                                    },
                                    onSelectItem = { item ->
                                        activeTransactionItem = item
                                        activeTransactionInitialType = "INBOUND"
                                    },
                                    onSeedDemoData = { viewModel.seedStandardWarehouseData() }
                                )
                            }

                            WarehouseTab.INVENTORY -> {
                                InventoryCatalogScreen(
                                    items = filteredItems,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    filterMode = filterMode,
                                    sortMode = sortMode,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onCategoryChange = viewModel::updateSelectedCategory,
                                    onFilterModeChange = viewModel::updateFilterMode,
                                    onSortModeChange = viewModel::updateSortMode,
                                    onSelectItem = { item ->
                                        activeTransactionItem = item
                                        activeTransactionInitialType = "INBOUND"
                                    },
                                    onOpenAddSku = {
                                        editingItem = null
                                        prefilledBarcodeForNewSku = ""
                                        showAddOrEditDialog = true
                                    },
                                    onSeedDemoData = { viewModel.seedStandardWarehouseData() }
                                )
                            }

                            WarehouseTab.SCANNER -> {
                                BarcodeScannerScreen(
                                    items = allItems,
                                    scannedCode = lastScannedCode,
                                    onCodeDetected = viewModel::onBarcodeDetected,
                                    onOpenTransactionDialog = { item, txType ->
                                        activeTransactionItem = item
                                        activeTransactionInitialType = txType
                                    },
                                    onQuickStockStep = { item, txType ->
                                        viewModel.executeStockMovement(
                                            item = item,
                                            type = txType,
                                            quantityInput = 1L,
                                            referenceDoc = "SCAN-${System.currentTimeMillis() % 100000}",
                                            notes = "Pemindaian Cepat Laser ($txType)"
                                        )
                                    },
                                    onRegisterUnrecognizedBarcode = { rawCode ->
                                        editingItem = null
                                        prefilledBarcodeForNewSku = rawCode
                                        showAddOrEditDialog = true
                                    }
                                )
                            }

                            WarehouseTab.MOVEMENTS -> {
                                MovementsAndAuditScreen(
                                    items = allItems,
                                    movements = allMovements,
                                    audits = allAudits,
                                    selectedFilterType = movementFilterType,
                                    onFilterTypeChange = viewModel::updateMovementFilterType,
                                    onSubmitAuditCount = { item, sessionCode, countedQty, autoReconcile, notes ->
                                        viewModel.submitAuditCount(
                                            item = item,
                                            sessionCode = sessionCode,
                                            countedQty = countedQty,
                                            autoReconcileStock = autoReconcile,
                                            notes = notes
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Master SKU Dialog
    if (showAddOrEditDialog) {
        val targetEdit = editingItem
        AddOrEditSkuDialog(
            initialItem = targetEdit,
            prefilledBarcode = prefilledBarcodeForNewSku,
            onDismiss = { showAddOrEditDialog = false },
            onSave = { sku, barcode, name, category, zone, aisle, quantity, minStock, maxStock, unit, unitCost, unitPrice, supplier, batch ->
                if (targetEdit == null) {
                    viewModel.createNewItem(
                        sku = sku,
                        barcode = barcode,
                        name = name,
                        category = category,
                        zone = zone,
                        aisle = aisle,
                        quantity = quantity,
                        minStock = minStock,
                        maxStock = maxStock,
                        unit = unit,
                        unitCost = unitCost,
                        unitPrice = unitPrice,
                        supplierName = supplier,
                        batchNumber = batch,
                        onSuccess = { showAddOrEditDialog = false }
                    )
                } else {
                    viewModel.updateItemDetails(
                        itemId = targetEdit.id,
                        sku = sku,
                        barcode = barcode,
                        name = name,
                        category = category,
                        zone = zone,
                        aisle = aisle,
                        quantity = quantity,
                        minStock = minStock,
                        maxStock = maxStock,
                        unit = unit,
                        unitCost = unitCost,
                        unitPrice = unitPrice,
                        supplierName = supplier,
                        batchNumber = batch,
                        onSuccess = { showAddOrEditDialog = false }
                    )
                }
            }
        )
    }

    // Stock Movement Transaction Dialog
    val currentTxItem = activeTransactionItem
    if (currentTxItem != null) {
        // Resolve freshest version from allItems if available
        val liveItem = allItems.firstOrNull { it.id == currentTxItem.id } ?: currentTxItem
        StockMovementTransactionDialog(
            item = liveItem,
            initialType = activeTransactionInitialType,
            onDismiss = { activeTransactionItem = null },
            onEditMaster = {
                activeTransactionItem = null
                editingItem = liveItem
                showAddOrEditDialog = true
            },
            onDeleteItem = {
                activeTransactionItem = null
                viewModel.deleteItem(liveItem)
            },
            onSubmitTransaction = { type, qtyInput, refDoc, targetZone, targetAisle, notes ->
                viewModel.executeStockMovement(
                    item = liveItem,
                    type = type,
                    quantityInput = qtyInput,
                    referenceDoc = refDoc,
                    targetZone = targetZone,
                    targetAisle = targetAisle,
                    notes = notes,
                    onSuccess = { activeTransactionItem = null }
                )
            }
        )
    }
}
