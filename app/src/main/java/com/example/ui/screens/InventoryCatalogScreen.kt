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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InventoryItem
import com.example.ui.components.WAREHOUSE_CATEGORIES
import com.example.ui.components.formatCompactRupiah
import com.example.ui.components.formatRupiah
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.viewmodel.InventoryFilterMode
import com.example.ui.viewmodel.InventorySortMode

@Composable
fun InventoryCatalogScreen(
    items: List<InventoryItem>,
    searchQuery: String,
    selectedCategory: String,
    filterMode: InventoryFilterMode,
    sortMode: InventorySortMode,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onFilterModeChange: (InventoryFilterMode) -> Unit,
    onSortModeChange: (InventorySortMode) -> Unit,
    onSelectItem: (InventoryItem) -> Unit,
    onOpenAddSku: () -> Unit,
    onSeedDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search & Sort Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari SKU, nama, barcode, rak...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = LaserAmber500)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventory_search_input")
            )

            Box {
                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showSortMenu = true },
                    color = Slate900,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Urutkan Inventaris",
                            tint = CyberTeal400
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    InventorySortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == sortMode) LaserAmber500 else Color.Unspecified
                                )
                            },
                            onClick = {
                                onSortModeChange(mode)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Mode Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventoryFilterMode.entries.forEach { mode ->
                FilterChip(
                    selected = filterMode == mode,
                    onClick = { onFilterModeChange(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserAmber500.copy(alpha = 0.22f),
                        selectedLabelColor = LaserAmber500
                    )
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Semua") + WAREHOUSE_CATEGORIES
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberTeal400.copy(alpha = 0.2f),
                        selectedLabelColor = CyberTeal400
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tidak Ada SKU yang Cocok",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tambahkan SKU baru atau muat katalog standar gudang untuk memulai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenAddSku,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LaserAmber500,
                                contentColor = Slate950
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah SKU")
                        }
                        Button(
                            onClick = onSeedDemoData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal400,
                                contentColor = Slate950
                            )
                        ) {
                            Text("Muat Data Standar")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inventory_catalog_list"),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    InventorySkuCard(
                        item = item,
                        onClick = { onSelectItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventorySkuCard(
    item: InventoryItem,
    onClick: () -> Unit
) {
    val statusColor = when {
        item.isOutOfStock -> Rose400
        item.isLowStock -> LaserAmber500
        item.isOverstock -> CyberTeal400
        else -> Emerald400
    }
    val statusText = when {
        item.isOutOfStock -> "HABIS (0)"
        item.isLowStock -> "MENIPIS"
        item.isOverstock -> "OVERSTOCK"
        else -> "OPTIMAL"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("sku_card_${item.sku}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.sku,
                                style = MaterialTheme.typography.labelMedium,
                                color = LaserAmber500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = CyberTeal400.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTeal400,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fill Ratio Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Slate800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = item.stockFillRatio.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.barcode,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${item.zone} • Rak ${item.aisle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTeal400
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.quantity} / ${item.maxStock} ${item.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Valuasi: ${formatCompactRupiah(item.totalCostValue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LaserAmber500
                    )
                }
            }
        }
    }
}
