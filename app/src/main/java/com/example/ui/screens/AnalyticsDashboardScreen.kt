package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.InventoryItem
import com.example.data.model.StockMovement
import com.example.data.model.WarehouseAnalyticsSummary
import com.example.ui.components.AbcParetoCard
import com.example.ui.components.MovementFlowVelocityChart
import com.example.ui.components.StockHealthDonutChart
import com.example.ui.components.ZoneCapacityHeatmapCard
import com.example.ui.components.formatCompactRupiah
import com.example.ui.components.formatRupiah
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun AnalyticsDashboardScreen(
    analytics: WarehouseAnalyticsSummary,
    items: List<InventoryItem>,
    movements: List<StockMovement>,
    onNavigateToScanner: () -> Unit,
    onOpenAddSku: () -> Unit,
    onSelectItem: (InventoryItem) -> Unit,
    onSeedDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val criticalItems = items.filter { it.isOutOfStock || it.isLowStock }.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Command Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_warehouse_hero_1791472193594),
                        contentDescription = "Pusat Telemetri Gudang",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Slate950.copy(alpha = 0.94f),
                                        Slate950.copy(alpha = 0.72f),
                                        Slate950.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Emerald400.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Emerald400)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE TELEMETRY SYNC",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald400
                                    )
                                }
                            }
                            Text(
                                text = "Akurasi Audit: ${analytics.auditAccuracyRate.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberTeal400
                            )
                        }

                        Column {
                            Text(
                                text = "Total Valuasi Aset Gudang",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = formatRupiah(analytics.totalValuationCost),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Potensi Nilai Jual: ${formatCompactRupiah(analytics.totalValuationRetail)} (Margin +${formatCompactRupiah(analytics.estimatedMarginValue)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = LaserAmber500
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToScanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LaserAmber500,
                                    contentColor = Slate950
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("hero_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pindai Barcode",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenAddSku,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("hero_add_sku_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SKU Baru",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Empty Warehouse Quick Seed Banner (if no items yet)
        if (items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_inventory_1791472214784),
                            contentDescription = "Gudang Kosong",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Database Inventaris Gudang Masih Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Muat dataset gudang industri standar (7 SKU multi-zona, barcode EAN-13, dan riwayat mutasi) untuk langsung menguji analitik & pemindai barcode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onSeedDemoData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal400,
                                contentColor = Slate950
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seed_warehouse_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Muat Data Gudang Standar Sekarang",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. 4-Card KPI Telemetry Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiTelemetryTile(
                        title = "Total SKU & Unit",
                        primaryValue = "${analytics.totalSkus} SKU",
                        secondaryText = "${analytics.totalUnits} Total Unit Fisik",
                        icon = Icons.Default.Inventory,
                        accentColor = CyberTeal400,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTelemetryTile(
                        title = "Peringatan Stok",
                        primaryValue = "${analytics.lowStockCount + analytics.outOfStockCount} Kritis",
                        secondaryText = "${analytics.outOfStockCount} Habis • ${analytics.lowStockCount} Menipis",
                        icon = Icons.Default.WarningAmber,
                        accentColor = if (analytics.lowStockCount + analytics.outOfStockCount > 0) Rose400 else Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiTelemetryTile(
                        title = "Total Inbound",
                        primaryValue = "+${analytics.totalInboundUnits}",
                        secondaryText = "Unit Barang Masuk",
                        icon = Icons.Default.ArrowDownward,
                        accentColor = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTelemetryTile(
                        title = "Total Outbound",
                        primaryValue = "-${analytics.totalOutboundUnits}",
                        secondaryText = "Unit Barang Keluar",
                        icon = Icons.Default.ArrowUpward,
                        accentColor = LaserAmber500,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Stock Health Donut Chart
        item {
            StockHealthDonutChart(
                healthyCount = analytics.healthyStockCount,
                lowStockCount = analytics.lowStockCount,
                outOfStockCount = analytics.outOfStockCount,
                overstockCount = analytics.overstockCount
            )
        }

        // 5. Critical Low Stock Action List
        if (criticalItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Rose400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Prioritas Restock Segera (Stok Kritis)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Ketuk untuk Inbound",
                                style = MaterialTheme.typography.labelSmall,
                                color = LaserAmber500
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            criticalItems.forEach { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectItem(item) },
                                    color = Slate800.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.sku} • ${item.zone} (${item.aisle})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (item.isOutOfStock) Rose400.copy(alpha = 0.2f) else LaserAmber500.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${item.quantity} / Min ${item.minStock} ${item.unit}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (item.isOutOfStock) Rose400 else LaserAmber500,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Zone Utilization Heatmap
        item {
            ZoneCapacityHeatmapCard(zoneStats = analytics.zoneUtilization)
        }

        // 7. ABC Pareto Classification
        item {
            AbcParetoCard(abcSummary = analytics.abcClassification)
        }

        // 8. Real-Time Movement Velocity Chart
        item {
            MovementFlowVelocityChart(movements = movements)
        }
    }
}

@Composable
private fun KpiTelemetryTile(
    title: String,
    primaryValue: String,
    secondaryText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
