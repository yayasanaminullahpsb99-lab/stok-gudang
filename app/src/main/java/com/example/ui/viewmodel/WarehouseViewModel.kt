package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AbcSummary
import com.example.data.model.AuditSession
import com.example.data.model.InventoryItem
import com.example.data.model.StockMovement
import com.example.data.model.WarehouseAnalyticsSummary
import com.example.data.model.ZoneStat
import com.example.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class InventoryFilterMode(val label: String) {
    ALL("Semua SKU"),
    LOW_STOCK("Stok Menipis"),
    OUT_OF_STOCK("Habis (0)"),
    OVERSTOCK("Overstock"),
    CLASS_A("Prioritas A (ABC)")
}

enum class InventorySortMode(val label: String) {
    UPDATED_DESC("Terbaru Diperbarui"),
    VALUE_DESC("Nilai Aset Tertinggi"),
    QTY_ASC("Stok Terendah"),
    QTY_DESC("Stok Tertinggi"),
    NAME_ASC("Nama (A-Z)")
}

class WarehouseViewModel(
    private val repository: WarehouseRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedZone = MutableStateFlow("Semua")
    val selectedZone: StateFlow<String> = _selectedZone.asStateFlow()

    private val _filterMode = MutableStateFlow(InventoryFilterMode.ALL)
    val filterMode: StateFlow<InventoryFilterMode> = _filterMode.asStateFlow()

    private val _sortMode = MutableStateFlow(InventorySortMode.UPDATED_DESC)
    val sortMode: StateFlow<InventorySortMode> = _sortMode.asStateFlow()

    private val _movementFilterType = MutableStateFlow("ALL")
    val movementFilterType: StateFlow<String> = _movementFilterType.asStateFlow()

    private val _lastScannedCode = MutableStateFlow("")
    val lastScannedCode: StateFlow<String> = _lastScannedCode.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val inventoryState: StateFlow<UiState<List<InventoryItem>>> = repository
        .observeInventoryItems(currentUserId)
        .map<List<InventoryItem>, UiState<List<InventoryItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing inventory items", error)
            emit(UiState.Error(error.localizedMessage ?: "Gagal memuat data inventaris"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val movementsState: StateFlow<UiState<List<StockMovement>>> = repository
        .observeStockMovements(currentUserId)
        .map<List<StockMovement>, UiState<List<StockMovement>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing stock movements", error)
            emit(UiState.Error(error.localizedMessage ?: "Gagal memuat riwayat mutasi"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val auditsState: StateFlow<UiState<List<AuditSession>>> = repository
        .observeAuditLogs(currentUserId)
        .map<List<AuditSession>, UiState<List<AuditSession>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing audit logs", error)
            emit(UiState.Error(error.localizedMessage ?: "Gagal memuat log stock opname"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val analyticsSummary: StateFlow<WarehouseAnalyticsSummary> = combine(
        inventoryState,
        movementsState,
        auditsState
    ) { invUi, movUi, audUi ->
        val items = (invUi as? UiState.Success)?.data ?: emptyList()
        val movements = (movUi as? UiState.Success)?.data ?: emptyList()
        val audits = (audUi as? UiState.Success)?.data ?: emptyList()
        computeAnalytics(items, movements, audits)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WarehouseAnalyticsSummary()
    )

    val filteredInventoryItems: StateFlow<List<InventoryItem>> = combine(
        inventoryState,
        _searchQuery,
        _selectedCategory,
        _selectedZone,
        combine(_filterMode, _sortMode, analyticsSummary) { f, s, a -> Triple(f, s, a) }
    ) { invUi, query, category, zone, (fMode, sMode, analytics) ->
        val items = (invUi as? UiState.Success)?.data ?: emptyList()
        val classAIds = analytics.abcClassification.classAItems.map { it.id }.toSet()
        val q = query.trim().lowercase()

        val filtered = items.filter { item ->
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.sku.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q) ||
                item.supplierName.lowercase().contains(q) ||
                item.aisle.lowercase().contains(q)

            val matchesCategory = category == "Semua" || item.category.equals(category, ignoreCase = true)
            val matchesZone = zone == "Semua" || item.zone.equals(zone, ignoreCase = true)

            val matchesMode = when (fMode) {
                InventoryFilterMode.ALL -> true
                InventoryFilterMode.LOW_STOCK -> item.isLowStock
                InventoryFilterMode.OUT_OF_STOCK -> item.isOutOfStock
                InventoryFilterMode.OVERSTOCK -> item.isOverstock
                InventoryFilterMode.CLASS_A -> item.id in classAIds
            }

            matchesQuery && matchesCategory && matchesZone && matchesMode
        }

        when (sMode) {
            InventorySortMode.UPDATED_DESC -> filtered
            InventorySortMode.VALUE_DESC -> filtered.sortedByDescending { it.totalCostValue }
            InventorySortMode.QTY_ASC -> filtered.sortedBy { it.quantity }
            InventorySortMode.QTY_DESC -> filtered.sortedByDescending { it.quantity }
            InventorySortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSelectedZone(zone: String) {
        _selectedZone.value = zone
    }

    fun updateFilterMode(mode: InventoryFilterMode) {
        _filterMode.value = mode
    }

    fun updateSortMode(mode: InventorySortMode) {
        _sortMode.value = mode
    }

    fun updateMovementFilterType(type: String) {
        _movementFilterType.value = type
    }

    fun onBarcodeDetected(code: String) {
        if (code.isNotBlank()) {
            _lastScannedCode.value = code.trim()
        }
    }

    fun clearScannedCode() {
        _lastScannedCode.value = ""
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun createNewItem(
        sku: String,
        barcode: String,
        name: String,
        category: String,
        zone: String,
        aisle: String,
        quantity: Long,
        minStock: Long,
        maxStock: Long,
        unit: String,
        unitCost: Double,
        unitPrice: Double,
        supplierName: String,
        batchNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createInventoryItem(
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
                supplierName = supplierName,
                batchNumber = batchNumber
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "SKU '$name' berhasil didaftarkan ke gudang"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = "Gagal menambah SKU: ${err.localizedMessage ?: "Kesalahan sistem"}"
            }
        }
    }

    fun updateItemDetails(
        itemId: String,
        sku: String,
        barcode: String,
        name: String,
        category: String,
        zone: String,
        aisle: String,
        quantity: Long,
        minStock: Long,
        maxStock: Long,
        unit: String,
        unitCost: Double,
        unitPrice: Double,
        supplierName: String,
        batchNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateInventoryItemDetails(
                itemId = itemId,
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
                supplierName = supplierName,
                batchNumber = batchNumber
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Detail SKU '$name' berhasil diperbarui"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = "Gagal memperbarui SKU: ${err.localizedMessage ?: "Kesalahan"}"
            }
        }
    }

    fun executeStockMovement(
        item: InventoryItem,
        type: String,
        quantityInput: Long,
        referenceDoc: String,
        targetZone: String = item.zone,
        targetAisle: String = item.aisle,
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordStockTransaction(
                item = item,
                type = type,
                quantityInput = quantityInput,
                referenceDoc = referenceDoc,
                targetZone = targetZone,
                targetAisle = targetAisle,
                notes = notes
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Transaksi $type (${quantityInput} ${item.unit}) berhasil dicatat"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = "Gagal mencatat mutasi: ${err.localizedMessage ?: "Kesalahan"}"
            }
        }
    }

    fun submitAuditCount(
        item: InventoryItem,
        sessionCode: String,
        countedQty: Long,
        autoReconcileStock: Boolean,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordAuditCount(
                item = item,
                sessionCode = sessionCode,
                countedQty = countedQty,
                autoReconcileStock = autoReconcileStock,
                notes = notes
            )
            _isBusy.value = false
            result.onSuccess {
                val diff = countedQty - item.quantity
                val statusText = if (diff == 0L) "Sesuai (Matched)" else "Selisih $diff unit"
                _actionMessage.value = "Audit '${item.sku}' tercatat: $statusText"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = "Gagal menyimpan hasil opname: ${err.localizedMessage ?: "Kesalahan"}"
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteInventoryItem(item.id)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "SKU '${item.name}' dihapus dari inventaris"
            }.onFailure { err ->
                _actionMessage.value = "Gagal menghapus SKU: ${err.localizedMessage ?: "Kesalahan"}"
            }
        }
    }

    fun seedStandardWarehouseData() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedEnterpriseWarehouseDataset()
            _isBusy.value = false
            result.onSuccess { count ->
                _actionMessage.value = "$count SKU gudang standar & histori mutasi berhasil dimuat"
            }.onFailure { err ->
                _actionMessage.value = "Gagal memuat data standar: ${err.localizedMessage ?: "Kesalahan"}"
            }
        }
    }

    private fun computeAnalytics(
        items: List<InventoryItem>,
        movements: List<StockMovement>,
        audits: List<AuditSession>
    ): WarehouseAnalyticsSummary {
        if (items.isEmpty()) {
            return WarehouseAnalyticsSummary()
        }

        val totalSkus = items.size
        val totalUnits = items.sumOf { it.quantity }
        val totalValuationCost = items.sumOf { it.totalCostValue }
        val totalValuationRetail = items.sumOf { it.totalRetailValue }
        val estimatedMarginValue = (totalValuationRetail - totalValuationCost).coerceAtLeast(0.0)

        val outOfStockCount = items.count { it.isOutOfStock }
        val lowStockCount = items.count { it.isLowStock }
        val overstockCount = items.count { it.isOverstock }
        val healthyStockCount = (totalSkus - outOfStockCount - lowStockCount - overstockCount).coerceAtLeast(0)

        val totalInboundUnits = movements.filter { it.type == "INBOUND" }.sumOf { it.quantityDelta }
        val totalOutboundUnits = movements.filter { it.type == "OUTBOUND" }.sumOf { it.quantityDelta }

        val categoryBreakdown = items
            .groupBy { it.category }
            .mapValues { (_, catItems) -> catItems.sumOf { it.totalCostValue } }

        val zoneUtilization = items
            .groupBy { it.zone }
            .mapValues { (zoneName, zoneItems) ->
                ZoneStat(
                    zoneName = zoneName,
                    skuCount = zoneItems.size,
                    currentUnits = zoneItems.sumOf { it.quantity },
                    maxCapacityUnits = zoneItems.sumOf { it.maxStock }.coerceAtLeast(1L),
                    totalValue = zoneItems.sumOf { it.totalCostValue }
                )
            }

        // ABC Pareto Classification based on Cost Valuation
        val sortedByVal = items.sortedByDescending { it.totalCostValue }
        val classA = mutableListOf<InventoryItem>()
        val classB = mutableListOf<InventoryItem>()
        val classC = mutableListOf<InventoryItem>()

        var cumulativeVal = 0.0
        val totalValSafe = totalValuationCost.coerceAtLeast(1.0)

        sortedByVal.forEachIndexed { index, item ->
            val prevRatio = cumulativeVal / totalValSafe
            cumulativeVal += item.totalCostValue
            when {
                prevRatio < 0.70 || index == 0 -> classA.add(item)
                prevRatio < 0.90 -> classB.add(item)
                else -> classC.add(item)
            }
        }

        val classAVal = classA.sumOf { it.totalCostValue }
        val classBVal = classB.sumOf { it.totalCostValue }
        val classCVal = classC.sumOf { it.totalCostValue }

        val abcSummary = AbcSummary(
            classAItems = classA,
            classBItems = classB,
            classCItems = classC,
            classAValueShare = ((classAVal / totalValSafe) * 100.0).toFloat(),
            classBValueShare = ((classBVal / totalValSafe) * 100.0).toFloat(),
            classCValueShare = ((classCVal / totalValSafe) * 100.0).toFloat()
        )

        val matchedAudits = audits.count { it.discrepancy == 0L || it.status == "MATCHED" }
        val auditAccuracyRate = if (audits.isNotEmpty()) {
            (matchedAudits.toFloat() / audits.size.toFloat()) * 100f
        } else {
            100f
        }

        return WarehouseAnalyticsSummary(
            totalSkus = totalSkus,
            totalUnits = totalUnits,
            totalValuationCost = totalValuationCost,
            totalValuationRetail = totalValuationRetail,
            estimatedMarginValue = estimatedMarginValue,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            overstockCount = overstockCount,
            healthyStockCount = healthyStockCount,
            totalInboundUnits = totalInboundUnits,
            totalOutboundUnits = totalOutboundUnits,
            categoryBreakdown = categoryBreakdown,
            zoneUtilization = zoneUtilization,
            abcClassification = abcSummary,
            auditAccuracyRate = auditAccuracyRate
        )
    }

    private companion object {
        const val TAG = "WarehouseVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
