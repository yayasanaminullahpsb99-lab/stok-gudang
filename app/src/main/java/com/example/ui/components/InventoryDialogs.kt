package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryItem
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet500

val WAREHOUSE_CATEGORIES = listOf(
    "Elektronik",
    "Logistik",
    "Otomotif",
    "Bahan Baku",
    "Farmasi",
    "Umum"
)

val WAREHOUSE_ZONES = listOf(
    "Zona A - Fast Moving",
    "Zona B - Bulk Storage",
    "Zona C - Cold Chain",
    "Zona D - Hazmat/Khusus"
)

val WAREHOUSE_UNITS = listOf("Unit", "Pcs", "Box", "Pallet", "Roll", "Kg")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditSkuDialog(
    initialItem: InventoryItem? = null,
    prefilledBarcode: String = "",
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val isEdit = initialItem != null
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var sku by remember {
        mutableStateOf(
            initialItem?.sku ?: "SKU-${(1000..9999).random()}"
        )
    }
    var barcode by remember {
        mutableStateOf(
            initialItem?.barcode?.ifBlank { prefilledBarcode }
                ?: prefilledBarcode.ifBlank { "8992761${(100000..999999).random()}" }
        )
    }
    var category by remember { mutableStateOf(initialItem?.category ?: WAREHOUSE_CATEGORIES.first()) }
    var zone by remember { mutableStateOf(initialItem?.zone ?: WAREHOUSE_ZONES.first()) }
    var aisle by remember { mutableStateOf(initialItem?.aisle ?: "A-01-R1") }
    var quantityStr by remember { mutableStateOf((initialItem?.quantity ?: 25L).toString()) }
    var minStockStr by remember { mutableStateOf((initialItem?.minStock ?: 10L).toString()) }
    var maxStockStr by remember { mutableStateOf((initialItem?.maxStock ?: 100L).toString()) }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "Unit") }
    var unitCostStr by remember { mutableStateOf((initialItem?.unitCost?.toLong() ?: 250000L).toString()) }
    var unitPriceStr by remember { mutableStateOf((initialItem?.unitPrice?.toLong() ?: 350000L).toString()) }
    var supplierName by remember { mutableStateOf(initialItem?.supplierName ?: "PT Distribusi Nusantara") }
    var batchNumber by remember { mutableStateOf(initialItem?.batchNumber ?: "LOT-2026-10") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Slate900
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Master SKU Gudang" else "Registrasi SKU Gudang Baru",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Nama Barang / Komponen *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sku_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Kode SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / EAN") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    barcode = "8992761${(100000..999999).random()}"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Generate Barcode",
                                    tint = LaserAmber500
                                )
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Text(
                    text = "Kategori Inventaris",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WAREHOUSE_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserAmber500.copy(alpha = 0.25f),
                                selectedLabelColor = LaserAmber500
                            )
                        )
                    }
                }

                Text(
                    text = "Zona Penyimpanan Gudang",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WAREHOUSE_ZONES.forEach { zn ->
                        FilterChip(
                            selected = zone == zn,
                            onClick = { zone = zn },
                            label = { Text(zn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberTeal400.copy(alpha = 0.25f),
                                selectedLabelColor = CyberTeal400
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = aisle,
                        onValueChange = { aisle = it },
                        label = { Text("Lorong / Rak (Bin)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Nomor Batch / Lot") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stok Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxStockStr,
                        onValueChange = { maxStockStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kapasitas Maks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Satuan Hitung",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WAREHOUSE_UNITS.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = unitCostStr,
                        onValueChange = { unitCostStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Modal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Nama Pemasok / Vendor Utama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                BarcodeLabelPreview(
                    barcodeValue = barcode,
                    skuValue = sku,
                    itemName = name.ifBlank { "Pratinjau Label Barcode SKU" },
                    zoneText = "$zone ($aisle)"
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = Rose400,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                validationError = "Nama barang wajib diisi"
                                return@Button
                            }
                            onSave(
                                sku.ifBlank { "SKU-001" },
                                barcode.ifBlank { "899000111222" },
                                name,
                                category,
                                zone,
                                aisle.ifBlank { "A-01-R1" },
                                quantityStr.toLongOrNull() ?: 0L,
                                minStockStr.toLongOrNull() ?: 5L,
                                (maxStockStr.toLongOrNull() ?: 100L).coerceAtLeast(1L),
                                unit,
                                unitCostStr.toDoubleOrNull() ?: 0.0,
                                unitPriceStr.toDoubleOrNull() ?: 0.0,
                                supplierName.ifBlank { "Vendor Umum" },
                                batchNumber.ifBlank { "BATCH-01" }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserAmber500,
                            contentColor = Slate950
                        ),
                        modifier = Modifier.testTag("save_sku_button")
                    ) {
                        Text(if (isEdit) "Simpan Perubahan" else "Daftarkan SKU")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockMovementTransactionDialog(
    item: InventoryItem,
    initialType: String = "INBOUND",
    onDismiss: () -> Unit,
    onEditMaster: () -> Unit,
    onDeleteItem: () -> Unit,
    onSubmitTransaction: (
        type: String,
        quantityInput: Long,
        referenceDoc: String,
        targetZone: String,
        targetAisle: String,
        notes: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var quantityStr by remember { mutableStateOf("10") }
    var referenceDoc by remember {
        mutableStateOf(
            when (initialType) {
                "INBOUND" -> "PO-2026-${(1000..9999).random()}"
                "OUTBOUND" -> "DO-2026-${(1000..9999).random()}"
                "TRANSFER" -> "TRF-2026-${(1000..9999).random()}"
                else -> "ADJ-2026-${(1000..9999).random()}"
            }
        )
    }
    var targetZone by remember { mutableStateOf(item.zone) }
    var targetAisle by remember { mutableStateOf(item.aisle) }
    var notes by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus SKU dari Gudang?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus '${item.name}' (${item.sku}) secara permanen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteItem()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose400, contentColor = Slate950)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Batal")
                }
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Slate900
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${item.sku} • Barcode: ${item.barcode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LaserAmber500
                        )
                    }
                    IconButton(onClick = onEditMaster) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Master SKU",
                            tint = CyberTeal400
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus SKU",
                            tint = Rose400
                        )
                    }
                }

                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Stok Saat Ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Lokasi Rak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.zone} (${item.aisle})",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyberTeal400
                            )
                        }
                    }
                }

                Text(
                    text = "Pilih Jenis Transaksi Mutasi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val txTypes = listOf(
                    Triple("INBOUND", "Inbound (+)", Emerald400),
                    Triple("OUTBOUND", "Outbound (-)", Rose400),
                    Triple("ADJUSTMENT", "Set Opname", LaserAmber500),
                    Triple("TRANSFER", "Pindah Zona", Violet500)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    txTypes.forEach { (typeKey, label, color) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = {
                                selectedType = typeKey
                                referenceDoc = when (typeKey) {
                                    "INBOUND" -> "PO-2026-${(1000..9999).random()}"
                                    "OUTBOUND" -> "DO-2026-${(1000..9999).random()}"
                                    "TRANSFER" -> "TRF-2026-${(1000..9999).random()}"
                                    else -> "ADJ-2026-${(1000..9999).random()}"
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = {
                                val icon = when (typeKey) {
                                    "INBOUND" -> Icons.Default.AddCircle
                                    "OUTBOUND" -> Icons.Default.RemoveCircle
                                    "TRANSFER" -> Icons.Default.SwapHoriz
                                    else -> Icons.Default.Tune
                                }
                                Icon(icon, contentDescription = null, tint = color)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.22f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }

                if (selectedType != "TRANSFER") {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it.filter { ch -> ch.isDigit() } },
                        label = {
                            Text(
                                when (selectedType) {
                                    "INBOUND" -> "Jumlah Unit Masuk (${item.unit})"
                                    "OUTBOUND" -> "Jumlah Unit Keluar (${item.unit})"
                                    else -> "Jumlah Stok Fisik Baru (${item.unit})"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_qty_input")
                    )
                } else {
                    Text(
                        text = "Pilih Zona Tujuan Transfer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WAREHOUSE_ZONES.forEach { zn ->
                            FilterChip(
                                selected = targetZone == zn,
                                onClick = { targetZone = zn },
                                label = { Text(zn) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = targetAisle,
                        onValueChange = { targetAisle = it },
                        label = { Text("Lorong / Rak Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = referenceDoc,
                    onValueChange = { referenceDoc = it },
                    label = { Text("Nomor Dokumen Referensi (PO / DO / SJ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Operasional Gudang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                BarcodeLabelPreview(
                    barcodeValue = item.barcode,
                    skuValue = item.sku,
                    itemName = item.name,
                    zoneText = "${item.zone} • ${item.aisle}"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantityStr.toLongOrNull() ?: 0L
                            onSubmitTransaction(
                                selectedType,
                                qty,
                                referenceDoc,
                                targetZone,
                                targetAisle,
                                notes.ifBlank { "Transaksi $selectedType via terminal WMS" }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserAmber500,
                            contentColor = Slate950
                        ),
                        modifier = Modifier.testTag("submit_transaction_button")
                    ) {
                        Text(
                            text = "Proses Mutasi",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
