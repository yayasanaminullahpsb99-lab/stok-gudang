package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditSession
import com.example.data.model.InventoryItem
import com.example.data.model.StockMovement
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WarehouseRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun toInventoryItem(doc: DocumentSnapshot): InventoryItem {
        val data = doc.data ?: emptyMap()
        return InventoryItem(
            id = doc.id,
            userId = data["userId"] as? String ?: "",
            sku = data["sku"] as? String ?: "",
            barcode = data["barcode"] as? String ?: "",
            name = data["name"] as? String ?: "",
            category = data["category"] as? String ?: "Elektronik",
            zone = data["zone"] as? String ?: "Zona A - Fast Moving",
            aisle = data["aisle"] as? String ?: "A-01-R1",
            quantity = (data["quantity"] as? Number)?.toLong() ?: 0L,
            minStock = (data["minStock"] as? Number)?.toLong() ?: 10L,
            maxStock = (data["maxStock"] as? Number)?.toLong() ?: 100L,
            unit = data["unit"] as? String ?: "Pcs",
            unitCost = (data["unitCost"] as? Number)?.toDouble() ?: 0.0,
            unitPrice = (data["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            supplierName = data["supplierName"] as? String ?: "",
            batchNumber = data["batchNumber"] as? String ?: "",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun toStockMovement(doc: DocumentSnapshot): StockMovement {
        val data = doc.data ?: emptyMap()
        return StockMovement(
            id = doc.id,
            userId = data["userId"] as? String ?: "",
            itemId = data["itemId"] as? String ?: "",
            itemName = data["itemName"] as? String ?: "",
            sku = data["sku"] as? String ?: "",
            barcode = data["barcode"] as? String ?: "",
            type = data["type"] as? String ?: "INBOUND",
            quantityDelta = (data["quantityDelta"] as? Number)?.toLong() ?: 0L,
            previousQty = (data["previousQty"] as? Number)?.toLong() ?: 0L,
            newQty = (data["newQty"] as? Number)?.toLong() ?: 0L,
            referenceDoc = data["referenceDoc"] as? String ?: "",
            zone = data["zone"] as? String ?: "",
            notes = data["notes"] as? String ?: "",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun toAuditSession(doc: DocumentSnapshot): AuditSession {
        val data = doc.data ?: emptyMap()
        return AuditSession(
            id = doc.id,
            userId = data["userId"] as? String ?: "",
            sessionCode = data["sessionCode"] as? String ?: "",
            zone = data["zone"] as? String ?: "",
            itemId = data["itemId"] as? String ?: "",
            itemName = data["itemName"] as? String ?: "",
            barcode = data["barcode"] as? String ?: "",
            systemQty = (data["systemQty"] as? Number)?.toLong() ?: 0L,
            countedQty = (data["countedQty"] as? Number)?.toLong() ?: 0L,
            discrepancy = (data["discrepancy"] as? Number)?.toLong() ?: 0L,
            status = data["status"] as? String ?: "MATCHED",
            notes = data["notes"] as? String ?: "",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    fun observeInventoryItems(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<InventoryItem>> = flow {
        val path = "users/$userId/inventory_items"
        emitAll(
            db.collection("users").document(userId).collection("inventory_items")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .map { toInventoryItem(it) }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeStockMovements(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<StockMovement>> = flow {
        val path = "users/$userId/stock_movements"
        emitAll(
            db.collection("users").document(userId).collection("stock_movements")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .map { toStockMovement(it) }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAuditLogs(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<AuditSession>> = flow {
        val path = "users/$userId/audit_logs"
        emitAll(
            db.collection("users").document(userId).collection("audit_logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .map { toAuditSession(it) }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getInventoryItemById(targetUserId: String, itemId: String): Result<InventoryItem> {
        val path = "users/$targetUserId/inventory_items/$itemId"
        return try {
            val doc = db.collection("users")
                .document(targetUserId)
                .collection("inventory_items")
                .document(itemId)
                .get()
                .await()
            if (doc.exists()) {
                Result.success(toInventoryItem(doc))
            } else {
                Result.failure(NoSuchElementException("Item not found: $itemId"))
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createInventoryItem(
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
        batchNumber: String
    ): Result<String> {
        val uid = requireUserId()
        val itemId = "item_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val itemRef = db.collection("users").document(uid).collection("inventory_items").document(itemId)

        val payload = mapOf(
            "userId" to uid,
            "sku" to sku.trim().ifEmpty { "SKU-${System.currentTimeMillis() % 10000}" },
            "barcode" to barcode.trim().ifEmpty { "899${System.currentTimeMillis() % 10000000000L}" },
            "name" to name.trim(),
            "category" to category.trim().ifEmpty { "Elektronik" },
            "zone" to zone.trim().ifEmpty { "Zona A - Fast Moving" },
            "aisle" to aisle.trim().ifEmpty { "A-01-R1" },
            "quantity" to quantity.coerceAtLeast(0L),
            "minStock" to minStock.coerceAtLeast(0L),
            "maxStock" to maxStock.coerceAtLeast(1L),
            "unit" to unit.trim().ifEmpty { "Pcs" },
            "unitCost" to unitCost.coerceAtLeast(0.0),
            "unitPrice" to unitPrice.coerceAtLeast(0.0),
            "supplierName" to supplierName.trim(),
            "batchNumber" to batchNumber.trim().ifEmpty { "BATCH-2026-01" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.set(itemRef, payload)

            if (quantity > 0L) {
                val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val movRef = db.collection("users").document(uid).collection("stock_movements").document(movId)
                val movPayload = mapOf(
                    "userId" to uid,
                    "itemId" to itemId,
                    "itemName" to name.trim(),
                    "sku" to (payload["sku"] as String),
                    "barcode" to (payload["barcode"] as String),
                    "type" to "INBOUND",
                    "quantityDelta" to quantity,
                    "previousQty" to 0L,
                    "newQty" to quantity,
                    "referenceDoc" to "INIT-REG-${(1000..9999).random()}",
                    "zone" to (payload["zone"] as String),
                    "notes" to "Registrasi stok awal SKU baru",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(movRef, movPayload)
            }

            batch.commit().await()
            Result.success(itemId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItemDetails(
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
        batchNumber: String
    ): Result<Unit> {
        val uid = requireUserId()
        val itemRef = db.collection("users").document(uid).collection("inventory_items").document(itemId)
        val updates = mapOf(
            "sku" to sku.trim(),
            "barcode" to barcode.trim(),
            "name" to name.trim(),
            "category" to category.trim(),
            "zone" to zone.trim(),
            "aisle" to aisle.trim(),
            "quantity" to quantity.coerceAtLeast(0L),
            "minStock" to minStock.coerceAtLeast(0L),
            "maxStock" to maxStock.coerceAtLeast(1L),
            "unit" to unit.trim(),
            "unitCost" to unitCost.coerceAtLeast(0.0),
            "unitPrice" to unitPrice.coerceAtLeast(0.0),
            "supplierName" to supplierName.trim(),
            "batchNumber" to batchNumber.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            itemRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordStockTransaction(
        item: InventoryItem,
        type: String,
        quantityInput: Long,
        referenceDoc: String,
        targetZone: String = item.zone,
        targetAisle: String = item.aisle,
        notes: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val prevQty = item.quantity
        val newQty = when (type) {
            "INBOUND" -> prevQty + quantityInput.coerceAtLeast(0L)
            "OUTBOUND" -> (prevQty - quantityInput.coerceAtLeast(0L)).coerceAtLeast(0L)
            "ADJUSTMENT" -> quantityInput.coerceAtLeast(0L)
            "TRANSFER" -> prevQty
            else -> prevQty
        }
        val delta = kotlin.math.abs(newQty - prevQty).let {
            if (type == "TRANSFER") quantityInput.coerceAtLeast(1L) else it
        }

        val itemRef = db.collection("users").document(uid).collection("inventory_items").document(item.id)
        val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val movRef = db.collection("users").document(uid).collection("stock_movements").document(movId)

        val itemUpdates = mapOf(
            "quantity" to newQty,
            "zone" to targetZone.trim().ifEmpty { item.zone },
            "aisle" to targetAisle.trim().ifEmpty { item.aisle },
            "batchNumber" to item.batchNumber,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val movPayload = mapOf(
            "userId" to uid,
            "itemId" to item.id,
            "itemName" to item.name,
            "sku" to item.sku,
            "barcode" to item.barcode,
            "type" to type,
            "quantityDelta" to delta,
            "previousQty" to prevQty,
            "newQty" to newQty,
            "referenceDoc" to referenceDoc.trim().ifEmpty { "TRX-${System.currentTimeMillis() % 100000}" },
            "zone" to targetZone.trim().ifEmpty { item.zone },
            "notes" to notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.update(itemRef, itemUpdates)
            batch.set(movRef, movPayload)
            batch.commit().await()
            Result.success(movId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordAuditCount(
        item: InventoryItem,
        sessionCode: String,
        countedQty: Long,
        autoReconcileStock: Boolean,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val discrepancy = countedQty - item.quantity
        val status = when {
            discrepancy == 0L -> "MATCHED"
            autoReconcileStock -> "DISCREPANCY_RESOLVED"
            else -> "DISCREPANCY_PENDING"
        }

        val auditId = "aud_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val auditRef = db.collection("users").document(uid).collection("audit_logs").document(auditId)
        val auditPayload = mapOf(
            "userId" to uid,
            "sessionCode" to sessionCode.trim().ifEmpty { "OPN-2026-10" },
            "zone" to item.zone,
            "itemId" to item.id,
            "itemName" to item.name,
            "barcode" to item.barcode,
            "systemQty" to item.quantity,
            "countedQty" to countedQty.coerceAtLeast(0L),
            "discrepancy" to discrepancy,
            "status" to status,
            "notes" to notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.set(auditRef, auditPayload)

            if (autoReconcileStock && discrepancy != 0L) {
                val itemRef = db.collection("users").document(uid).collection("inventory_items").document(item.id)
                batch.update(
                    itemRef,
                    mapOf(
                        "quantity" to countedQty.coerceAtLeast(0L),
                        "zone" to item.zone,
                        "aisle" to item.aisle,
                        "batchNumber" to item.batchNumber,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val movRef = db.collection("users").document(uid).collection("stock_movements").document(movId)
                batch.set(
                    movRef,
                    mapOf(
                        "userId" to uid,
                        "itemId" to item.id,
                        "itemName" to item.name,
                        "sku" to item.sku,
                        "barcode" to item.barcode,
                        "type" to "ADJUSTMENT",
                        "quantityDelta" to kotlin.math.abs(discrepancy),
                        "previousQty" to item.quantity,
                        "newQty" to countedQty.coerceAtLeast(0L),
                        "referenceDoc" to sessionCode.trim().ifEmpty { "OPN-2026-10" },
                        "zone" to item.zone,
                        "notes" to "Rekonsiliasi Otomatis Stock Opname (Selisih: $discrepancy)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(auditId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, auditRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        val uid = requireUserId()
        val itemRef = db.collection("users").document(uid).collection("inventory_items").document(itemId)
        return try {
            itemRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedEnterpriseWarehouseDataset(): Result<Int> {
        val uid = requireUserId()
        val starterItems = listOf(
            InventoryItem(
                sku = "ELK-2026-001",
                barcode = "8992761102014",
                name = "Industrial Barcode Scanner Zebra DS3678",
                category = "Elektronik",
                zone = "Zona A - Fast Moving",
                aisle = "A-01-R2",
                quantity = 48L,
                minStock = 15L,
                maxStock = 80L,
                unit = "Unit",
                unitCost = 4250000.0,
                unitPrice = 5600000.0,
                supplierName = "PT Solusi Otomasi Nusantara",
                batchNumber = "LOT-ZBR-2610"
            ),
            InventoryItem(
                sku = "OTM-2026-014",
                barcode = "8992761102021",
                name = "Hydraulic Pallet Jack 3 Ton Crown PTH50",
                category = "Logistik",
                zone = "Zona B - Bulk Storage",
                aisle = "B-04-R1",
                quantity = 7L,
                minStock = 10L,
                maxStock = 30L,
                unit = "Unit",
                unitCost = 6800000.0,
                unitPrice = 8950000.0,
                supplierName = "CV Mekanika Jaya",
                batchNumber = "LOT-CRW-2609"
            ),
            InventoryItem(
                sku = "ELK-2026-089",
                barcode = "8992761102038",
                name = "Programmable Logic Controller Siemens S7-1200",
                category = "Elektronik",
                zone = "Zona A - Fast Moving",
                aisle = "A-03-R4",
                quantity = 64L,
                minStock = 20L,
                maxStock = 100L,
                unit = "Unit",
                unitCost = 3450000.0,
                unitPrice = 4750000.0,
                supplierName = "PT Elektro Mandiri",
                batchNumber = "LOT-SMN-2610"
            ),
            InventoryItem(
                sku = "BHB-2026-102",
                barcode = "8992761102045",
                name = "Stretch Film Industrial Roll 500mm x 300m",
                category = "Bahan Baku",
                zone = "Zona B - Bulk Storage",
                aisle = "B-01-R1",
                quantity = 185L,
                minStock = 50L,
                maxStock = 250L,
                unit = "Roll",
                unitCost = 95000.0,
                unitPrice = 135000.0,
                supplierName = "PT Polimer Kemasindo",
                batchNumber = "LOT-STF-2608"
            ),
            InventoryItem(
                sku = "FRM-2026-055",
                barcode = "8992761102052",
                name = "Cold Chain Thermal Data Logger Testo 184-T4",
                category = "Farmasi",
                zone = "Zona C - Cold Chain",
                aisle = "C-02-R1",
                quantity = 4L,
                minStock = 12L,
                maxStock = 50L,
                unit = "Unit",
                unitCost = 2150000.0,
                unitPrice = 2850000.0,
                supplierName = "PT Medika Presisi",
                batchNumber = "LOT-TST-2610"
            ),
            InventoryItem(
                sku = "OTM-2026-077",
                barcode = "8992761102069",
                name = "Lithium-Ion Forklift Battery Pack 48V 400Ah",
                category = "Otomotif",
                zone = "Zona D - Hazmat/Khusus",
                aisle = "D-01-R1",
                quantity = 9L,
                minStock = 4L,
                maxStock = 15L,
                unit = "Unit",
                unitCost = 28500000.0,
                unitPrice = 35000000.0,
                supplierName = "PT Energi Baterai Industri",
                batchNumber = "LOT-LFP-2607"
            ),
            InventoryItem(
                sku = "LOG-2026-033",
                barcode = "8992761102076",
                name = "RFID UHF Smart Pallet Tag IP68 Waterproof (Box 100)",
                category = "Logistik",
                zone = "Zona A - Fast Moving",
                aisle = "A-02-R3",
                quantity = 0L,
                minStock = 15L,
                maxStock = 60L,
                unit = "Box",
                unitCost = 1450000.0,
                unitPrice = 1980000.0,
                supplierName = "PT Solusi Otomasi Nusantara",
                batchNumber = "LOT-RFD-2609"
            )
        )

        return try {
            val batch = db.batch()
            starterItems.forEachIndexed { index, item ->
                val itemId = "item_seed_${index + 1}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
                val itemRef = db.collection("users").document(uid).collection("inventory_items").document(itemId)
                batch.set(
                    itemRef,
                    mapOf(
                        "userId" to uid,
                        "sku" to item.sku,
                        "barcode" to item.barcode,
                        "name" to item.name,
                        "category" to item.category,
                        "zone" to item.zone,
                        "aisle" to item.aisle,
                        "quantity" to item.quantity,
                        "minStock" to item.minStock,
                        "maxStock" to item.maxStock,
                        "unit" to item.unit,
                        "unitCost" to item.unitCost,
                        "unitPrice" to item.unitPrice,
                        "supplierName" to item.supplierName,
                        "batchNumber" to item.batchNumber,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val movId = "mov_seed_${index + 1}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
                val movRef = db.collection("users").document(uid).collection("stock_movements").document(movId)
                val movType = if (index % 3 == 1) "OUTBOUND" else "INBOUND"
                val delta = if (movType == "OUTBOUND") 5L else item.quantity.coerceAtLeast(10L)
                val prev = if (movType == "OUTBOUND") item.quantity + 5L else 0L
                batch.set(
                    movRef,
                    mapOf(
                        "userId" to uid,
                        "itemId" to itemId,
                        "itemName" to item.name,
                        "sku" to item.sku,
                        "barcode" to item.barcode,
                        "type" to movType,
                        "quantityDelta" to delta,
                        "previousQty" to prev,
                        "newQty" to item.quantity,
                        "referenceDoc" to if (movType == "INBOUND") "PO-2026-100${index + 1}" else "DO-2026-200${index + 1}",
                        "zone" to item.zone,
                        "notes" to if (movType == "INBOUND") "Penerimaan stok kontainer utama" else "Pengiriman distribusi regional",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val sampleAuditId = "aud_seed_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            val auditRef = db.collection("users").document(uid).collection("audit_logs").document(sampleAuditId)
            batch.set(
                auditRef,
                mapOf(
                    "userId" to uid,
                    "sessionCode" to "OPN-2026-Q4",
                    "zone" to "Zona A - Fast Moving",
                    "itemId" to "seed_audit_item",
                    "itemName" to "Industrial Barcode Scanner Zebra DS3678",
                    "barcode" to "8992761102014",
                    "systemQty" to 48L,
                    "countedQty" to 48L,
                    "discrepancy" to 0L,
                    "status" to "MATCHED",
                    "notes" to "Verifikasi fisik sesuai 100% dengan sistem",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(starterItems.size)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/inventory_items")
            Result.failure(e)
        }
    }
}
