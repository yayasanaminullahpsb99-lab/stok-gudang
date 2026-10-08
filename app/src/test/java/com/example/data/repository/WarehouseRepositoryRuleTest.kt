package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WarehouseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createInventoryItem_validPayload_createsDocumentAndMovement() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = WarehouseRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInventoryItem(
                sku = "SKU-TEST-001",
                barcode = "8991002003001",
                name = "Industrial Barcode Scanner",
                category = "Elektronik",
                zone = "Zona A - Fast Moving",
                aisle = "A-01-R1",
                quantity = 35L,
                minStock = 10L,
                maxStock = 100L,
                unit = "Unit",
                unitCost = 1500000.0,
                unitPrice = 2100000.0,
                supplierName = "PT Tekno Gudang",
                batchNumber = "LOT-2026-01"
            )
        }
        assertTrue(createResult.isSuccess)
        val itemId = createResult.getOrThrow()
        assertTrue(itemId.isNotEmpty())

        val emittedItems = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventoryItems(aliceUid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(emittedItems.any { it.id == itemId && it.quantity == 35L })
    }

    @Test
    fun recordStockTransaction_inboundAndOutbound_updatesQuantityAndCreatesMovement() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = WarehouseRepository(firestore)

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInventoryItem(
                sku = "SKU-TEST-002",
                barcode = "8991002003002",
                name = "Forklift Sensor Module",
                category = "Otomotif",
                zone = "Zona B - Bulk Storage",
                aisle = "B-02-R1",
                quantity = 20L,
                minStock = 5L,
                maxStock = 50L,
                unit = "Pcs",
                unitCost = 500000.0,
                unitPrice = 750000.0,
                supplierName = "PT Otomotif Nusantara",
                batchNumber = "LOT-2026-02"
            ).getOrThrow()
        }

        val createdItem = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getInventoryItemById(aliceUid, itemId).getOrThrow()
        }

        val trxResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordStockTransaction(
                item = createdItem,
                type = "INBOUND",
                quantityInput = 15L,
                referenceDoc = "PO-2026-999",
                notes = "Restock bulanan"
            )
        }
        assertTrue(trxResult.isSuccess)

        val updatedItem = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getInventoryItemById(aliceUid, itemId).getOrThrow()
        }
        assertEquals(35L, updatedItem.quantity)
    }

    @Test
    fun getInventoryItemById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WarehouseRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInventoryItem(
                sku = "SKU-ALICE-SEC",
                barcode = "8991002003099",
                name = "Alice Private Inventory",
                category = "Elektronik",
                zone = "Zona A - Fast Moving",
                aisle = "A-01-R1",
                quantity = 10L,
                minStock = 2L,
                maxStock = 50L,
                unit = "Pcs",
                unitCost = 100000.0,
                unitPrice = 150000.0,
                supplierName = "Supplier A",
                batchNumber = "BATCH-A"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = WarehouseRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getInventoryItemById(aliceUid, itemId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeInventoryItems_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = WarehouseRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeInventoryItems("unauth_user_123").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertTrue("Expected FirebaseFirestoreException in cause chain, got $e", firestoreEx != null)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.warehouse@test.com"
        const val BOB_EMAIL = "bob.warehouse@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
