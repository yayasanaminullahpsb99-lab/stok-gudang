package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditSession
import com.example.data.model.InventoryItem
import com.example.data.model.StockMovement
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet500
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatTimestampId(ts: Timestamp?): String {
    val date = (ts ?: Timestamp.now()).toDate()
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(date)
}

@Composable
fun MovementsAndAuditScreen(
    items: List<InventoryItem>,
    movements: List<StockMovement>,
    audits: List<AuditSession>,
    selectedFilterType: String,
    onFilterTypeChange: (String) -> Unit,
    onSubmitAuditCount: (
        item: InventoryItem,
        sessionCode: String,
        countedQty: Long,
        autoReconcile: Boolean,
        notes: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Mutasi Ledger, 1 = Stock Opname Audit
    var selectedAuditItem by remember(items) { mutableStateOf(items.firstOrNull()) }
    var sessionCode by remember { mutableStateOf("OPN-2026-10") }
    var countedQtyStr by remember(selectedAuditItem) {
        mutableStateOf((selectedAuditItem?.quantity ?: 0L).toString())
    }
    var autoReconcile by remember { mutableStateOf(true) }
    var auditNotes by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Sub-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Slate900)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("Buku Besar Mutasi (${movements.size})", "Stock Opname & Audit (${audits.size})")
            tabs.forEachIndexed { idx, title ->
                val selected = activeSubTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeSubTab = idx },
                    color = if (selected) LaserAmber500 else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Slate950 else Color.White,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == 0) {
            // Filter Chips for Movement Ledger
            val filterOptions = listOf(
                "ALL" to "Semua Mutasi",
                "INBOUND" to "Inbound (+)",
                "OUTBOUND" to "Outbound (-)",
                "ADJUSTMENT" to "Adjustment",
                "TRANSFER" to "Transfer Zona"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilterType == key,
                        onClick = { onFilterTypeChange(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberTeal400.copy(alpha = 0.22f),
                            selectedLabelColor = CyberTeal400
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedMovements = remember(movements, selectedFilterType) {
                if (selectedFilterType == "ALL") movements
                else movements.filter { it.type == selectedFilterType }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("movements_ledger_list"),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (displayedMovements.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada riwayat transaksi mutasi untuk filter ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(displayedMovements, key = { it.id }) { mov ->
                        MovementLedgerRow(movement = mov)
                    }
                }
            }
        } else {
            // Stock Opname / Cycle Count Audit Mode
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("audit_opname_list"),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = LaserAmber500
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Formulir Rekonsiliasi Stock Opname (Cycle Count)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }

                            if (items.isEmpty()) {
                                Text(
                                    text = "Tambahkan atau muat SKU terlebih dahulu untuk memulai audit stok fisik.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Pilih SKU yang Diaudit:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items.forEach { skuItem ->
                                        val isChosen = selectedAuditItem?.id == skuItem.id
                                        FilterChip(
                                            selected = isChosen,
                                            onClick = {
                                                selectedAuditItem = skuItem
                                                countedQtyStr = skuItem.quantity.toString()
                                            },
                                            label = { Text("${skuItem.sku} (${skuItem.quantity})") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LaserAmber500.copy(alpha = 0.25f),
                                                selectedLabelColor = LaserAmber500
                                            )
                                        )
                                    }
                                }

                                val currentTarget = selectedAuditItem ?: items.first()
                                val countedVal = countedQtyStr.toLongOrNull() ?: 0L
                                val discrepancy = countedVal - currentTarget.quantity

                                Surface(
                                    color = Slate800,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentTarget.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Stok Sistem: ${currentTarget.quantity} ${currentTarget.unit} • ${currentTarget.zone}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = CyberTeal400
                                            )
                                        }
                                        val diffColor = if (discrepancy == 0L) Emerald400 else Rose400
                                        Text(
                                            text = if (discrepancy >= 0) "Selisih: +$discrepancy" else "Selisih: $discrepancy",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = diffColor
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = sessionCode,
                                        onValueChange = { sessionCode = it },
                                        label = { Text("Kode Sesi Opname") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = countedQtyStr,
                                        onValueChange = { countedQtyStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Stok Fisik Aktual") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = auditNotes,
                                    onValueChange = { auditNotes = it },
                                    label = { Text("Catatan Auditor / Penyebab Selisih") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { autoReconcile = !autoReconcile }
                                ) {
                                    Checkbox(
                                        checked = autoReconcile,
                                        onCheckedChange = { autoReconcile = it }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sesuaikan stok sistem secara otomatis jika terdapat selisih fisik",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        onSubmitAuditCount(
                                            currentTarget,
                                            sessionCode,
                                            countedVal,
                                            autoReconcile,
                                            auditNotes.ifBlank { "Audit rutin gudang" }
                                        )
                                        auditNotes = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LaserAmber500,
                                        contentColor = Slate950
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_audit_button")
                                ) {
                                    Text(
                                        text = "Simpan Hasil Verifikasi Opname",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                items(audits, key = { it.id }) { audit ->
                    AuditLogCard(audit = audit)
                }
            }
        }
    }
}

@Composable
private fun MovementLedgerRow(movement: StockMovement) {
    val (accentColor, icon, prefix) = when (movement.type) {
        "INBOUND" -> Triple(Emerald400, Icons.Default.ArrowDownward, "+")
        "OUTBOUND" -> Triple(Rose400, Icons.Default.ArrowUpward, "-")
        "TRANSFER" -> Triple(Violet500, Icons.Default.SwapHoriz, "⇄ ")
        else -> Triple(LaserAmber500, Icons.Default.Tune, "±")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = movement.referenceDoc,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Text(
                        text = "• ${movement.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = movement.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${movement.zone} • ${formatTimestampId(movement.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (movement.notes.isNotBlank()) {
                    Text(
                        text = movement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTeal400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${movement.quantityDelta}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movement.previousQty} → ${movement.newQty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuditLogCard(audit: AuditSession) {
    val isMatched = audit.discrepancy == 0L
    val badgeColor = if (isMatched) Emerald400 else LaserAmber500

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${audit.sessionCode} • ${audit.itemName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = "${audit.zone} • Barcode: ${audit.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (audit.notes.isNotBlank()) {
                    Text(
                        text = audit.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTeal400
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Fisik: ${audit.countedQty} (Sistem: ${audit.systemQty})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = if (isMatched) "SESUAI 100%" else "SELISIH ${audit.discrepancy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
            }
        }
    }
}
