package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sku: String = "",
    val barcode: String = "",
    val name: String = "",
    val category: String = "Elektronik",
    val zone: String = "Zona A - Fast Moving",
    val aisle: String = "A-01-R1",
    val quantity: Long = 0L,
    val minStock: Long = 10L,
    val maxStock: Long = 100L,
    val unit: String = "Pcs",
    val unitCost: Double = 0.0,
    val unitPrice: Double = 0.0,
    val supplierName: String = "",
    val batchNumber: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val totalCostValue: Double
        get() = quantity * unitCost

    val totalRetailValue: Double
        get() = quantity * unitPrice

    val isOutOfStock: Boolean
        get() = quantity <= 0L

    val isLowStock: Boolean
        get() = quantity in 1L..minStock

    val isOverstock: Boolean
        get() = quantity > maxStock

    val stockFillRatio: Float
        get() = if (maxStock > 0L) (quantity.toFloat() / maxStock.toFloat()).coerceIn(0f, 1.25f) else 0f
}

enum class MovementType(val label: String) {
    INBOUND("Barang Masuk (Inbound)"),
    OUTBOUND("Barang Keluar (Outbound)"),
    ADJUSTMENT("Penyesuaian (Adjustment)"),
    TRANSFER("Transfer Zona (Mutasi)")
}

data class StockMovement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val sku: String = "",
    val barcode: String = "",
    val type: String = "INBOUND",
    val quantityDelta: Long = 0L,
    val previousQty: Long = 0L,
    val newQty: Long = 0L,
    val referenceDoc: String = "",
    val zone: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null
)

data class AuditSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionCode: String = "",
    val zone: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val barcode: String = "",
    val systemQty: Long = 0L,
    val countedQty: Long = 0L,
    val discrepancy: Long = 0L,
    val status: String = "MATCHED",
    val notes: String = "",
    val createdAt: Timestamp? = null
)

data class WarehouseAnalyticsSummary(
    val totalSkus: Int = 0,
    val totalUnits: Long = 0L,
    val totalValuationCost: Double = 0.0,
    val totalValuationRetail: Double = 0.0,
    val estimatedMarginValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val overstockCount: Int = 0,
    val healthyStockCount: Int = 0,
    val totalInboundUnits: Long = 0L,
    val totalOutboundUnits: Long = 0L,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val zoneUtilization: Map<String, ZoneStat> = emptyMap(),
    val abcClassification: AbcSummary = AbcSummary(),
    val auditAccuracyRate: Float = 100f
)

data class ZoneStat(
    val zoneName: String,
    val skuCount: Int,
    val currentUnits: Long,
    val maxCapacityUnits: Long,
    val totalValue: Double
) {
    val utilizationPercent: Float
        get() = if (maxCapacityUnits > 0L) {
            ((currentUnits.toFloat() / maxCapacityUnits.toFloat()) * 100f).coerceIn(0f, 100f)
        } else 0f
}

data class AbcSummary(
    val classAItems: List<InventoryItem> = emptyList(),
    val classBItems: List<InventoryItem> = emptyList(),
    val classCItems: List<InventoryItem> = emptyList(),
    val classAValueShare: Float = 0f,
    val classBValueShare: Float = 0f,
    val classCValueShare: Float = 0f
)
